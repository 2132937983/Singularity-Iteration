package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Basic heat exchanger (reactor component).
 *
 * Properties:
 * - It stores up to {@link #MAX_HEAT} (2500) heat.
 * - It moves up to {@link #TRANSFER_TO_ADJACENT} heat per cycle with each adjacent component.
 * - It moves up to {@link #TRANSFER_TO_REACTOR} heat per cycle with the reactor hull.
 * - During normal operation, the durability bar shows the stored heat.
 * - When the stored heat goes above the limit, the component is destroyed.
 */
@SuppressWarnings("null")
public class mio_icif_basic_heat_switch extends mio_icif_heat_switch {

    // Maximum heat that this component stores.
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


