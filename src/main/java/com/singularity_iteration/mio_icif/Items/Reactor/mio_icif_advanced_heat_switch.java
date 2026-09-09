package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 高级热交换器
 *
 * 特性：
 * - 自身可以储存10000点热�? * - 向相邻其他元件交换?8点热�? * - 与核反应堆交换?6点热�? * - 正常工作时会显示耐久�? * - 超过自身热量上限耐久度将会变化?（消失）
 */
@SuppressWarnings("null")
public class mio_icif_advanced_heat_switch extends mio_icif_heat_switch {

    // 最大热量存�
public static final int MAX_HEAT = 10000;

    // 向相邻元件传递的热量
    public static final int TRANSFER_TO_ADJACENT = 24;

    // 向核反应堆传递的热量
    public static final int TRANSFER_TO_REACTOR = 8;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_advanced_heat_switch(Properties properties) {
        super(properties, MAX_HEAT, TRANSFER_TO_ADJACENT, TRANSFER_TO_REACTOR);
    }
}


