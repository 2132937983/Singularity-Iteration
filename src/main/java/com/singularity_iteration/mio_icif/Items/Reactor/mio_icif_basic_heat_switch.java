package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 基础热交换器
 *
 * 特性：
 * - 自身可以储存2500点热�? * - 向相邻其他元件传�?4点热�? * - 向核反应堆自身传�?点热�? * - 正常工作时会显示耐久�? * - 超过自身热量上限耐久度将会变化?（消失）
 */
@SuppressWarnings("null")
public class mio_icif_basic_heat_switch extends mio_icif_heat_switch {

    // 最大热量存�
public static final int MAX_HEAT = 2500;

    // 向相邻元件传递的热量
    public static final int TRANSFER_TO_ADJACENT = 12;

    // 向核反应堆传递的热量
    public static final int TRANSFER_TO_REACTOR = 4;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_basic_heat_switch(Properties properties) {
        super(properties, MAX_HEAT, TRANSFER_TO_ADJACENT, TRANSFER_TO_REACTOR);
    }
}


