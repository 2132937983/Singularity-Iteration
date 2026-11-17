// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.14: the irradiance model of every solar panel, shared by the panels, the screen and the tests.
 *
 * <ul>
 *   <li><b>Sun.</b> The Minecraft sun rises in the east at time 0, crosses the zenith at 6000 and sets in the west at
 *       12000, so its elevation rises and falls linearly: {@code 90 - |90 - 360 t / 24000|} degrees, below 0 at night.</li>
 *   <li><b>Clear sky.</b> {@code 1361 sin(e) 0.7^(AM^0.678)} W/m², AM = 1 / sin(e) the air mass (Meinel): weak at dawn
 *       and dusk, about 950 W/m² with the sun overhead.</li>
 *   <li><b>Clouds.</b> Cloud cover from the weather (rain 0.75, a thunderstorm 0.95) and the clear-sky index
 *       {@code 1 - 0.75 cover^3.4} (Kasten and Czeplak): rain keeps about 70 %, a thunderstorm about 37 %.</li>
 *   <li><b>Shade.</b> A panel that sees the sky straight up gets it all; one under cover but with sky light around gets
 *       the diffuse part only (15 % at full sky light); one in the dark gets nothing.</li>
 *   <li><b>Cell temperature.</b> Ambient from the biome's temperature, plus 20 K at 800 W/m² (the NOCT rule); the output
 *       falls 0.4 % per kelvin above 25 °C (and rises a little below it).</li>
 *   <li><b>Inverter clipping.</b> The panel reaches its rated output at 700 W/m² and stays there above it, as power
 *       stations size their inverters below the array's peak.</li>
 * </ul>
 */
public final class SolarPhysics {
    private SolarPhysics() {}

    public static final double CLIP = 700.0, DIFFUSE = 0.15, NOCT_RISE = 20.0, GAMMA = 0.004;

    /** Sun elevation in degrees for a time of day (0..23999); negative at night. */
    public static double elevation(long dayTime) {
        double t = Math.floorMod(dayTime, 24000L);
        if (t >= 12000) return -90 + Math.abs(270 - 360 * t / 24000.0);
        return 90 - Math.abs(90 - 360 * t / 24000.0);
    }

    /** Clear-sky irradiance on the panel (W/m²) at an elevation in degrees. */
    public static double clearSky(double elevation) {
        if (elevation <= 0) return 0;
        double s = Math.sin(Math.toRadians(elevation)), am = 1 / Math.max(0.05, s);
        return 1361 * s * Math.pow(0.7, Math.pow(am, 0.678));
    }

    /** Clear-sky index (0..1) from the rain and thunder levels (0..1 each). */
    public static double cloudIndex(double rain, double thunder) {
        double cover = Math.max(0, Math.min(1, 0.75 * rain + 0.2 * thunder));
        return 1 - 0.75 * Math.pow(cover, 3.4);
    }

    /** Ambient air temperature in °C from a biome temperature (plains 0.8, desert 2.0, snowy plains 0). */
    public static double ambient(float biomeTemperature, boolean raining) {
        return 2 + 20 * biomeTemperature - (raining ? 4 : 0);
    }

    public static double cellTemperature(double ambient, double irradiance) {
        return ambient + irradiance / 800.0 * NOCT_RISE;
    }

    /** Output factor of the cell temperature (0.75..1.1). */
    public static double derate(double cellTemperature) {
        return Math.max(0.75, Math.min(1.1, 1 - GAMMA * (cellTemperature - 25)));
    }

    /** Share of the rated output (0..1): irradiance over the clipping level, times the temperature factor. */
    public static double output(double irradiance, double derate) {
        return Math.max(0, Math.min(1, irradiance / CLIP * derate));
    }
}
