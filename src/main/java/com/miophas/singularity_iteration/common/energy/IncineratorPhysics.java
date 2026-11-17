// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.19: the furnace of the item generators (incinerators). The chamber heats toward {@link #FLAME} °C while it
 * burns (time constant {@link #HEAT_TAU} ticks) and cools toward the air ({@link #COOL_TAU}) when it does not; an item
 * that does not burn takes {@link #QUENCH} K out of it. The share of the fuel's energy turned into EU follows the
 * chamber: nothing below {@link #IGNITION} °C, rising in a straight line to all of it at {@link #DESIGN} °C. Burning
 * items leave ash: one per item, two per item that does not burn.
 */
public final class IncineratorPhysics {
    private IncineratorPhysics() {}

    public static final double AMBIENT = 20, FLAME = 1000, IGNITION = 300, DESIGN = 850, QUENCH = 25;
    public static final double HEAT_TAU = 200, COOL_TAU = 400;
    public static final int ASH_CAPACITY = 64, ASH_BURNT = 1, ASH_INERT = 2;

    public static double efficiency(double celsius) {
        if (celsius <= IGNITION) return 0;
        return Math.min(1, (celsius - IGNITION) / (DESIGN - IGNITION));
    }

    public static double step(double celsius, boolean burning) {
        return burning ? celsius + (FLAME - celsius) / HEAT_TAU : celsius + (AMBIENT - celsius) / COOL_TAU;
    }

    public static double quench(double celsius) {
        return Math.max(AMBIENT, celsius - QUENCH);
    }
}
