package com.miophas.singularity_iteration.core.api.world;

import net.minecraft.world.level.Level;

/**
 * 全局风场 API
 *
 * <p>暴露 mio_icif 的全局风场模拟（WindSim）给附属模组。风场按维度独立维护并随存档持久化：
 * 基础风力强度做随机游走，查询时按高度三次多项式系数与天气（雷暴 ×1.5、降雨 ×1.25）换算，
 * 最终乘以全局系数得到该高度的瞬时风力（MCU）。
 *
 * <p>使用示例：
 * <pre>{@code
 * IWindAPI wind = MioIcifAPI.instance().getWindAPI();
 *
 * // 查询某高度的风力（决定风力动能发生机/风力相关机器的输出）
 * double windAt = wind.getWindAt(level, pos.getY() + 0.5);
 *
 * // 查询风场基础强度与风向（未经高度/天气修正）
 * int base = wind.getBaseWindStrength(level);
 * int direction = wind.getWindDirection(level);
 * }</pre>
 *
 * <p>所有方法仅在服务端有效；客户端世界一律返回 0。附属模组可据此实现
 * 自定义风力发电机、风力计、气象预测设备等。
 */
public interface IWindAPI {

    /**
     * 查询指定维度的全局风场在给定高度的瞬时风力。
     *
     * @param level  服务端世界
     * @param height 查询高度（通常为方块或实体的 Y 坐标）
     * @return 瞬时风力（MCU），不小于 0；客户端世界返回 0
     */
    double getWindAt(Level level, double height);

    /**
     * 查询风场的基础风力强度（随机游走原始值，未经高度与天气修正）。
     *
     * @param level 服务端世界
     * @return 基础风力强度；客户端世界返回 0
     */
    int getBaseWindStrength(Level level);

    /**
     * 查询当前风向。
     *
     * @param level 服务端世界
     * @return 风向角度（0~359 度）；客户端世界返回 0
     */
    int getWindDirection(Level level);
}
