package com.singularity_iteration.mio_icif.Items.Reactor;

@SuppressWarnings("null")
public class mio_icif_advanced_heat_vent extends mio_icif_heat_vent {

    public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 12;
    public static final int REACTOR_ABSORPTION = 0;

    public mio_icif_advanced_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

}