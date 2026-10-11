package com.miophas.singularity_iteration.common.item.reactor;

/**
 * Reactor heat exchanger.
 *
 * Properties:
 * - It can store 5000 heat.
 * - It does not exchange heat with adjacent components (it transfers 0 heat to them).
 * - It transfers heat only to the reactor (TRANSFER_TO_REACTOR heat).
 * - It shows its durability during normal operation.
 * - When its heat goes above its heat limit, its durability becomes 0 and the item disappears.
 */
@SuppressWarnings("null")
public class mio_icif_reactor_heat_switch extends mio_icif_heat_switch {

    // Maximum heat storage.
public static final int MAX_HEAT = 5000;

    // Heat transferred to adjacent components. The reactor heat exchanger does not transfer heat to adjacent components.
    public static final int TRANSFER_TO_ADJACENT = 0;

    // Heat transferred to the reactor.
    public static final int TRANSFER_TO_REACTOR = 72;

    /**
     * Constructor.
 * @param properties the item properties
 */
    public mio_icif_reactor_heat_switch(Properties properties) {
        super(properties, MAX_HEAT, TRANSFER_TO_ADJACENT, TRANSFER_TO_REACTOR);
    }
}


