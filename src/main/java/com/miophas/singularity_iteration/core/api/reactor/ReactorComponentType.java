package com.miophas.singularity_iteration.core.api.reactor;

/**
 * Nuclear reactor component type.
 * Equivalent to the internal mio_icif_reactor.ReactorComponentType.
 */
public enum ReactorComponentType {
    FUEL_ROD,
    HEAT_SINK,
    COOLER,
    HEAT_EXCHANGER,
    HEAT_SWITCH,
    CONDENSATOR,
    NEUTRON_REFLECTOR,
    REACTOR_PLATING,
    OTHER
}

