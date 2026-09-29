// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.runtime.CoreConfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 全局风场模拟（复刻 IC2 WindSim 机制，独立实现）。
 *
 * <p>每个维度维护一份风场状态：
 * <ul>
 *   <li>基础风力强度每 128 tick 随机游走一次：风力越高越难继续上升（&gt;30 时上升概率递减），
 *       风力越低越难继续下降（&lt;12 时下降概率递减），自然形成 12~30 的均衡区间；</li>
 *   <li>风向每 128 tick 有概率左右偏转 18°；</li>
 *   <li>风力随高度按三次多项式变化：在 (海平面与建筑上限的中点) 处达到峰值系数 1，
 *       在建筑上限的 1.125 倍处衰减为 0，且峰值处导数为 0（系数由三个约束解出）；
 *       以主世界（上限 320、海平面 63）为例：y=64 约 0.54、y=140 约 0.93、y=191 峰值 1.0、y=320 约 0.44；</li>
 *   <li>雷暴 ×1.5、降雨 ×1.25，最终乘以全局风力系数换算为 MCU——中高空晴天的典型风力约 45~110 MCU，
 *       风暴峰值可超过 150 MCU（碳转子会过载，符合 IC2 的风险收益设计）。</li>
 * </ul>
 *
 * <p>全局风力系数可通过 <code>assets/mio_icif/config/core.ini</code> 的
 * <code>[wind] globalMultiplier</code> 调整（默认 4.0），对应 IC2 的
 * balance/energyfix/kineticgenerator/wind 配置位。
 *
 * <p>状态随存档持久化；随机游走以绝对游戏时间推进，加载存档或服务器重启后自动补齐
 * 期间的风力演化（有步数上限防止离线过久卡顿）。
 */
public final class WindSim extends SavedData {
    private static final String DATA_ID = "scex_wind_sim";
    /** IC2 的风场刷新间隔（tick）。 */
    public static final int UPDATE_INTERVAL = 128;
    /** 全局风力系数默认值（调高自 IC2 的 2.4，使中高空典型风力达到碳转子的有效工作区间）。 */
    public static final double DEFAULT_GLOBAL_MULTIPLIER = 4.0;
    /** 单次访问最多补算的演化步数，超出后直接对齐当前时间。 */
    private static final int MAX_CATCH_UP_STEPS = 4096;

    private ServerLevel level;
    /** 三次多项式系数 p(y) = c1*y + c2*y^2 + c3*y^3。 */
    private double[] coefficients;
    private int windStrength = -1;
    private int windDirection;
    private long lastProcessedTick = Long.MIN_VALUE;

    public WindSim() {}

