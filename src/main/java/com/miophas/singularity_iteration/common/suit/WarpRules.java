// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * 0.1.11.72 (phase warp style phase W2, decision three: the full warp at the sound barrier): the timeline of a phase warp.
 *
 * <ol>
 *   <li><b>Gather</b> (anticipation): from {@link #GATHER_FROM} of the barrier speed on the way up, light is drawn in: a
 *       ring closes on the flyer, the star streams are pulled in and shortened, a violet glow grows; full after
 *       {@link #GATHER_FULL} ticks. It is called off below {@link #GATHER_CANCEL} or after {@link #GATHER_MAX} ticks
 *       without a crossing.</li>
 *   <li><b>Flash</b> ({@link #FLASH} ticks from the crossing): a white burst, a gate of light the flyer goes through, a
 *       thin line of light across.</li>
 *   <li><b>Warp</b> ({@link #WARP} ticks): the streams long and hot, afterimages of the flyer behind it, rings of
 *       rippled space left behind every ten ticks.</li>
 *   <li><b>Drop</b> ({@link #DROP} ticks): everything settles back.</li>
 * </ol>
 * Times are ticks; the crossing itself is the barrier of {@link ManeuverMode#supersonic}.
 */
public final class WarpRules {
    private WarpRules() {}

    /** 0.1.11.73: from 0.65 (was 0.8: the climb from 0.8 to the barrier took only a few ticks, too short to gather). */
    public static final float GATHER_FROM = 0.65F, GATHER_CANCEL = 0.58F;
    public static final int GATHER_FULL = 12, GATHER_MAX = 60, FLASH = 2, WARP = 40, DROP = 16;

    public enum Phase { NONE, FLASH, WARP, DROP }

    /** True when the gather starts: the Mach number passes {@link #GATHER_FROM} on the way up. */
    public static boolean gatherStarts(double prevMach, double mach) {
        return Double.isFinite(prevMach) && Double.isFinite(mach) && prevMach < GATHER_FROM && mach >= GATHER_FROM;
    }

    /** True when a gather of {@code ticks} is called off at a Mach number. */
    public static boolean gatherCancels(double mach, int ticks) {
        return !Double.isFinite(mach) || mach < GATHER_CANCEL || ticks > GATHER_MAX;
    }

    /** The strength of a gather {@code ticks} old, 0..1 (eased in). */
    public static float gather(float ticks) {
        float k = clamp(ticks / GATHER_FULL);
        return 1 - (1 - k) * (1 - k);
    }

    /** The phase {@code since} ticks after the crossing. */
    public static Phase phase(float since) {
        if (!(since >= 0)) return Phase.NONE;
        if (since < FLASH) return Phase.FLASH;
        if (since < FLASH + WARP) return Phase.WARP;
        if (since < FLASH + WARP + DROP) return Phase.DROP;
        return Phase.NONE;
    }

    /** How far through its phase, 0..1, {@code since} ticks after the crossing. */
    public static float progress(float since) {
        return switch (phase(since)) {
            case FLASH -> since / FLASH;
            case WARP -> (since - FLASH) / WARP;
            case DROP -> (since - FLASH - WARP) / DROP;
            case NONE -> 0F;
        };
    }

    /** The stretch of the star streams (length and light, 1 = plain) {@code since} ticks after the crossing. */
    public static float streamBoost(float since) {
        return switch (phase(since)) {
            case FLASH, WARP -> 1.8F;
            case DROP -> 1.8F - 0.8F * progress(since);
            case NONE -> 1F;
        };
    }

    /** The light of afterimage {@code i} (1 nearest, 3 farthest) {@code since} ticks after the crossing. */
    public static float ghost(int i, float since) {
        float base = 0.7F * (1 - i / 4F);
        return switch (phase(since)) {
            case WARP -> base;
            case DROP -> base * (1 - progress(since));
            default -> 0F;
        };
    }

    /** Rings of rippled space left behind; each lives this long (ticks). */
    public static final int RIPPLES = 4, RIPPLE_LIFE = 20;

    /**
     * Ripple {@code j} {@code since} ticks after the crossing: {distance behind the flyer, radius, alpha} in blocks, or null
     * when it is not out. Ripples are thrown every ten ticks from the end of the flash.
     */
    public static float[] ripple(int j, float since) {
        float q = (since - FLASH - j * 10F) / RIPPLE_LIFE;
        if (!(q >= 0) || q >= 1) return null;
        return new float[]{2F + 6F * q, 1F + 5F * q, 1 - q};
    }

    /** The gate of light the flyer goes through: {radius, alpha} {@code since} ticks after the crossing (gone after 6). */
    public static float[] gate(float since) {
        if (!(since >= 0) || since >= 6) return new float[]{0F, 0F};
        return new float[]{1.2F + 1.6F * since, 1 - since / 6F};
    }

    /**
     * 0.1.11.73 (W3): the strength of the screen effect of the own warp (edge colour split, radial blur, dark edge), 0..1:
     * full in the flash, easing to 0.6 over the warp, out over the drop.
     */
    public static float postStrength(float since) {
        return switch (phase(since)) {
            case FLASH -> 1F;
            case WARP -> 1F - 0.4F * progress(since);
            case DROP -> 0.6F * (1 - progress(since));
            case NONE -> 0F;
        };
    }

    private static float clamp(float v) { return Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }
}
