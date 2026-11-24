// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * Pure numbers for the Quantum Suit's high-speed geometric Phase Energy Wings and Air-Brake palm reverse-thrust rings.
 *
 * <p>Hidden during hover and low-speed cruise ({@code throttle <= UNFURL_START}), the energy wings smoothly unfold from
 * the open dorsal thruster pods as throttle climbs to full burn, sweeping further back with Mach speed and surging in
 * luminance past Mach 1.0.
 */
public final class PhaseWingRules {
    private PhaseWingRules() {}

    /** Throttle at which the dorsal Phase Energy Wings begin to unfold, and where they reach full span. */
    public static final float UNFURL_START = 0.55F, UNFURL_FULL = 0.95F;

    /** Minimum brake intensity required before the palm reverse-thrust rings ignite. */
    public static final float BRAKE_RING_START = 0.22F;

    /** Smooth unfurl factor (0..1) from throttle. */
    public static float unfurl(float throttle) {
        float k = clamp((throttle - UNFURL_START) / (UNFURL_FULL - UNFURL_START));
        return k * k * (3 - 2 * k);
    }

    /** Sweep-back angle of the wings in degrees: 18 deg when unfolding, sweeping to 44 deg at Mach 1.0 and 56 deg at Mach 1.5+. */
    public static float sweepDeg(double mach, float unfurl) {
        float m = clamp((float) ((mach - 0.5) / 1.0));
        return 18.0F + 14.0F * clamp(unfurl) + 24.0F * m;
    }

    /** Wing span scale multiplier: expands smoothly with unfurl and stretches slightly past Mach 1.0. */
    public static float spanScale(double mach, float unfurl) {
        float superSurge = clamp((float) ((mach - 0.95) / 0.45));
        return (0.45F + 0.55F * clamp(unfurl)) * (1.0F + 0.22F * superSurge);
    }

    /** Translucent wing opacity (0..0.90), modulated by subtle harmonic shimmer {@code pulse} (0..1) and supersonic surge. */
    public static float alpha(float unfurl, double mach, float pulse) {
        float u = clamp(unfurl);
        if (u <= 0.001F) return 0F;
        float superSurge = clamp((float) ((mach - 0.95) / 0.35));
        float base = (0.38F + 0.28F * u + 0.20F * superSurge) * u;
        return clamp(base * (0.86F + 0.14F * clamp(pulse)));
    }

    /** Air-Brake palm reverse-thrust ring opacity (0..0.85) from smoothed brake intensity. */
    public static float brakeRingAlpha(float brake) {
        float k = clamp((brake - BRAKE_RING_START) / (1.0F - BRAKE_RING_START));
        return k * k * 0.85F;
    }

    private static float clamp(float x) { return Math.max(0F, Math.min(1F, x)); }
}
