// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

/**
 * 0.1.11.110 (processing overhaul M3): why a processing machine runs or stands, for the instrument screens (synced as a
 * number through the menu data; the screen shows a line per code).
 */
public final class ProcessStatus {
    private ProcessStatus() {}

    public static final int RUNNING = 0, NO_INPUT = 1, NO_RECIPE = 2, OUTPUT_FULL = 3, NO_POWER = 4, REDSTONE_OFF = 5;
    /** 0.1.11.112: the advanced compressor's accumulator is building up the pressure of the next stroke. */
    public static final int CHARGING = 6;
    /** 0.1.11.113: a furnace is heating up to the temperature the item needs. */
    public static final int HEATING = 7;
    /** Language key suffixes, by code. */
    public static final String[] KEYS = {"running", "no_input", "no_recipe", "output_full", "no_power", "redstone_off", "charging", "heating"};

    /** True for a code the screen shows as a stop (red), not a wait. */
    public static boolean isFault(int code) {
        return code == OUTPUT_FULL || code == NO_POWER || code == REDSTONE_OFF;
    }
}
