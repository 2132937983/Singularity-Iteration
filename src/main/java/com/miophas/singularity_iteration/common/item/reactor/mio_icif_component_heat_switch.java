package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Component heat exchanger.
 *
 * Properties:
 * - It can store 5000 heat.
 * - It does not exchange heat with the reactor (it transfers 0 heat).
 * - It transfers 36 heat only to other adjacent components.
 * - It shows durability during normal operation.
 * - If its heat goes above its heat limit, its durability becomes 0 and it disappears.
 */
@SuppressWarnings("null")
public class mio_icif_component_heat_switch extends mio_icif_heat_switch {

    // Maximum heat storage.
public static final int MAX_HEAT = 5000;

    // Heat that it transfers to adjacent components.
    public static final int TRANSFER_TO_ADJACENT = 36;

    // Heat that it transfers to the reactor. The component heat exchanger does not transfer heat to the reactor.
    public static final int TRANSFER_TO_REACTOR = 0;

    /**
     * Constructor.
 * @param properties the item properties
 */
    public mio_icif_component_heat_switch(Properties properties) {
        super(properties, MAX_HEAT, TRANSFER_TO_ADJACENT, TRANSFER_TO_REACTOR);
    }
}