    public static WindSim get(ServerLevel level) {
        WindSim sim = level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(WindSim::new, WindSim::load), DATA_ID);
        sim.attach(level);
        return sim;
    }

    /** DataStorage 按维度隔离实例；首次访问时绑定维度并初始化本地参数。 */
    private void attach(ServerLevel level) {
        if (this.level == level && coefficients != null) return;
        this.level = level;
        this.coefficients = calculateCoefficients(level.getMaxBuildHeight(), level.getSeaLevel());
        if (windStrength < 0) windStrength = 10 + level.random.nextInt(20);
        if (lastProcessedTick == Long.MIN_VALUE) lastProcessedTick = level.getGameTime();
    }

    public static WindSim load(CompoundTag tag, HolderLookup.Provider registries) {
        WindSim sim = new WindSim();
        sim.windStrength = tag.getInt("WindStrength");
        sim.windDirection = tag.getInt("WindDirection");
        sim.lastProcessedTick = tag.contains("LastProcessedTick") ? tag.getLong("LastProcessedTick") : Long.MIN_VALUE;
        return sim;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("WindStrength", windStrength);
        tag.putInt("WindDirection", windDirection);
        tag.putLong("LastProcessedTick", lastProcessedTick);
        return tag;
    }

    /** 将风场演化推进到当前游戏时间（每 128 tick 一步）。 */
    private void advance() {
        if (level == null) return;
        long now = level.getGameTime();
        if (lastProcessedTick == Long.MIN_VALUE) { lastProcessedTick = now; return; }
        int steps = 0;
        while (lastProcessedTick + UPDATE_INTERVAL <= now && steps < MAX_CATCH_UP_STEPS) {
            step();
            lastProcessedTick += UPDATE_INTERVAL;
            steps++;
        }
        if (lastProcessedTick + UPDATE_INTERVAL <= now) lastProcessedTick = now;
        if (steps > 0) setDirty();
    }

    private void step() {
        RandomSource random = level.random;
        int upChance = 10;
        int downChance = 10;
        // 均衡区间 12~30：超过 30 后上升概率递减，低于 12 后下降概率递减
        if (windStrength > 30) {
            upChance -= windStrength - 30;
        } else if (windStrength < 12) {
            downChance -= 12 - windStrength;
        }
        if (random.nextInt(100) < upChance) {
            windStrength++;
        } else if (random.nextInt(100) < downChance) {
            windStrength--;
        }
        switch (random.nextInt(3)) {
            case 0 -> windDirection = turn(windDirection, -18);
            case 2 -> windDirection = turn(windDirection, 18);
        }
    }

    private static int turn(int direction, int amount) {
        int next = direction + amount;
        if (next < 0) return 359 + next + 1;
        if (next > 359) return next - 360;
        return next;
    }

    /**
     * 求解高度系数：p(sh)=1（中点峰值）、p(fh)=0（1.125 倍建筑上限）、p'(sh)=0，Cramer 法则求解。
     */
    private static double[] calculateCoefficients(int height, int seaLevel) {
        double h = Math.max(1, height);
        double sea = Math.max(0, seaLevel);
        double baseHeight = sea < h ? sea : h * 0.5;
        double sh = baseHeight + (h - baseHeight) / 2.0;
        double fh = h * 1.125;
        double det = det3(sh, sh * sh, sh * sh * sh,
                          fh, fh * fh, fh * fh * fh,
                          1.0, 2.0 * sh, 3.0 * sh * sh);
        if (det == 0.0) return new double[]{0, 0, 0};
        double c1 = det3(1.0, sh * sh, sh * sh * sh,
                         0.0, fh * fh, fh * fh * fh,
                         0.0, 2.0 * sh, 3.0 * sh * sh) / det;
        double c2 = det3(sh, 1.0, sh * sh * sh,
                         fh, 0.0, fh * fh * fh,
                         1.0, 0.0, 3.0 * sh * sh) / det;
        double c3 = det3(sh, sh * sh, 1.0,
                         fh, fh * fh, 0.0,
                         1.0, 2.0 * sh, 0.0) / det;
        return new double[]{c1, c2, c3};
    }

    private static double det3(double a00, double a01, double a02,
                               double a10, double a11, double a12,
                               double a20, double a21, double a22) {
        return a00 * (a11 * a22 - a12 * a21)
             - a01 * (a10 * a22 - a12 * a20)
             + a02 * (a10 * a21 - a11 * a20);
    }

    /** 全局风力系数，读取 core.ini 的 [wind] globalMultiplier（默认 4.0，负值按 0 处理）。 */
    public static double globalMultiplier() {
        return Math.max(0.0, CoreConfig.getDouble("wind", "globalMultiplier", DEFAULT_GLOBAL_MULTIPLIER));
    }

    /** 指定高度的瞬时风力（MCU），不小于 0。 */
    public double getWindAt(double height) {
        advance();
        double multiplier = 0.0;
        if (coefficients != null) {
            multiplier = Math.max(0.0,
                coefficients[0] * height + coefficients[1] * height * height + coefficients[2] * height * height * height);
        }
        double ret = Math.max(0, windStrength) * multiplier;
        if (level != null && level.isThundering()) {
            ret *= 1.5;
        } else if (level != null && level.isRaining()) {
            ret *= 1.25;
        }
        return ret * globalMultiplier();
    }

    /** 当前基础风力强度（未经高度与天气修正）。 */
    public int getWindStrength() {
        advance();
        return Math.max(0, windStrength);
    }

    /** 当前风向（0~359 度）。 */
    public int getWindDirection() {
        return Math.floorMod(windDirection, 360);
    }
}
