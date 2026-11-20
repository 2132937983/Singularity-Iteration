// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.100 (solar overhaul S0): how one solar panel turns the sky into output. {@link SolarPhysics} gives the sun and the
 * weather; a profile says how the panel uses them, so the two routes (concentrator and photon resonance) can differ in
 * light, weather, temperature and night without separate code. {@link #STANDARD} is the model every panel used before.
 *
 * @param clip           irradiance (W/m²) at which the panel reaches its rated output
 * @param diffuse        share of the sky light a covered panel still gets (at full sky light); ignored if {@code directOnly}
 * @param directOnly     the panel uses the direct beam only: a covered panel gets nothing
 * @param cloudExponent  the weather factor is the clear-sky index to this power (1 = the index itself)
 * @param cellRise       cell temperature rise (K) at 800 W/m²
 * @param fixedCell      the cell is held at 25 °C (no temperature effect)
 * @param gamma          output change per kelvin away from 25 °C
 * @param derateMin      lowest temperature factor
 * @param derateMax      highest temperature factor
 * @param derateAfterClip the temperature factor applies after clipping (heat costs output even in full sun)
 * @param nightRain      night output factor in full rain (1 = rain does not matter)
 * @param nightThunder   night output factor in a full thunderstorm
 * @param packets        packets the panel may send each tick (each at most its tier's packet size)
 */
public record SolarProfile(double clip, double diffuse, boolean directOnly, double cloudExponent,
                           double cellRise, boolean fixedCell, double gamma, double derateMin, double derateMax,
                           boolean derateAfterClip, double nightRain, double nightThunder, int packets) {

    /** The model of every panel up to 0.1.11.99: {@link SolarPhysics}' constants, night output whatever the weather. */
    public static final SolarProfile STANDARD = new SolarProfile(SolarPhysics.CLIP, SolarPhysics.DIFFUSE, false, 1,
        SolarPhysics.NOCT_RISE, false, SolarPhysics.GAMMA, 0.75, 1.1, false, 1, 1, 1);

    public SolarProfile {
        if (!(clip > 0) || diffuse < 0 || cloudExponent < 0 || gamma < 0 || derateMin > derateMax || packets < 1)
            throw new IllegalArgumentException("bad solar profile");
    }

    /** Share of the sky that reaches a panel: 1 open, the diffuse share times the sky light (0..15) when covered. */
    public double shade(boolean open, int skyLight) {
        if (open) return 1;
        return directOnly || skyLight <= 0 ? 0 : diffuse * skyLight / 15.0;
    }

    /** Weather factor from the clear-sky index (0..1). */
    public double weather(double clearSkyIndex) {
        return cloudExponent == 1 ? clearSkyIndex : Math.pow(Math.max(0, clearSkyIndex), cloudExponent);
    }

    public double cellTemperature(double ambient, double irradiance) {
        return fixedCell ? 25 : ambient + irradiance / 800.0 * cellRise;
    }

    public double derate(double cellTemperature) {
        return Math.max(derateMin, Math.min(derateMax, 1 - gamma * (cellTemperature - 25)));
    }

    /** Share of the rated output (0..1). */
    public double output(double irradiance, double derate) {
        if (derateAfterClip) return Math.max(0, Math.min(1, Math.min(1, irradiance / clip) * derate));
        return Math.max(0, Math.min(1, irradiance / clip * derate));
    }

    /** Night output factor from the rain and thunder levels (0..1 each). */
    public double night(double rain, double thunder) {
        double r = 1 + (nightRain - 1) * Math.max(0, Math.min(1, rain));
        double t = nightRain > 0 ? 1 + (nightThunder / nightRain - 1) * Math.max(0, Math.min(1, thunder)) : 1;
        return Math.max(0, r * t);
    }
}
