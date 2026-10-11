package com.miophas.singularity_iteration.common.item.tools;

/**
 * Small power unit.
 * Extends the electric tool base class.
 * It stores less energy than the standard power unit. It also uses less energy.
 * It is a crafting component for small electric tools.
 */
@SuppressWarnings("null")
public class mio_icif_power_unit_small extends mio_icif_tool_elc {

    // 小型驱动把手默认最大能量（比标准版少）
    public static final int POWER_UNIT_SMALL_MAX_ENERGY = 5000;

    // Default energy that the small power unit uses each time (less than the standard power unit).
public static final int POWER_UNIT_SMALL_ENERGY_PER_USE = 25;

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性
 */
    public mio_icif_power_unit_small(Properties properties) {
        super(properties, POWER_UNIT_SMALL_MAX_ENERGY, POWER_UNIT_SMALL_MAX_ENERGY, "power_unit_small", POWER_UNIT_SMALL_MAX_ENERGY, POWER_UNIT_SMALL_ENERGY_PER_USE, 1);
    }

    /**
     * 带初始能量的构造函数
 * @param properties 物品属性
 * @param initialEnergy 初始能量
     */
    public mio_icif_power_unit_small(Properties properties, int initialEnergy) {
        super(properties, POWER_UNIT_SMALL_MAX_ENERGY, POWER_UNIT_SMALL_MAX_ENERGY - initialEnergy, "power_unit_small", POWER_UNIT_SMALL_MAX_ENERGY, POWER_UNIT_SMALL_ENERGY_PER_USE, 1);
    }

    /**
     * 带最大能量和初始能量的构造函数
 * @param properties 物品属性
 * @param maxEnergy 最大能量
 * @param initialEnergy 初始能量
     */
    public mio_icif_power_unit_small(Properties properties, int maxEnergy, int initialEnergy) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit_small", maxEnergy, POWER_UNIT_SMALL_ENERGY_PER_USE, 1);
    }

    /**
     * 完整参数的构造函数
 * @param properties 物品属性
 * @param maxEnergy 最大能量
 * @param initialEnergy 初始能量
     * @param chargeRate 充电速率
     * @param energyPerUse 每次使用消耗的能量
     * @param toolTier 工具等级
     */
    public mio_icif_power_unit_small(Properties properties, int maxEnergy, int initialEnergy, int chargeRate, int energyPerUse, int toolTier) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit_small", chargeRate, energyPerUse, toolTier);
    }
}

