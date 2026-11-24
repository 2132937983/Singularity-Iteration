// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.armory.WingmanMode;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.122: the light trail of the quantum suit's special flight (and of the Wingmen), pure numbers for the client to draw.
 * Each of the two back nozzles leaves one short red trail in two layers, like the wall a light cycle leaves:
 * <ul>
 *   <li><b>The line</b>: a thin, hard red line of light down the middle of the nozzle's path (additive, full-bright), with a
 *       faint white core.</li>
 *   <li><b>The curtain</b>: a sheet of red light hanging from the line towards the ground (world down, kept square to the
 *       path), {@link #CURTAIN_HEIGHT} blocks deep at the nozzle and a little shallower at the end. It is brightest at the
 *       line and fades smoothly to nothing at its lower edge; along the trail it fades from the nozzle to nothing at the
 *       end (alpha-blended, both sides, no depth write).</li>
 * </ul>
 * The trail is short: {@link #MAX_AGE_TICKS} ticks of the path (a tenth to a twentieth of the 36 ticks of the first demo), so
 * it hangs right behind the nozzles. It grows with the speed: nothing below Mach {@link #RAMP_START_MACH}, full from Mach
 * {@link #RAMP_FULL_MACH}. No trail in first person (the nozzles are behind the camera).
 *
 * <p>Lengths are blocks, colours {r, g, b} 0..1. Change the constants to tune it.
 */
public final class LightTrailRules {
    private LightTrailRules() {}

    /** How long a point of the path stays in the trail, in ticks (the trail's length is this times the speed). */
    public static final float MAX_AGE_TICKS = 3.0F;
    /** Points kept per nozzle: one per tick, and two spare so the oldest one can still be faded out. */
    public static final int MAX_POINTS = (int) Math.ceil(MAX_AGE_TICKS) + 2;
    /** Points drawn between two samples (a Catmull-Rom spline through the samples, so a hard turn draws a smooth arc). */
    public static final int SUBDIVISIONS = 4;

    /** The depth of the curtain below the line at the nozzle, in blocks. */
    public static final float CURTAIN_HEIGHT = 0.45F;
    /** How much shallower the curtain is at the end of the trail (0.3: 70 % of {@link #CURTAIN_HEIGHT}). */
    public static final float CURTAIN_TAPER = 0.3F;
    /** The curtain's alpha right under the line, at the nozzle. */
    public static final float CURTAIN_TOP_ALPHA = 0.5F;
    /** How the curtain fades downwards: alpha share {@code (1 - depth)^CURTAIN_FADE_POWER} (1: straight, higher: thinner lower down). */
    public static final float CURTAIN_FADE_POWER = 1.4F;
    /** Draw the curtain additive (true) or alpha-blended (false; reads red on a bright sky). */
    public static final boolean CURTAIN_ADDITIVE = false;

    /** The half-width of the red line and of its white core, in blocks. */
    public static final float LINE_HALF_WIDTH = 0.02F, CORE_HALF_WIDTH = 0.006F;
    /** The line's alpha at the nozzle, and the core's. */
    public static final float LINE_ALPHA = 0.95F, CORE_ALPHA = 0.7F;

    /** The red of the line (#FF2A38) and of the curtain (#FF1E2D); the line's core is this white-red. */
    public static final float[] LINE_RED = {1F, 0x2A / 255F, 0x38 / 255F}, CURTAIN_RED = {1F, 0x1E / 255F, 0x2D / 255F},
        CORE_WHITE = {1F, 0.86F, 0.88F};

    /** The trail fades in from this Mach number and is full from {@link #RAMP_FULL_MACH}. */
    public static final double RAMP_START_MACH = 0.3, RAMP_FULL_MACH = 0.9;

    /** The part of the trail within this many blocks of the camera fades out over {@link #NEAR_FADE_SPAN} blocks. */
    public static final float NEAR_FADE_START = 1.0F, NEAR_FADE_SPAN = 2.5F;

    /** The back nozzles of a Wingman, from its waist: up the body, out of its back, and to each side. */
    public static final double WINGMAN_UP = 0.42, WINGMAN_BACK = 0.24, WINGMAN_SIDE = 0.17;

    // ---- 2.0 electromagnetic energy ribbons (longer persistence, sleeker curtain, speed/stance palettes)

    /** Persistence of the 2.0 electromagnetic energy ribbon, in ticks (draws clean glowing arcs on hard turns). */
    public static final float RIBBON_AGE_TICKS = 7.0F;
    /** Sleek curtain depth of the 2.0 ribbon at the nozzle, in blocks (avoids twisting during dives). */
    public static final float RIBBON_CURTAIN_HEIGHT = 0.28F;
    /** Slightly wider glowing line and core for the 2.0 energy ribbon, in blocks. */
    public static final float RIBBON_LINE_HALF_WIDTH = 0.026F, RIBBON_CORE_HALF_WIDTH = 0.009F;
    /** Lower Mach ramp for Wingmen so cruising V-formations draw crisp stance-colored light-wing trails. */
    public static final double WINGMAN_RAMP_START_MACH = 0.14, WINGMAN_RAMP_FULL_MACH = 0.55;

    /** Quantum Suit dynamic palette: subsonic cyan (#3EE8FF) -> transonic violet (#A86BFF) -> supersonic white-hot core. */
    public static final float[] CYAN_LINE = {0x3E / 255F, 0xE8 / 255F, 1.0F};
    public static final float[] CYAN_CURTAIN = {0x0F / 255F, 0xB8 / 255F, 1.0F};
    public static final float[] CYAN_CORE = {0.86F, 0.98F, 1.0F};
    public static final float[] VIOLET_LINE = {0xA8 / 255F, 0x6B / 255F, 1.0F};
    public static final float[] VIOLET_CURTAIN = {0x7A / 255F, 0x3B / 255F, 1.0F};
    public static final float[] SUPERSONIC_CORE = {1.0F, 0.97F, 1.0F};

    /** Wingman stance palettes (OFFENSIVE crimson-orange, DISPERSAL amber-gold, ESCORT neon cyan, Living Lining emerald core). */
    public static final float[] WINGMAN_OFFENSIVE_LINE = {1.0F, 0x3B / 255F, 0x30 / 255F};
    public static final float[] WINGMAN_OFFENSIVE_CURTAIN = {1.0F, 0x5A / 255F, 0x1F / 255F};
    public static final float[] WINGMAN_DISPERSAL_LINE = {1.0F, 0xB8 / 255F, 0x2E / 255F};
    public static final float[] WINGMAN_DISPERSAL_CURTAIN = {1.0F, 0x8C / 255F, 0x1A / 255F};
    public static final float[] WINGMAN_ESCORT_LINE = {0x2E / 255F, 0xE6 / 255F, 1.0F};
    public static final float[] WINGMAN_ESCORT_CURTAIN = {0x0F / 255F, 0xA8 / 255F, 1.0F};
    public static final float[] LIVING_LINING_CORE = {0x6B / 255F, 1.0F, 0xB8 / 255F};

    /** Trail strength (0..1) for a flying Wingman at {@code mach}. */
    public static float wingmanLevel(double mach) {
        double k = (mach - WINGMAN_RAMP_START_MACH) / (WINGMAN_RAMP_FULL_MACH - WINGMAN_RAMP_START_MACH);
        k = Math.max(0, Math.min(1, k));
        return (float) (k * k * (3 - 2 * k));
    }

    /** Curtain depth of the 2.0 energy ribbon at {@code age} (0 nozzle, 1 tail). */
    public static float ribbonCurtainHeight(float age) {
        return RIBBON_CURTAIN_HEIGHT * (1 - CURTAIN_TAPER * clamp(age));
    }

    /** Speed-graded line colour for a Quantum Suit player: cyan below Mach 0.70, shifting to electric violet across Mach 1.0. */
    public static float[] playerLineColor(double mach) {
        float t = clamp((float) ((mach - 0.70) / 0.40));
        return lerpColor(CYAN_LINE, VIOLET_LINE, t);
    }

    /** Speed-graded curtain colour for a Quantum Suit player. */
    public static float[] playerCurtainColor(double mach) {
        float t = clamp((float) ((mach - 0.70) / 0.40));
        return lerpColor(CYAN_CURTAIN, VIOLET_CURTAIN, t);
    }

    /** Speed-graded core colour for a Quantum Suit player (white-hot past Mach 1.0). */
    public static float[] playerCoreColor(double mach) {
        float t = clamp((float) ((mach - 0.85) / 0.30));
        return lerpColor(CYAN_CORE, SUPERSONIC_CORE, t);
    }

    /** Stance-colored line RGB for a Wingman (with subtle emerald infusion when Living Nano Lining is awakened). */
    public static float[] wingmanLineColor(WingmanMode mode, boolean livingLining) {
        float[] base = switch (mode == null ? WingmanMode.ESCORT : mode) {
            case OFFENSIVE -> WINGMAN_OFFENSIVE_LINE;
            case DISPERSAL -> WINGMAN_DISPERSAL_LINE;
            case ESCORT -> WINGMAN_ESCORT_LINE;
        };
        return livingLining ? lerpColor(base, LIVING_LINING_CORE, 0.20F) : base;
    }

    /** Stance-colored curtain RGB for a Wingman. */
    public static float[] wingmanCurtainColor(WingmanMode mode, boolean livingLining) {
        float[] base = switch (mode == null ? WingmanMode.ESCORT : mode) {
            case OFFENSIVE -> WINGMAN_OFFENSIVE_CURTAIN;
            case DISPERSAL -> WINGMAN_DISPERSAL_CURTAIN;
            case ESCORT -> WINGMAN_ESCORT_CURTAIN;
        };
        return livingLining ? lerpColor(base, LIVING_LINING_CORE, 0.15F) : base;
    }

    /** Core RGB for a Wingman trail: emerald-jade when Living Nano Lining is active, else bright white-tinted stance glow. */
    public static float[] wingmanCoreColor(WingmanMode mode, boolean livingLining) {
        if (livingLining) return LIVING_LINING_CORE;
        float[] line = wingmanLineColor(mode, false);
        return new float[]{0.65F + 0.35F * line[0], 0.65F + 0.35F * line[1], 0.65F + 0.35F * line[2]};
    }

    private static float[] lerpColor(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** The strength of the trail (0..1) at a Mach number: smooth from {@link #RAMP_START_MACH} to {@link #RAMP_FULL_MACH}. */
    public static float level(double mach) {
        double k = (mach - RAMP_START_MACH) / (RAMP_FULL_MACH - RAMP_START_MACH);
        k = Math.max(0, Math.min(1, k));
        return (float) (k * k * (3 - 2 * k));
    }

    /** The fade along the trail at {@code age} (0 the nozzle, 1 the end): 1 to 0, smooth at both ends. */
    public static float along(float age) {
        float a = clamp(age);
        return (1 - a) * (1 - a) * (1 + 2 * a);
    }

    /** The curtain's depth at {@code age} along the trail. */
    public static float curtainHeight(float age) {
        return CURTAIN_HEIGHT * (1 - CURTAIN_TAPER * clamp(age));
    }

    /** The curtain's alpha at depth {@code d} (0 at the line, 1 at its lower edge), as a share of its alpha at the line. */
    public static float curtainFade(float d) {
        return (float) Math.pow(1 - clamp(d), CURTAIN_FADE_POWER);
    }

    /** The alpha factor for a point {@code distance} blocks from the camera. */
    public static float nearFade(double distance) {
        return clamp((float) ((distance - NEAR_FADE_START) / NEAR_FADE_SPAN));
    }

    /**
     * The way the curtain hangs from a point of the line whose path runs along {@code tangent}: world down with the part
     * along the path taken out. When the path runs (nearly) straight up or down, it hangs along {@code fallback} (the side
     * of a camera-facing ribbon) instead.
     */
    public static Vec3 hang(Vec3 tangent, Vec3 fallback) {
        Vec3 t = tangent.lengthSqr() < 1.0E-12 ? new Vec3(0, 0, 1) : tangent.normalize();
        Vec3 down = new Vec3(0, -1, 0);
        Vec3 h = down.subtract(t.scale(down.dot(t)));
        double len = h.length();
        if (len > 0.3) return h.scale(1 / len);
        Vec3 f = fallback.subtract(t.scale(fallback.dot(t)));
        Vec3 blend = len < 1.0E-6 ? f : h.scale(len / 0.3).add(f.scale(1 - len / 0.3));
        return blend.lengthSqr() < 1.0E-12 ? new Vec3(1, 0, 0) : blend.normalize();
    }

    private static float clamp(float x) { return Math.max(0F, Math.min(1F, x)); }
}
