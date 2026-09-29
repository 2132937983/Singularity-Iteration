package com.miophas.singularity_iteration.common.item.reactor;

/** 青金石冷凝器：容量 100000；每红石修复 5000，每青金石修复 40000。 */
public class mio_icif_lapis_condensator extends mio_icif_condensator {
    public static final int MAX_HEAT = 100000;
    // Legacy item API metadata; the authoritative reactor cycle receives heat from neighbours.
    public static final int ABSORPTION_RATE = 2;
    public static final int REDSTONE_REPAIR = 5000;
    public static final int LAPIS_REPAIR = 40000;

    public mio_icif_lapis_condensator(Properties properties) {
        super(properties, MAX_HEAT, ABSORPTION_RATE, REDSTONE_REPAIR, LAPIS_REPAIR);
    }
}
