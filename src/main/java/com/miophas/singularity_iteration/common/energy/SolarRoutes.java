// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.101 (solar overhaul S1): the solar profiles of the two routes.
 *
 * <p><b>Route A, concentrator photovoltaic-thermal</b> (advanced, hybrid, ultimate hybrid, quantum solar panel). Fresnel
 * lenses focus the direct beam on small multi-junction cells: the strongest daytime output, but only the direct beam
 * counts (a covered panel makes nothing), clouds take most of it (the clear-sky index cubed: rain about 37 %, a
 * thunderstorm about 5 %), full output needs 850 W/m², and nothing at night. Focused light heats the cells (45 K at
 * 800 W/m², three times a flat panel's NOCT rise), and the heat costs output after clipping, so it matters at noon too.
 * The hybrid panels cool the cells with a liquid loop that carries the heat away: the hybrid keeps half the rise, the
 * ultimate hybrid a quarter, and the quantum solar panel holds its cells at 25 °C.
 */
public final class SolarRoutes {
    private SolarRoutes() {}

    /** Cell temperature rise of an uncooled concentrator cell at 800 W/m² (K). */
    public static final double CONCENTRATOR_RISE = 45;

    /**
     * A concentrator profile.
     *
     * @param heatKept share of the rise the cooling leaves (1 none, 0 the cell is held at 25 °C)
     * @param packets  packets of its tier a tick
     */
    public static SolarProfile concentrator(double heatKept, int packets) {
        return new SolarProfile(850, 0, true, 3, CONCENTRATOR_RISE * heatKept, heatKept <= 0, 0.004, 0.6, 1.05, true, 1, 1, packets);
    }

    public static final SolarProfile ADVANCED = concentrator(1, 1);
    public static final SolarProfile HYBRID = concentrator(0.5, 1);
    /** 768 EU/t: two HV packets a tick (the voltage stays HV, so the copper cables of old saves hold). */
    public static final SolarProfile ULTIMATE_HYBRID = concentrator(0.25, 2);
    public static final SolarProfile QUANTUM = concentrator(0, 1);

    /** Share of a concentrator's heat its cooling removes (0..1); 0 for a panel that is not a concentrator. */
    public static double cooling(SolarProfile p) {
        if (!p.directOnly()) return 0;
        if (p.fixedCell()) return 1;
        return Math.max(0, Math.min(1, 1 - p.cellRise() / CONCENTRATOR_RISE));
    }
}
