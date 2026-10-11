// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.tuning;

import static com.miophas.singularity_iteration.common.tuning.SiTuning.intValue;

/**
 * All tuning values of the mod in one place (0.1.7.36 pilot). See {@link SiTuning} for the file
 * format and the rules.
 *
 * <p>Why one class: the reload check must know every declared key before it can warn about an
 * unknown key. A handle in a machine class would only exist after that class loads. The table
 * also gives a pack maker one list of the values that a datapack can change.
 *
 * <p>Rule for each entry: the default must be equal to the value in the bundled file
 * {@code data/mio_icif/si_tuning/<file>.json}, and to the old constant that it replaces.
 */
public final class SiTuningValues {
    private SiTuningValues() {}

    /** Loads this class, so that every handle below is declared. */
    static void init() {}

    // ---------------------------------------------------------------- OV Scanner (tool)
    public static final SiTuning.IntValue OV_SCAN_RADIUS = intValue("ov_scanner", "scan_radius", 12, 1, 32);
    public static final SiTuning.IntValue OV_Y_RADIUS = intValue("ov_scanner", "y_radius", 6, 1, 32);
    public static final SiTuning.IntValue OV_ENERGY_PER_USE = intValue("ov_scanner", "energy_per_use", 96, 0, 1_000_000);

    // ---------------------------------------------------------------- OD Scanner (tool)
    public static final SiTuning.IntValue OD_SCAN_RADIUS = intValue("od_scanner", "scan_radius", 6, 1, 32);
    public static final SiTuning.IntValue OD_Y_RADIUS = intValue("od_scanner", "y_radius", 6, 1, 32);
    public static final SiTuning.IntValue OD_ENERGY_PER_USE = intValue("od_scanner", "energy_per_use", 48, 0, 100_000);

    // ---------------------------------------------------------------- Miner (machine)
    public static final SiTuning.IntValue MINER_IRON_DRILL_MIN = intValue("miner", "iron_drill_energy_min", 450, 0, 100_000);
    public static final SiTuning.IntValue MINER_IRON_DRILL_MAX = intValue("miner", "iron_drill_energy_max", 470, 0, 100_000);
    public static final SiTuning.IntValue MINER_DIAMOND_DRILL_MIN = intValue("miner", "diamond_drill_energy_min", 880, 0, 100_000);
    public static final SiTuning.IntValue MINER_DIAMOND_DRILL_MAX = intValue("miner", "diamond_drill_energy_max", 900, 0, 100_000);
    public static final SiTuning.IntValue MINER_IRIDIUM_DRILL_MIN = intValue("miner", "iridium_drill_energy_min", 1500, 0, 100_000);
    public static final SiTuning.IntValue MINER_IRIDIUM_DRILL_MAX = intValue("miner", "iridium_drill_energy_max", 1600, 0, 100_000);
    public static final SiTuning.IntValue MINER_OD_SCANNER_MIN = intValue("miner", "od_scanner_energy_min", 45, 0, 100_000);
    public static final SiTuning.IntValue MINER_OD_SCANNER_MAX = intValue("miner", "od_scanner_energy_max", 75, 0, 100_000);
    public static final SiTuning.IntValue MINER_OV_SCANNER_MIN = intValue("miner", "ov_scanner_energy_min", 165, 0, 100_000);
    public static final SiTuning.IntValue MINER_OV_SCANNER_MAX = intValue("miner", "ov_scanner_energy_max", 190, 0, 100_000);
    // The miner scan radius stays a constant: the client draws the work area with it, and the
    // client does not get tuning values.

    // ---------------------------------------------------------------- Teleporter (machine)
    public static final SiTuning.IntValue TELEPORTER_COOLDOWN = intValue("teleporter", "cooldown_ticks", 20, 0, 1200);

    // ---------------------------------------------------------------- Armory Remote
    public static final SiTuning.IntValue ARMORY_PLAYER_RANGE = intValue("armory", "showcase_player_range", 24, 1, 64);
    public static final SiTuning.IntValue ARMORY_ARMORY_RANGE = intValue("armory", "showcase_armory_range", 16, 1, 64);
}
