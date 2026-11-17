// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * 0.1.11.70 (phase warp style phase W0): the shapes and colours of the phase warp flight effects, pure numbers for the
 * client to draw. A nozzle burns a cone of light: a white core going through the quantum cyan to violet and out, with
 * rings of compressed space sliding from the nozzle to the tip; an attitude jet blinks (a point of light and a ring that
 * grows and fades in one pulse); by day a soft dark bed lies under the light so it reads on a bright sky.
 *
 * <p>All lengths are blocks, times seconds, colours {r, g, b, a} 0..1.
 */
public final class PhaseRules {
    private PhaseRules() {}

    /** The glow colours of the quantum suit: cyan, violet, white. */
    public static final float[] CYAN = {0.37F, 0.90F, 1.00F}, VIOLET = {0.65F, 0.55F, 1.00F}, WHITE = {1F, 1F, 1F};

    /** Rings of compressed space in one cone. */
    public static final int RINGS = 4;

    /**
     * The radius of a cone at {@code f} of its length (0 the nozzle, 1 the tip), as a fraction of its half-width: full at
     * the nozzle, a slight bulge, then closing to a point.
     */
    public static float radius(float f) {
        float x = clamp(f);
        return (float) (Math.pow(1 - x, 0.7) * (1 + 0.35 * x * (1 - x)));
    }

    /**
     * The colour of a cone at {@code f} of its length, {r, g, b, a}: white at the nozzle, cyan by a fifth, then towards
     * violet (all the way when {@code hot} is 1), fading out at the tip.
     */
    public static float[] colour(float f, float hot) {
        float x = clamp(f), h = clamp(hot);
        float[] mid = mix(CYAN, VIOLET, 0.35F + 0.65F * h);
        if (x < 0.18F) return with(mix(WHITE, CYAN, x / 0.18F), 0.9F - 0.2F * (x / 0.18F));
        if (x < 0.6F) { float k = (x - 0.18F) / 0.42F; return with(mix(CYAN, mid, k), 0.7F - 0.35F * k); }
        if (x >= 1F) return with(VIOLET, 0F);
        float k = Math.min(1F, (x - 0.6F) / 0.4F);
        return with(mix(mid, VIOLET, k), 0.35F * (1 - k));
    }

    /** The place of ring {@code k} of {@link #RINGS} along its cone (0..1) at {@code seconds}, sliding at {@code rate} per second. */
    public static float ring(float seconds, float rate, int k) {
        double v = fin(seconds) * fin(rate) + (double) k / RINGS;
        return (float) (v - Math.floor(v));
    }

    /** A ring at {@code phase} of its way: its radius (fraction of the cone half-width) and its alpha. */
    public static float[] ringShape(float phase) {
        float p = clamp(phase);
        return new float[]{1 - 0.55F * p, 0.9F * (1 - p)};
    }

    /** The length of a cone for an output {@code p}, the full-burn share {@code burn} and the nozzle size. */
    public static float length(float p, float burn, float size) {
        float o = clamp(p);
        return (0.18F + 0.5F * o + 0.6F * clamp(burn) * o) * (0.6F + 0.4F * Math.max(0F, fin(size)));
    }

    /** The width of a cone for an output {@code p} and the nozzle size. */
    public static float width(float p, float size) {
        return (0.12F + 0.06F * clamp(p)) * Math.max(0F, fin(size));
    }

    /** How fast the rings slide (per second) at a throttle: faster with the throttle. */
    public static float rate(float throttle) { return 0.8F + 1.6F * clamp(throttle); }

    /** One blink of an attitude jet at {@code frac} of its pulse (0..1): {ring radius in blocks for size 1, alpha}. */
    public static float[] blink(float frac) {
        float f = clamp(frac);
        return new float[]{0.05F + 0.14F * f, 1 - f};
    }

    /**
     * The alpha of the dark bed under the light by day, from the sky brightness (vanilla sky darken factor, about 1 at noon
     * and 0.2 at night): none at night, up to 0.3 at noon.
     */
    public static float underlay(float skyBrightness) {
        return 0.3F * clamp((fin(skyBrightness) - 0.35F) / 0.55F);
    }

    public static float[] mix(float[] a, float[] b, float k) {
        float t = clamp(k);
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static float[] with(float[] rgb, float a) { return new float[]{rgb[0], rgb[1], rgb[2], Math.max(0F, a)}; }

    private static float clamp(float v) { return Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }

    private static float fin(float v) { return Float.isFinite(v) ? v : 0F; }
}
