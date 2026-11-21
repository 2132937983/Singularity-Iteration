// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * 0.1.11.126 (armour and flight overhaul E3): the electromagnetic hover of the quantum suit, pure numbers for the client to
 * draw. The suit holds itself up on a magnetic field, so nothing is thrown downwards; the field only shows, faintly:
 * <ul>
 *   <li><b>Sole rings</b>: under each quantum boot a thin ring of light lies flat across the sole and breathes, and every
 *       {@link #PERIOD} seconds (each stroke of the 3.2 s float of the head ring and the back wing) a ripple runs out of it
 *       and fades.</li>
 *   <li><b>Ground ripple</b>: when the ground is within {@link #GROUND_RANGE} blocks below, the same beat sends a wide thin
 *       ring over it, fainter the higher the flyer.</li>
 *   <li><b>Glow breath</b>: the glow of the head ring and the back wing swells and ebbs with their float.</li>
 * </ul>
 * All of it fades in and out with the hover itself (the weight is the hover clip's boot-strip glow, which the flight state
 * machine crossfades). Lengths are blocks, times seconds, alphas 0..1; the colour is the quantum cyan.
 */
public final class HoverFieldRules {
    private HoverFieldRules() {}

    /** Seconds between two ripples: half the hover clip's 3.2 s float, one per stroke. */
    public static final float PERIOD = 1.6F;
    /** The float of the head ring and the back wing, seconds (the hover clip's length). */
    public static final float FLOAT_PERIOD = 3.2F;

    /** The sole ring: radius, line width and alpha at its brightest. */
    public static final float SOLE_RADIUS = 0.18F, SOLE_WIDTH = 0.022F, SOLE_ALPHA = 0.55F;
    /** A sole ripple runs from the ring out to this radius. */
    public static final float SOLE_RIPPLE_RADIUS = 0.5F;
    /** The ground ripple: from, to radius, line width, alpha at the ground, and how far down it reaches. */
    public static final float GROUND_FROM = 0.4F, GROUND_TO = 1.8F, GROUND_WIDTH = 0.05F, GROUND_ALPHA = 0.5F, GROUND_RANGE = 6F;
    /** The glow of the head ring and the wing breathes between this share and full. */
    public static final float GLOW_LOW = 0.7F;

    /** Where the current ripple is at time {@code t}, 0 (just out) to 1 (gone). */
    public static float phase(double t) {
        double p = (t % PERIOD) / PERIOD;
        return (float) (p < 0 ? p + 1 : p);
    }

    /** The breathing of the sole ring at {@code t}: 0.7 to 1, brightest as a ripple leaves. */
    public static float soleBreath(double t) {
        return 0.7F + 0.3F * (1 - phase(t));
    }

    public static float soleRippleRadius(float phase) { return SOLE_RADIUS + (SOLE_RIPPLE_RADIUS - SOLE_RADIUS) * ease(phase); }

    public static float soleRippleAlpha(float phase) { return 0.9F * SOLE_ALPHA * (1 - phase) * (1 - phase); }

    public static float groundRadius(float phase) { return GROUND_FROM + (GROUND_TO - GROUND_FROM) * ease(phase); }

    /** The ground ripple's alpha at {@code phase} with the ground {@code height} blocks below the feet (0 out of range). */
    public static float groundAlpha(float phase, double height) {
        if (height < 0 || height > GROUND_RANGE) return 0F;
        float fall = (float) (1 - height / GROUND_RANGE);
        return GROUND_ALPHA * (float) Math.pow(1 - phase, 1.5) * fall;
    }

    /** The share of the head ring's and wing's glow at {@code t} in a full hover ({@code weight} 1); 1 out of the hover. */
    public static float glowBreath(double t, float weight) {
        float wave = 0.5F + 0.5F * (float) Math.cos(2 * Math.PI * t / FLOAT_PERIOD);
        float share = GLOW_LOW + (1 - GLOW_LOW) * wave;
        return 1 - Math.max(0, Math.min(1, weight)) * (1 - share);
    }

    private static float ease(float x) {
        float k = Math.max(0, Math.min(1, x));
        return 1 - (1 - k) * (1 - k) * (1 - k);
    }
}
