package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Overclocked heat vent.
 *
 * Properties (based on IC2):
 * - Maximum heat storage: 1000.
 * - Self cooling: 20 heat per tick.
 * - Heat absorption from the reactor: 36 heat per tick.
 * - It does not absorb heat from fuel rods.
 * - The absorption is higher than the cooling. Excess heat collects in the vent.
 * - Use a heat exchanger or component heat vents to remove the excess heat.
 * - The vent melts when its heat exceeds the maximum.
 *
 * Recommendations:
 * - Put component heat vents on all sides.
 * - In a fluid reactor, make sure that the coolant flows freely.
 * - Use pulse mode (run and stop the reactor in cycles) to prevent overheating.
 */
@SuppressWarnings("null")
public class mio_icif_overclocked_heat_vent extends mio_icif_heat_vent {

    // 超频散热片参数
public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 20;
    public static final int REACTOR_ABSORPTION = 36;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_overclocked_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

}


