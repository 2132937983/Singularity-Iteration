// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

/**
 * 0.1.10.31 (S-4): when a player wearing the quantum boots makes a boost jump or lands hard, and when the quantum leggings
 * sprint. Pure (java.* only); {@code SuitMotionFx} feeds it once per client tick for every nearby player.
 *
 * <p>The client sees its own player exactly, but another player only through the position packets of the server (every
 * 2 ticks) that the client eases in over 3 ticks, and an on-ground flag that arrives with the packet. So one rule set reads
 * both: the height and the vertical speed after the move of the tick ({@code y}, {@code dy}) and the on-ground flag.
 * <ul>
 *   <li><b>Boost jump:</b> within {@link #JUMP_WINDOW} ticks of leaving the ground the body rises faster than
 *       {@link #JUMP_SPEED} blocks a tick. A vanilla jump starts at 0.42 (0.62 with Jump Boost II, 0.72 with III); the
 *       boots add 0.3, 0.225, 0.17 ... in the next ticks, so the rise passes 0.85.</li>
 *   <li><b>Hard landing:</b> the fall from the highest point is at least {@link #LAND_HEIGHT} blocks and the body fell at
 *       least {@link #LAND_SPEED} blocks a tick in the last ticks before it stopped (a creative or jetpack descent at
 *       0.375 is soft). For another player the fall is measured to the position the client eases towards
 *       ({@code lerpTargetY}, the landed position that came with the on-ground flag), not to the eased position that is
 *       still in the air.</li>
 *   <li>Ticks in flight (jetpack thrust, Special Maneuver, elytra, water) start a new air phase from where they end.</li>
 * </ul>
 */
public final class MotionFxRules {
    private MotionFxRules() {}

    /** Rise (blocks a tick) that only a boost jump reaches. */
    public static final float JUMP_SPEED = 0.78F;
    /** Ticks after leaving the ground in which a boost jump is recognised. */
    public static final int JUMP_WINDOW = 8;
    /** Least fall (blocks) of a hard landing. */
    public static final float LAND_HEIGHT = 3.5F;
    /** Least fall speed (blocks a tick) of a hard landing. */
    public static final float LAND_SPEED = 0.45F;
    /** Least ground speed (blocks a tick) of the sprint streaks. */
    public static final float SPRINT_SPEED = 0.2F;

    public static final int NONE = 0, JUMP = 1, LAND = 2;

    /** Strength of a boost jump ring (0.4 .. 1) from the rise that recognised it. */
    public static float jumpStrength(double dy) {
        return (float) Math.max(0.4, Math.min(1.0, (dy - 0.6) / 0.45));
    }

    /** Strength of a landing ring (0.5 .. 1.4) from the fall height. */
    public static float landStrength(double fall) {
        return (float) Math.max(0.5, Math.min(1.4, 0.5 + (fall - LAND_HEIGHT) / 14.0));
    }

    /** True when the sprint streaks run: sprinting on the ground faster than {@link #SPRINT_SPEED}. */
    public static boolean streaks(boolean sprinting, boolean onGround, double groundSpeed) {
        return sprinting && onGround && groundSpeed >= SPRINT_SPEED;
    }

    /** The air phases of one player. */
    public static final class Tracker {
        private boolean air, jumped, seen;
        private int airTicks;
        private double groundY, maxY;
        /** The last three fall speeds while in the air (blocks a tick, positive down). */
        private final double[] fall = new double[3];
        private int fallAt;
        /** Strength and height (the ground) of the last event. */
        public float strength;
        public double eventY;

        /**
         * One tick of the player. {@code y}: height after the move of this tick (for another player the eased height);
         * {@code dy}: the move of this tick; {@code target}: the height the client eases towards (the last position of the
         * server, {@code lerpTargetY}; the own player passes {@code y}); {@code flight}: jetpack, Special Maneuver, elytra or
         * water. Returns {@link #NONE}, {@link #JUMP} or {@link #LAND}.
         */
        public int tick(double y, double dy, double target, boolean onGround, boolean flight) {
            if (!seen || flight) {
                seen = true;
                air = !onGround && !flight;
                jumped = true;                       // no boost jump out of a flight or out of nowhere
                groundY = maxY = target;
                java.util.Arrays.fill(fall, 0);
                return NONE;
            }
            if (!onGround) {
                if (!air) {
                    air = true;
                    jumped = false;
                    airTicks = 0;
                    maxY = Math.max(y, target);
                    java.util.Arrays.fill(fall, 0);
                }
                airTicks++;
                maxY = Math.max(maxY, Math.max(y, target));
                fall[fallAt++ % 3] = Math.max(0, -dy);
                if (!jumped && airTicks <= JUMP_WINDOW && dy >= JUMP_SPEED) {
                    jumped = true;
                    strength = jumpStrength(dy);
                    eventY = groundY;
                    return JUMP;
                }
                return NONE;
            }
            if (air) {
                // the on-ground flag comes with the landed position of the server: the fall is measured to it, not to the
                // eased position that is still in the air; the speed is that of the last airborne ticks
                air = false;
                double drop = maxY - target;
                double speed = Math.max(fall[0], Math.max(fall[1], fall[2]));
                groundY = target;
                if (drop >= LAND_HEIGHT && speed >= LAND_SPEED) {
                    strength = landStrength(drop);
                    eventY = target;
                    return LAND;
                }
                return NONE;
            }
            groundY = target;
            return NONE;
        }
    }
}
