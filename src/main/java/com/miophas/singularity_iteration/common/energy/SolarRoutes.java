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
 *
 * <p><b>Route B, photon resonance</b> (advanced METS, photon resonance, ultimate photon resonance; 0.1.11.102). A surface of
 * resonant nano-antennas absorbs a wide spectrum, diffuse light and infrared included: a covered panel keeps 30 / 45 / 60 %
 * of the sky light, clouds cost little (the square root of the clear-sky index: rain about 85 %, a thunderstorm about
 * 61 %), and it reaches full output in weak light (600 / 450 / 350 W/m²). The cells hardly mind heat (0.2 % per kelvin,
 * at least 85 %). At night it runs as a thermoradiative cell, radiating the panel's heat to the cold clear sky through the
 * atmosphere's infrared window and taking power from that flow: 15 / 25 / 35 % of the rated output under open sky. Clouds
 * close the window (rain halves it, a thunderstorm leaves a quarter). Its daytime rating is lower than route A's.
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

    /** Night output of a photon resonance panel as a share of its rated output, by tier (0.1.11.102). */
    public static final float METS_NIGHT = 0.15F, PHOTON_NIGHT = 0.25F, ULTIMATE_PHOTON_NIGHT = 0.35F;

    /**
     * A photon resonance profile.
     *
     * @param clip    irradiance (W/m²) for the rated output
     * @param diffuse share of the sky light a covered panel keeps
     */
    public static SolarProfile photonResonance(double clip, double diffuse) {
        return new SolarProfile(clip, diffuse, false, 0.5, 20, false, 0.002, 0.85, 1.05, false, 0.5, 0.25, 1);
    }

    public static final SolarProfile METS = photonResonance(600, 0.30);
    public static final SolarProfile PHOTON = photonResonance(450, 0.45);
    public static final SolarProfile ULTIMATE_PHOTON = photonResonance(350, 0.60);

    /** True for the photon resonance profiles (route B). */
    public static boolean photonResonance(SolarProfile p) {
        return p.equals(METS) || p.equals(PHOTON) || p.equals(ULTIMATE_PHOTON);
    }

    /** Share of a concentrator's heat its cooling removes (0..1); 0 for a panel that is not a concentrator. */
    public static double cooling(SolarProfile p) {
        if (!p.directOnly()) return 0;
        if (p.fixedCell()) return 1;
        return Math.max(0, Math.min(1, 1 - p.cellRise() / CONCENTRATOR_RISE));
    }
}
