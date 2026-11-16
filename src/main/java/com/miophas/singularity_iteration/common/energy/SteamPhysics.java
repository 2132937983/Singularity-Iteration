// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.16: the steam of the steam chain, shared by the boiler, the superheater, the turbines and the screens.
 *
 * <p>Water and steam properties are compact fits of the steam tables (within a few percent over 0.05..120 bar), in kJ/kg,
 * kJ/(kg K), °C and bar:
 * <ul>
 *   <li>saturation temperature {@code 100 p^0.25} (180 °C at 10 bar, 251 °C at 40 bar);</li>
 *   <li>saturated liquid {@code h = 4.19 T}, {@code s = 4.19 ln(T/273.15 K)};</li>
 *   <li>evaporation {@code h_fg = 2501 - 2.4 T}, {@code s_fg = h_fg / T};</li>
 *   <li>superheated steam with {@code cp = 2.1}.</li>
 * </ul>
 *
 * <p>The two steam fluids of the mod are two states: <b>steam</b> is saturated at {@link #STEAM_BAR} bar, <b>superheated
 * steam</b> is at {@link #SUPER_BAR} bar and {@link #SUPER_HEAT} K of superheat. A turbine expands its steam to the back
 * pressure: the atmosphere ({@link #ATMOSPHERE} bar) when the exhaust is vented, {@link #VACUUM} bar when a working
 * condenser beside it takes the exhaust. The work is the enthalpy drop times the turbine's isentropic efficiency, less
 * the Baumann loss of wet steam (1 % of efficiency per 1 % of moisture on the way). The screens show exhaust wetter
 * than 12 % (quality below {@link #EROSION_QUALITY}) in red, the wetness at which real blades erode.
 *
 * <p>EU per kJ ({@link #EU_PER_KJ}) is set so that steam through the steam turbine into the open air gives the 0.5 EU/mB it
 * always gave: a condenser and superheated steam give more from there.
 */
public final class SteamPhysics {
    private SteamPhysics() {}

    public static final double STEAM_BAR = 10, SUPER_BAR = 40, SUPER_HEAT = 150;
    public static final double ATMOSPHERE = 1.0, VACUUM = 0.1;
    public static final double EROSION_QUALITY = 0.88;
    public static final double CP_STEAM = 2.1;
    /** Isentropic efficiency of the steam turbine (EU block); the tunnel and the kinetic turbine set their own. */
    public static final double TURBINE_EFFICIENCY = 0.75;

    public static double tsat(double bar) { return 100 * Math.pow(Math.max(0.01, bar), 0.25); }
    public static double hf(double t) { return 4.19 * t; }
    public static double sf(double t) { return 4.19 * Math.log((t + 273.15) / 273.15); }
    public static double hfg(double t) { return Math.max(0, 2501 - 2.4 * t); }
    public static double sfg(double t) { return hfg(t) / (t + 273.15); }

    /** A steam state: pressure (bar), temperature (°C), enthalpy (kJ/kg), entropy (kJ/(kg K)), quality (1 = dry). */
    public record State(double bar, double temperature, double enthalpy, double entropy, double quality) {}

    /** Saturated (quality 1) or superheated steam at a pressure. */
    public static State steam(double bar, double superheat) {
        double t = tsat(bar);
        double h = hf(t) + hfg(t) + CP_STEAM * superheat;
        double s = sf(t) + sfg(t) + CP_STEAM * Math.log((t + superheat + 273.15) / (t + 273.15));
        return new State(bar, t + superheat, h, s, 1);
    }

    public static final State STEAM = steam(STEAM_BAR, 0), SUPERHEATED = steam(SUPER_BAR, SUPER_HEAT);

    /** The state after an expansion to {@code backBar} with isentropic efficiency {@code eta}. */
    public static State expand(State in, double backBar, double eta) {
        double t = tsat(backBar);
        double xs = (in.entropy() - sf(t)) / sfg(t);
        double hs;
        if (xs < 1) hs = hf(t) + xs * hfg(t);
        else {
            // still superheated at the back pressure: s = s_g + cp ln(T / T_sat)
            double ts = t + 273.15, tk = ts * Math.exp((in.entropy() - sf(t) - sfg(t)) / CP_STEAM);
            hs = hf(t) + hfg(t) + CP_STEAM * (tk - ts);
        }
        double h = in.enthalpy() - eta * (in.enthalpy() - hs);
        double x = Math.min(1, (h - hf(t)) / hfg(t));
        double s = sf(t) + x * sfg(t);
        return new State(backBar, t, h, s, x);
    }

    /**
     * Work per kg (kJ) of an expansion with the Baumann loss: the efficiency falls by the mean moisture of the expansion
     * (half the exhaust moisture when it starts dry).
     */
    public static double work(State in, double backBar, double eta) {
        State first = expand(in, backBar, eta);
        double moisture = Math.max(0, 1 - first.quality()) / 2;
        State out = expand(in, backBar, eta * (1 - moisture));
        return in.enthalpy() - out.enthalpy();
    }

    /** Exhaust quality of an expansion (with the Baumann loss). */
    public static double exhaustQuality(State in, double backBar, double eta) {
        State first = expand(in, backBar, eta);
        double moisture = Math.max(0, 1 - first.quality()) / 2;
        return expand(in, backBar, eta * (1 - moisture)).quality();
    }

    /** EU per kJ: steam through the steam turbine to the open air gives 0.5 EU/mB (1 mB = 1 kg here). */
    public static final double EU_PER_KJ = 0.5 / work(STEAM, ATMOSPHERE, TURBINE_EFFICIENCY);

    /** EU per mB of a steam state expanded to a back pressure with an efficiency. */
    public static double euPerMb(State in, double backBar, double eta) {
        return work(in, backBar, eta) * EU_PER_KJ;
    }
}
