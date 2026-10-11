package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Basic heat vent.
 *
 * Behavior (IC2 reference):
 * - Maximum heat storage: 1000.
 * - Self cooling: 6 heat per tick.
 * - It absorbs heat from adjacent fuel rods.
 * - It melts and disappears when its heat reaches the maximum.
 *
 * How it works:
 * - Each tick, it absorbs heat from adjacent fuel rods.
 * - At the same time, it vents 6 heat per tick to the environment.
 * - If it absorbs more heat than it vents, the heat accumulates.
 * - Stored heat is the inverse of durability. More heat gives less durability.
 * - When the heat reaches MAX_HEAT (1000), the vent melts and is consumed.
 */
@SuppressWarnings("null")
public class mio_icif_basic_heat_vent extends mio_icif_heat_vent {

    // 基础散热片参数
public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 6;
    public static final int REACTOR_ABSORPTION = 0;    // 不从反应堆直接吸热，而是从周围燃料棒吸热

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_basic_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

}


