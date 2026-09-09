package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 元件热交换器
 *
 * 特性：
 * - 自身可以储存5000点热? * - 不会与核反应自身交换热量（传?点）
 * - 只会向相邻其他元件传?2点热? * - 正常工作时会显示耐久? * - 超过自身热量上限耐久度将会变?（消失）
 */
@SuppressWarnings("null")
public class mio_icif_component_heat_switch extends mio_icif_heat_switch {

    // 最大热量存
public static final int MAX_HEAT = 5000;

    // 向相邻元件传递的热量
    public static final int TRANSFER_TO_ADJACENT = 36;

    // 向核反应堆传递的热量（元件热交换器不向反应堆传递）
    public static final int TRANSFER_TO_REACTOR = 0;

    /**
     * 构造函数
 * @param properties 物品属
 */
    public mio_icif_component_heat_switch(Properties properties) {
        super(properties, MAX_HEAT, TRANSFER_TO_ADJACENT, TRANSFER_TO_REACTOR);
    }
}


