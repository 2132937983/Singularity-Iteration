package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 红石冷凝模块
 *
 * 特性：
 * - 能够吸收附近元件20,000点热�? * - 不能被其他元件（如散热片）散�? * - 使用红石和冷凝模块在工作台上合成可将其修复? * - 每个红石修复冷凝模块10000点耐久
 * - 正常工作时会显示耐久�? * - 满热后无法继续吸热，但不会消耗? */
@SuppressWarnings("null")
public class mio_icif_redstone_condensator extends mio_icif_condensator {

    // 最大热量存�
public static final int MAX_HEAT = 20000;

    // 每tick吸收的热�
public static final int ABSORPTION_RATE = 2; // 每tick吸收2点（已翻倍）

    // 红石修复�
public static final int REDSTONE_REPAIR = 10000;

    // 青金石修复量（红石冷凝模块不使用青金石修复）
    public static final int LAPIS_REPAIR = 0;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_redstone_condensator(Properties properties) {
        super(properties, MAX_HEAT, ABSORPTION_RATE, REDSTONE_REPAIR, LAPIS_REPAIR);
    }
}


