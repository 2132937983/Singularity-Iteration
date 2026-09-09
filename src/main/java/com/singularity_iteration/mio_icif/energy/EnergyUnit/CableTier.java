/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 * Copyright (c) 2024 Industrial_Craft_In_Future
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.singularity_iteration.mio_icif.energy.EnergyUnit;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 电压等级定义 - 所有电线相关参数在此处统一配置
 *
 * <p>与IC2原版电压等级体系一致：</p>
 * <pre>
 *   LV = 32 EU/t   (tier 0) - Low Voltage / 低压
 *   MV = 128 EU/t  (tier 1) - Medium Voltage / 中压
 *   HV = 512 EU/t  (tier 2) - High Voltage / 高压
 *   EV = 2048 EU/t (tier 3) - Extreme Voltage / 超高压
 *   IV = 8192 EU/t (tier 4) - Insane Voltage / 超高电压
 *   LuV = 32768 EU/t (tier 5) - Ludicrous Voltage / 剧差压
 *   ZPMV = 131072 EU/t (tier 6) - Zero Point Module Voltage / 零点压
 *   UV = 524288 EU/t (tier 7) - Ultimate Voltage / 极高压
 *   UHV = 2097152 EU/t (tier 8) - Ultra High Voltage / 超极限压
 *   UEV = 8388608 EU/t (tier 9) - Ultra Excessive Voltage / 超极限压
 *   UIV = 33554432 EU/t (tier 10) - Ultra Immense Voltage / 极强导压
 *   UXV = 134217728 EU/t (tier 11) - Ultra Extreme Voltage / 极上拓压
 *   OpV = 536870912 EU/t (tier 12) - Overpowered Voltage / 过载压
 *   MAX = 2147483647 EU/t (tier 13) - Maximum Voltage / 终压
 * </pre>
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>{@link #powerRating} - 电压等级（EU/t），决定机器可接受的能量传输速率</li>
 *   <li>{@link #electricDamage} - 裸线触电伤害值（绝缘电线为 0）</li>
 *   <li>{@link #conductorBreakdownEnergy} - 导体熔毁能量阈值（超过此值导体被摧毁）</li>
 *   <li>{@link #insulationBreakdownEnergy} - 绝缘层熔毁能量阈值（超过此值绝缘层被剥离）</li>
 *   <li>{@link #insulationEnergyAbsorption} - 裸线绝缘吸收阈值（低于此值绝缘层完全吸收能量）</li>
 *   <li>{@link #conductionLoss} - 裸线传导损耗比例</li>
 *   <li>{@link #insulatedInsulationEnergyAbsorption} - 绝缘电线绝缘吸收阈值</li>
 *   <li>{@link #insulatedConductionLoss} - 绝缘电线传导损耗比例</li>
 * </ul>
 *
 * <p><b>修改电线参数只需修改此类的实例定义即可。</b></p>
 */
@SuppressWarnings("null")
public final class CableTier implements Comparable<CableTier>, ICableTier {

    // ============ 电压等级实例 ============

    /** LV 低压：32 EU/t，触电伤害 4 点 */
    public static CableTier LV = new CableTier(
        "lv", "LV", "Low Voltage",
        32, 0, 4.0f,
        33L,       // conductorBreakdownEnergy: 32 + 1
        9001L,     // insulationBreakdownEnergy
        8.0,       // insulationEnergyAbsorption (bare): 32 / 4
        0.002,     // conductionLoss (bare)
        32.0,      // insulatedInsulationEnergyAbsorption: full voltage
        0.0015     // insulatedConductionLoss
    );

    /** MV 中压：128 EU/t，触电伤害 8 点 */
    public static CableTier MV = new CableTier(
        "mv", "MV", "Medium Voltage",
        128, 1, 8.0f,
        129L,
        9001L,
        32.0,      // 128 / 4
        0.003,
        128.0,
        0.002
    );

    /** HV 高压：512 EU/t，触电伤害 18 点 */
    public static CableTier HV = new CableTier(
        "hv", "HV", "High Voltage",
        512, 2, 18.0f,
        513L,
        9001L,
        128.0,     // 512 / 4
        0.005,
        512.0,
        0.0045
    );

    /** EV 超高压：2048 EU/t，触电伤害 40 点 */
    public static CableTier EV = new CableTier(
        "ev", "EV", "Extreme Voltage",
        2048, 3, 40.0f,
        2049L,
        9001L,
        512.0,     // 2048 / 4
        0.01,
        2048.0,
        0.0095
    );

    /** IV 超高电压：8192 EU/t，无触电伤害（玻璃电缆，绝缘性好，电阻 0.02） */
    public static CableTier IV = new CableTier(
        "iv", "IV", "Insane Voltage",
        8192, 4, 0.0f,
        8193L,
        9001L,
        2048.0,
        0.02,
        8192.0,
        0.02
    );

    /** LuV 剧差压：32768 EU/t，无触电伤害（超导合金，完全绝缘零损耗） */
    public static CableTier LuV = new CableTier(
        "luv", "LuV", "Ludicrous Voltage",
        32768, 5, 0.0f,
        32769L,
        9001L,
        8192.0,
        0.0,
        32768.0,
        0.0
    );

    /** ZPMV 零点压：131072 EU/t，无触电伤害（零点模块，完全绝缘零损耗） */
    public static CableTier ZPMV = new CableTier(
        "zpmv", "ZPMV", "Zero Point Module Voltage",
        131072, 6, 0.0f,
        131073L,
        9001L,
        32768.0,
        0.0,
        131072.0,
        0.0
    );

    /** UV 极高压：524288 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier UV = new CableTier(
        "uv", "UV", "Ultimate Voltage",
        524288, 7, 0.0f,
        524289L,
        9001L,
        131072.0,
        0.0,
        524288.0,
        0.0
    );

    /** UHV 超极限压：2097152 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier UHV = new CableTier(
        "uhv", "UHV", "Ultra High Voltage",
        2097152, 8, 0.0f,
        2097153L,
        9001L,
        524288.0,
        0.0,
        2097152.0,
        0.0
    );

    /** UEV 超极限压：8388608 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier UEV = new CableTier(
        "uev", "UEV", "Ultra Excessive Voltage",
        8388608, 9, 0.0f,
        8388609L,
        9001L,
        2097152.0,
        0.0,
        8388608.0,
        0.0
    );

    /** UIV 极强导压：33554432 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier UIV = new CableTier(
        "uiv", "UIV", "Ultra Immense Voltage",
        33554432, 10, 0.0f,
        33554433L,
        9001L,
        8388608.0,
        0.0,
        33554432.0,
        0.0
    );

    /** UXV 极上拓压：134217728 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier UXV = new CableTier(
        "uxv", "UXV", "Ultra Extreme Voltage",
        134217728, 11, 0.0f,
        134217729L,
        9001L,
        33554432.0,
        0.0,
        134217728.0,
        0.0
    );

    /** OpV 过载压：536870912 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier OpV = new CableTier(
        "opv", "OpV", "Overpowered Voltage",
        536870912, 12, 0.0f,
        536870913L,
        9001L,
        134217728.0,
        0.0,
        536870912.0,
        0.0
    );

    /** MAX 终压：2147483647 EU/t，无触电伤害（超导，完全绝缘零损耗） */
    public static CableTier MAX = new CableTier(
        "max", "MAX", "Maximum Voltage",
        2147483647L, 13, 0.0f,
        2147483647L,
        9001L,
        536870912.0,
        0.0,
        2147483647.0,
        0.0
    );

    // ============ 字段 ============

    /** 内部名称（如"lv", "mv"） */
    public final String name;
    /** 短显示名（如"LV", "MV"） */
    public final String shortEnglishName;
    /** 长显示名（如"Low Voltage"） */
    public final String longEnglishName;
    /** 电压等级（EU/t） */
    public final long powerRating;
    /** 等级索引 0=LV, 1=MV, 2=HV, 3=EV, 4=IV, 5=LuV */
    public final int tierIndex;
    /** 是否内置等级 */
    public final boolean builtin;

    // --- 电线电气参数 ---

    /** 触电伤害值（0表示无伤害，绝缘电线始终为0） */
    public final float electricDamage;
    /** 导体熔毁能量阈值（超过此值导体本身被摧毁） */
    public final long conductorBreakdownEnergy;
    /** 绝缘层熔毁能量阈值（超过此值绝缘层被剥离） */
    public final long insulationBreakdownEnergy;
    /** 裸线绝缘吸收阈值（低于此值绝缘层完全吸收，高于此值产生触电伤害） */
    public final double insulationEnergyAbsorption;
    /** 裸线传导损耗比例 */
    public final double conductionLoss;
    /** 绝缘电线绝缘吸收阈值 */
    public final double insulatedInsulationEnergyAbsorption;
    /** 绝缘电线传导损耗比例 */
    public final double insulatedConductionLoss;

    // ============ 构造函数 ============

    /**
     * 创建电压等级（包含所有电线电气参数）
     *
     * @param name                            内部名称
     * @param shortEnglishName                短显示名
     * @param longEnglishName                 长显示名
     * @param powerRating                     电压等级（EU/t）
     * @param tierIndex                       等级索引
     * @param electricDamage                  触电伤害值
     * @param conductorBreakdownEnergy        导体熔毁能量
     * @param insulationBreakdownEnergy       绝缘层熔毁能量
     * @param insulationEnergyAbsorption      裸线绝缘吸收阈值
     * @param conductionLoss                  裸线传导损耗
     * @param insulatedInsulationEnergyAbsorption 绝缘电线绝缘吸收阈值
     * @param insulatedConductionLoss         绝缘电线传导损耗
     */
    public CableTier(String name, String shortEnglishName, String longEnglishName,
                     long powerRating, int tierIndex, float electricDamage,
                     long conductorBreakdownEnergy,
                     long insulationBreakdownEnergy,
                     double insulationEnergyAbsorption,
                     double conductionLoss,
                     double insulatedInsulationEnergyAbsorption,
                     double insulatedConductionLoss) {
        if (powerRating < 0) {
            throw new IllegalArgumentException("Voltage rating must be non-negative");
        }

        this.name = name;
        this.shortEnglishName = shortEnglishName;
        this.longEnglishName = longEnglishName;
        this.powerRating = powerRating;
        this.tierIndex = tierIndex;
        this.builtin = true;
        this.electricDamage = electricDamage;
        this.conductorBreakdownEnergy = conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = Math.max(insulationBreakdownEnergy, conductorBreakdownEnergy);
        this.insulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulationEnergyAbsorption;
        this.conductionLoss = conductionLoss;
        this.insulatedInsulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulatedInsulationEnergyAbsorption;
        this.insulatedConductionLoss = insulatedConductionLoss;
    }

    /**
     * 创建自定义电压等级（可指定是否为内置）
     */
    public CableTier(String name, String shortEnglishName, String longEnglishName,
                     long powerRating, int tierIndex, float electricDamage,
                     long conductorBreakdownEnergy,
                     long insulationBreakdownEnergy,
                     double insulationEnergyAbsorption,
                     double conductionLoss,
                     double insulatedInsulationEnergyAbsorption,
                     double insulatedConductionLoss,
                     boolean builtin) {
        if (powerRating < 0) {
            throw new IllegalArgumentException("Voltage rating must be non-negative");
        }

        this.name = name;
        this.shortEnglishName = shortEnglishName;
        this.longEnglishName = longEnglishName;
        this.powerRating = powerRating;
        this.tierIndex = tierIndex;
        this.builtin = builtin;
        this.electricDamage = electricDamage;
        this.conductorBreakdownEnergy = conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = Math.max(insulationBreakdownEnergy, conductorBreakdownEnergy);
        this.insulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulationEnergyAbsorption;
        this.conductionLoss = conductionLoss;
        this.insulatedInsulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulatedInsulationEnergyAbsorption;
        this.insulatedConductionLoss = insulatedConductionLoss;
    }

    // ============ Getter 方法 ============

    public long getPowerRating() {
        return powerRating;
    }

    public int getTierIndex() {
        return tierIndex;
    }

    @Override
    public int getTier() {
        return tierIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return shortEnglishName;
    }

    @Override
    public String getFullName() {
        return longEnglishName;
    }

    @Override
    public float getElectricDamage() {
        return electricDamage;
    }

    @Override
    public long getConductorBreakdownEnergy() {
        return conductorBreakdownEnergy;
    }

    @Override
    public long getInsulationBreakdownEnergy() {
        return insulationBreakdownEnergy;
    }

    /** @deprecated 使用 {@link #getPowerRating()} 替代 */
    @Deprecated
    public long getMaxTransfer() {
        return powerRating;
    }

    /** @deprecated 使用 {@link #getPowerRating()} 替代 */
    @Deprecated
    public long getEu() {
        return powerRating;
    }

    // ============ 显示名 ============

    public String shortEnglishKey() {
        return "cable_tier_short.mio_icif." + name;
    }

    public MutableComponent shortEnglishName() {
        return Component.translatable(shortEnglishKey());
    }

    public String longEnglishKey() {
        return "cable_tier_long.mio_icif." + name;
    }

    public MutableComponent longEnglishName() {
        return Component.translatable(longEnglishKey());
    }

    // ============ 比较 ============

    @Override
    public int compareTo(CableTier other) {
        return Long.compare(powerRating, other.powerRating);
    }

    @Override
    public String toString() {
        return name;
    }

    // ============ 注册表 ============

    private final static Map<String, CableTier> tiers = new HashMap<>();
    private static List<CableTier> cachedAllTiers;

    /**
     * 注册新的电压等级。如果已存在同名或同电压等级的等级，则抛出异常。
     */
    public static void addTier(CableTier tier) {
        for (var existingTier : tiers.values()) {
            if (existingTier.name.equals(tier.name)) {
                throw new IllegalArgumentException("Tier " + tier + " already exists!");
            }
            if (existingTier.powerRating == tier.powerRating) {
                throw new IllegalArgumentException("A tier with voltage rating " + tier.powerRating + " already exists!");
            }
        }
        tiers.put(tier.name, tier);
    }

    /**
     * 根据名称查找已注册的电压等级。
     */
    public static CableTier getTier(String name) {
        CableTier existing = tiers.get(name);
        if (existing == null) {
            throw new NoSuchElementException("No such cable tier: " + name);
        }
        return existing;
    }

    public static CableTier fromICableTier(ICableTier tier) {
        if (tier instanceof CableTier ct) return ct;
        return getTier(tier.getName());
    }

    /**
     * @return 所有已注册的电压等级，按电压等级排序。
     */
    public static List<CableTier> allTiers() {
        if (cachedAllTiers == null) {
            cachedAllTiers = tiers.values().stream().sorted().toList();
        }
        return cachedAllTiers;
    }

    static {
        addTier(LV);
        addTier(MV);
        addTier(HV);
        addTier(EV);
        addTier(IV);
        addTier(LuV);
        addTier(ZPMV);
        addTier(UV);
        addTier(UHV);
        addTier(UEV);
        addTier(UIV);
        addTier(UXV);
        addTier(OpV);
        addTier(MAX);
        cachedAllTiers = tiers.values().stream().sorted().toList();
    }
}