package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.tuning.SiTuningValues;

/**
 * OD scanner (ore density): the first tier of {@link OreScannerItem}, a 13×13×13 box, 48 EU a scan, 100,000 EU. The
 * miner takes it as its small scanner. Its numbers are datapack tuning ({@code si_tuning/od_scanner.json}).
 */
public class mio_icif_od_scanner extends OreScannerItem {
    public static final int OD_SCANNER_MAX_ENERGY = 100000;
    public static final int OD_SCANNER_ENERGY_PER_USE = 48;

    public mio_icif_od_scanner(Properties properties) {
        super(properties, OD_SCANNER_MAX_ENERGY, OD_SCANNER_ENERGY_PER_USE, "od_scanner",
            SiTuningValues.OD_SCAN_RADIUS, SiTuningValues.OD_Y_RADIUS, SiTuningValues.OD_ENERGY_PER_USE);
    }
}
