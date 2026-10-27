// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

/**
 * 0.1.10.30: the light of the suit glow (pure arithmetic, no game types; the offline harness runs it).
 *
 * <p>The glow layer draws the glow mask of each worn piece full bright and additively. Its colour is the suit's light colour
 * (the palette accent, or the default of the suit) while the piece holds at least half its charge, turns towards amber down to
 * a quarter and towards red below; an empty piece is dark. Its strength:
 * <ul>
 *   <li><b>breathing</b>: 0.8 to 1.0 over {@link #BREATH_SECONDS};</li>
 *   <li><b>low charge</b> (below a quarter): a double heartbeat every {@link #HEARTBEAT_SECONDS};</li>
 *   <li><b>hit</b>: flickers for the hurt time of the wearer;</li>
 *   <li><b>boost</b>: up to 35 % brighter with the thrust of the wearer;</li>
 *   <li><b>energy band</b>: a bright band runs down the body (model pixels, -10 above the head to 26 below the feet) every
 *       {@link #BAND_SECONDS};</li>
 *   <li><b>power-on</b>: when a charged piece comes on (put on, suit-up, charged from empty), the light spreads from the chest
 *       core at {@link #SWEEP_SPEED} model pixels a second, with a bright front.</li>
 * </ul>
 */
public final class SuitGlowRules {
    private SuitGlowRules() {}

    public static final float BREATH_SECONDS = 3.2F, HEARTBEAT_SECONDS = 1.1F, BAND_SECONDS = 3.6F;
    public static final float BAND_TOP = -10F, BAND_BOTTOM = 26F, BAND_WIDTH = 2.6F, BAND_GAIN = 0.6F;
    public static final float SWEEP_SPEED = 40F, SWEEP_FRONT = 5F;
    /** Distance (model pixels) the power-on sweep covers: past the boots seen from the chest core. */
    public static final float SWEEP_REACH = 26F;
    public static final int AMBER = 0xFFA838, RED = 0xFF3B30;
    /** Default light colours (the light pixels of the suit textures, brightened for additive light). */
    public static final int QUANTUM = 0x3CB4FF, NANO = 0x46E070;

    /** RGB of the light at {@code charge} (0..1) from the suit's light colour {@code base}. */
    public static int colour(int base, float charge) {
        if (charge >= 0.5F) return base & 0xFFFFFF;
        if (charge >= 0.25F) return mix(base, AMBER, (0.5F - charge) / 0.25F);
        return mix(AMBER, RED, clamp((0.25F - charge) / 0.25F));
    }

    /**
     * Strength of the light of a piece, 0..1.35, before the band and the sweep: 0 when empty; breathing; a heartbeat below a
     * quarter; a flicker while {@code hurt} (the remaining hurt ticks, 0 when not hurt); {@code boost} 0..1 brightens.
     */
    public static float strength(float charge, float seconds, int hurt, float boost, int seed) {
        if (!(charge > 0)) return 0F;
        float breath = 0.8F + 0.2F * (0.5F + 0.5F * (float) Math.sin((seconds + seed * 0.37F) * Math.PI * 2 / BREATH_SECONDS));
        float s = breath;
        if (charge < 0.25F) s *= 0.35F + 0.65F * heartbeat(seconds + seed * 0.13F);
        if (hurt > 0) s *= ((hurt + seed) % 3 == 0) ? 0.25F : 1.3F;
        return Math.min(1.35F, s * (1F + 0.35F * clamp(boost)));
    }

    /** Double heartbeat 0..1 (two short beats, then rest), period {@link #HEARTBEAT_SECONDS}. */
    public static float heartbeat(float seconds) {
        float t = (float) (seconds / HEARTBEAT_SECONDS - Math.floor(seconds / HEARTBEAT_SECONDS));
        return Math.max(pulse(t, 0.08F), 0.7F * pulse(t, 0.3F));
    }

    private static float pulse(float t, float at) {
        float d = (t - at) / 0.07F;
        return (float) Math.exp(-d * d);
    }

    /** Extra light of the energy band at model height {@code y} (pixels, y down), 0..{@link #BAND_GAIN}. */
    public static float band(float y, float seconds) {
        float k = (float) (seconds / BAND_SECONDS - Math.floor(seconds / BAND_SECONDS));
        float centre = BAND_TOP + (BAND_BOTTOM - BAND_TOP) * k;
        float d = (y - centre) / BAND_WIDTH;
        return BAND_GAIN * (float) Math.exp(-d * d);
    }

    /**
     * Power-on factor at {@code distance} model pixels from the chest core, {@code since} seconds after the piece came on:
     * 0 ahead of the front, 1 + a bright front just behind it, 1 when the sweep has passed (and for since below 0: no sweep).
     */
    public static float powerOn(float distance, float since) {
        if (since < 0) return 1F;
        float front = since * SWEEP_SPEED;
        if (front > SWEEP_REACH + SWEEP_FRONT * 2) return 1F;
        float behind = front - distance;
        if (behind < 0) return 0F;
        return 1F + 1.2F * (float) Math.exp(-(behind / SWEEP_FRONT) * (behind / SWEEP_FRONT));
    }

    /** Seconds the power-on sweep takes to pass the whole suit. */
    public static float sweepSeconds() {
        return (SWEEP_REACH + SWEEP_FRONT * 2) / SWEEP_SPEED;
    }

    /** Base light of a vertex at strength 1 without band: the band and the power-on front brighten it up to the colour's full value. */
    public static final float VERTEX_BASE = 0.72F;

    /** Light of one vertex (0..; scaled into the colour by {@link #scale}): strength, band and power-on together. */
    public static float vertexLight(float strength, float band, float powerOn) {
        return VERTEX_BASE * strength * (1F + band) * powerOn;
    }

    /** One vertex: the RGB (0..255 each) of {@code colour} times {@code light} (clamped to 1). */
    public static int scale(int colour, float light) {
        float l = Math.max(0F, Math.min(1F, light));
        int r = Math.round(((colour >> 16) & 255) * l), g = Math.round(((colour >> 8) & 255) * l), b = Math.round((colour & 255) * l);
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float t) {
        float k = clamp(t);
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
