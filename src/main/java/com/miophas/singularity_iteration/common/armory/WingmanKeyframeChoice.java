// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.225 (Wingman motion A0): which keyframe animation of batch AN1 a Wingman plays, pure so that tests can ask.
 *
 * <p>For now only a Wingman on the ground plays keyframes: it lands with {@code land_light} ({@link #LAND_TICKS} ticks), then
 * stands with {@code idle_stand}, now and then looking round ({@code idle_look}) or checking its suit ({@code idle_check}),
 * chosen per window of {@link #IDLE_WINDOW} ticks from its id so that a squad does not move as one. A hit plays one of the
 * four {@code hit_*} reactions on the upper body. In flight the code-driven poses stay ({@code null}); the walk of the cave
 * overhaul (G3) adds the rest.
 */
public final class WingmanKeyframeChoice {
    private WingmanKeyframeChoice() {}

    public static final String PREFIX = "animation.wingman.";
    /** Ticks of {@code land_light} (0.3 s). */
    public static final int LAND_TICKS = 6;
    /** Ticks per idle window. */
    public static final int IDLE_WINDOW = 120;

    /**
     * Development only: {@code -Dsi.wingman.keyframe=<short name>} makes every landed Wingman play that animation and hold its
     * last frame (the screenshot scene {@code wingman_keyframes} checks the calibration pose with it). Null in play.
     */
    @Nullable public static final String FORCED = System.getProperty("si.wingman.keyframe");

    /** The base animation (short name) of a Wingman in this state, or null for the code-driven poses. */
    @Nullable
    public static String base(boolean landed, float sinceLanding, int id, int tick) {
        if (!landed) return null;
        if (FORCED != null) return FORCED;
        if (sinceLanding < LAND_TICKS) return "land_light";
        int window = Math.floorDiv(tick + id * 37, IDLE_WINDOW);
        int roll = Math.floorMod(window * 1103515245 + id * 12345, 10);
        if (roll < 2) return "idle_look";
        if (roll == 2) return "idle_check";
        return "idle_stand";
    }

    /**
     * The hit reaction (short name) for a hit from {@code fromYawDeg} (the direction the blow came from, degrees, Minecraft
     * yaw) on a Wingman facing {@code bodyYawDeg}: front, back, left or right of the Wingman.
     */
    public static String hit(float bodyYawDeg, float fromYawDeg) {
        float d = wrap(fromYawDeg - bodyYawDeg);
        if (Math.abs(d) <= 45) return "hit_front";
        if (Math.abs(d) >= 135) return "hit_back";
        // a positive yaw turns right in Minecraft: a blow from the Wingman's right
        return d > 0 ? "hit_right" : "hit_left";
    }

    private static float wrap(float deg) {
        float d = deg % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
