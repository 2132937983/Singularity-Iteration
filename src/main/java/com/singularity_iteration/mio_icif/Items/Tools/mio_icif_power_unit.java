package com.singularity_iteration.mio_icif.Items.Tools;

/**
 * 驱动把手
 * 继承电力工具基础值?
 * 用于合成采矿钻头、电锯等电力工具的组�?
 */
@SuppressWarnings("null")
public class mio_icif_power_unit extends mio_icif_tool_elc {

    // 驱动把手默认最大能量?
    public static final int POWER_UNIT_MAX_ENERGY = 10000;

    // 驱动把手默认每次使用消耗的能量
    public static final int POWER_UNIT_ENERGY_PER_USE = 50;

    /**
     * 默认构造函数（空电状态）
     * @param properties 物品属性?
     */
    public mio_icif_power_unit(Properties properties) {
        super(properties, POWER_UNIT_MAX_ENERGY, POWER_UNIT_MAX_ENERGY, "power_unit", POWER_UNIT_MAX_ENERGY, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * 带初始能量的构造函数?
     * @param properties 物品属性?
     * @param initialEnergy 初始能量
     */
    public mio_icif_power_unit(Properties properties, int initialEnergy) {
        super(properties, POWER_UNIT_MAX_ENERGY, POWER_UNIT_MAX_ENERGY - initialEnergy, "power_unit", POWER_UNIT_MAX_ENERGY, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * 带最大能量和初始能量的构造函数?
     * @param properties 物品属性?
     * @param maxEnergy 最大能量?
     * @param initialEnergy 初始能量
     */
    public mio_icif_power_unit(Properties properties, int maxEnergy, int initialEnergy) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit", maxEnergy, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * 完整参数的构造函数?
     * @param properties 物品属性?
     * @param maxEnergy 最大能量?
     * @param initialEnergy 初始能量
     * @param chargeRate 充电速率
     * @param energyPerUse 每次使用消耗的能量
     * @param toolTier 工具等级
     */
    public mio_icif_power_unit(Properties properties, int maxEnergy, int initialEnergy, int chargeRate, int energyPerUse, int toolTier) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit", chargeRate, energyPerUse, toolTier);
    }
}

