package com.miophas.singularity_iteration.common.energy;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSubstitutions;

/** 内置方块接入独立能源平台的登记清单；替换机制本身在 core。 */
public final class BuiltinIndependentSubstitutions {

    private BuiltinIndependentSubstitutions() {}

    public static void install() {
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_lv_mv", "mio_icif:transformer_lv_mv", 32);
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_mv_hv", "mio_icif:transformer_mv_hv", 128);
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_hv_ev", "mio_icif:transformer_hv_ev", 512);
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_ev_sc", "mio_icif:transformer_ev_sc", 2048);
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_iv_luv", "mio_icif:transformer_iv_luv", 8192);
        IndependentSubstitutions.registerTransformer("mio_icif:wiring/transformer_luv_zpmv", "mio_icif:transformer_luv_zpmv", 32768);
        IndependentSubstitutions.registerCable("mio_icif:wiring/block_eu_detector_cable", "mio_icif:wire_detector", true);
        IndependentSubstitutions.registerCable("mio_icif:wiring/block_eu_splitter_cable", "mio_icif:wire_splitter", false);
    }
}