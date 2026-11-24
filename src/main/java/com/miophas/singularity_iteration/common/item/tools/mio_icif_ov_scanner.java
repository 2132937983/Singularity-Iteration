package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.tuning.SiTuningValues;

/**
 * OV scanner (ore value): the second tier of {@link OreScannerItem}, crafted from an OD scanner (its charge carried over),
 * a 25×13×25 box, 96 EU a scan, 1,000,000 EU. The miner takes it as its large scanner. Datapack tuning:
 * {@code si_tuning/ov_scanner.json}.
 */
public class mio_icif_ov_scanner extends OreScannerItem {
    public static final int OV_SCANNER_MAX_ENERGY = 1000000;
    public static final int OV_SCANNER_ENERGY_PER_USE = 96;

    public mio_icif_ov_scanner(Properties properties) {
        super(properties, OV_SCANNER_MAX_ENERGY, OV_SCANNER_ENERGY_PER_USE, "ov_scanner",
            SiTuningValues.OV_SCAN_RADIUS, SiTuningValues.OV_Y_RADIUS, SiTuningValues.OV_ENERGY_PER_USE);
    }
}
