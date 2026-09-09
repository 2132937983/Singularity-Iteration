package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 青金石冷凝模式? *
 * 特性：
 * - 能够将热量抛散至异次元空气? * - 能够吸收附近元件100,000点热�? * - 不能被其他元件（如散热片）散�? * - 将青金石冷凝模块与红石在工作台内合成可将其修复? * - 每个红石修复冷凝模块10000点耐久
 * - 每个青金石修复冷凝模式?0000点耐久
 * - 正常工作时会显示耐久�? * - 满热后无法继续吸热，但不会消耗? */
@SuppressWarnings("null")
public class mio_icif_lapis_condensator extends mio_icif_condensator {

    // 最大热量存�
public static final int MAX_HEAT = 100000;

    // 每tick吸收的热�
public static final int ABSORPTION_RATE = 2; // 每tick吸收2点（已翻倍）

    // 红石修复�
public static final int REDSTONE_REPAIR = 10000;

    // 青金石修复量
    public static final int LAPIS_REPAIR = 40000;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_lapis_condensator(Properties properties) {
        super(properties, MAX_HEAT, ABSORPTION_RATE, REDSTONE_REPAIR, LAPIS_REPAIR);
    }
}


