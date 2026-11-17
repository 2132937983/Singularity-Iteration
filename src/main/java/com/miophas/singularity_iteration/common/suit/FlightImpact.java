// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * 0.1.11.68 (quantum flight phase R2, decision three: hard landing, take-off burst, boost and brake, high-speed wall hit):
 * the rules behind the weight of the quantum flight. Pure functions; the client draws and shakes from them.
 *
 * <ul>
 *   <li><b>Landing</b>: a touchdown from maneuver flight is hard from {@link #HARD_FALL} blocks per tick of fall; its
 *       strength (0..1) grows to {@link #FULL_FALL}. A hard landing cracks the ground ({@link #cracks}) and shakes.</li>
 *   <li><b>Wall hit</b>: at {@link #WALL_SPEED} blocks per tick or more, losing most of the speed in one tick against a
 *       wall; strength to 3 blocks per tick.</li>
 *   <li><b>Camera shake</b>: a trauma (0..1) that the events add to and that fades by {@link #TRAUMA_DECAY} a tick; the
 *       angles go with the square of the trauma, on a smooth noise of three sines.</li>
 *   <li><b>FOV kick</b>: a boost widens the view at once and lets it settle over {@link #KICK_TICKS} ticks; a brake narrows
 *       it the same way.</li>
 * </ul>
 * The scale of the shake and of the FOV kick is a client option, 0..2 (1 = default, 0 = off). 0.1.11.69 (R3): the
 * shockwave of a hard landing pushes the creatures around ({@link #shove}), without damage.
 */
public final class FlightImpact {
    private FlightImpact() {}

    /** Fall speed (blocks per tick) from which a landing is hard, and at which it is at full strength. */
    public static final float HARD_FALL = 0.55F, FULL_FALL = 1.6F;
    /** Speed (blocks per tick) from which hitting a wall counts. */
    public static final float WALL_SPEED = 1.1F;
    /** Trauma lost per tick. */
    public static final float TRAUMA_DECAY = 0.06F;
    /** Ticks of a FOV kick. */
    public static final int KICK_TICKS = 10;
    /** Largest shake angles at full trauma and scale 1, degrees: yaw, pitch, roll. */
    public static final float SHAKE_YAW = 2.2F, SHAKE_PITCH = 2.6F, SHAKE_ROLL = 4.0F;

    /** Strength of a landing (0 when not hard, else 0.35..1) from the fall speed (blocks per tick, either sign). */
    public static float landing(float fall) {
        float f = Math.abs(fin(fall));
        if (f < HARD_FALL) return 0F;
        return 0.35F + 0.65F * clamp((f - HARD_FALL) / (FULL_FALL - HARD_FALL));
    }

    /**
     * Strength of a wall hit (0 when none, else 0.4..1): the speed before ({@code before}) is at least {@link #WALL_SPEED}
     * and the speed after is under 40 % of it, against a wall ({@code blocked}).
     */
    public static float wallHit(float before, float after, boolean blocked) {
        float b = fin(before), a = Math.abs(fin(after));
        if (!blocked || b < WALL_SPEED || a > 0.4F * b) return 0F;
        return 0.4F + 0.6F * clamp((b - WALL_SPEED) / (3F - WALL_SPEED));
    }

    /** The trauma after one tick. */
    public static float decay(float trauma) { return Math.max(0F, clamp(trauma) - TRAUMA_DECAY); }

    /** The trauma after an event of {@code amount} (stacks, at most 1). */
    public static float add(float trauma, float amount) { return clamp(clamp(trauma) + clamp(amount)); }

    /** Camera angles of a shake, degrees {yaw, pitch, roll}: the square of the trauma on a smooth noise, times the scale. */
    public static float[] shake(float trauma, float seconds, float scale) {
        float k = clamp(trauma), s = Math.max(0F, Math.min(2F, fin(scale))) * k * k, t = fin(seconds);
        return new float[]{SHAKE_YAW * s * noise(t, 0), SHAKE_PITCH * s * noise(t, 1), SHAKE_ROLL * s * noise(t, 2)};
    }

    /** A smooth noise of three sines in -1..1. */
    static float noise(float t, int channel) {
        float c = channel * 1.7F;
        return (float) (0.5 * Math.sin(t * 29.0 + c) + 0.3 * Math.sin(t * 17.3 + c * 2.3) + 0.2 * Math.sin(t * 43.1 + c * 3.1));
    }

    /**
     * The FOV kick {@code age} ticks after an event of {@code size} (+ wider, - narrower): all at once, then settling
     * (quadratic) to 0 at {@link #KICK_TICKS}; times the scale.
     */
    public static float fovKick(float age, float size, float scale) {
        float a = fin(age);
        if (a < 0 || a >= KICK_TICKS) return 0F;
        float left = 1 - a / KICK_TICKS;
        return fin(size) * left * left * Math.max(0F, Math.min(2F, fin(scale)));
    }

    /** FOV kick of a boost and of a full brake (as fractions of the FOV). */
    public static final float BOOST_KICK = 0.09F, BRAKE_KICK = -0.06F;

    /**
     * The cracks of a hard landing: {angle in radians, length in blocks} for each, their number and reach with the strength
     * (5 to 9 cracks, 0.8 to 3 blocks), the angles spread around and jittered by the seed (the same seed, the same cracks).
     */
    public static float[][] cracks(long seed, float strength) {
        float k = clamp(strength);
        int n = 5 + Math.round(4 * k);
        float[][] out = new float[n][2];
        long h = seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        for (int i = 0; i < n; i++) {
            h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
            float r0 = (h & 0xFFFF) / 65535F, r1 = ((h >>> 16) & 0xFFFF) / 65535F;
            out[i][0] = (float) ((i + 0.5 + (r0 - 0.5) * 0.6) * Math.PI * 2 / n);
            out[i][1] = (0.8F + 2.2F * k) * (0.6F + 0.4F * r1) * (i % 2 == 0 ? 1F : 0.7F);
        }
        return out;
    }

    /**
     * 0.1.11.69 (R3, decision four B): the reach of the shockwave of a hard landing, blocks: 2.5 at the weakest, 5.5 at full
     * strength.
     */
    public static float shoveRadius(float strength) { return 2.5F + 3F * clamp(strength); }

    /**
     * 0.1.11.69: the push of the shockwave on a creature {@code distance} blocks from the touchdown, {horizontal, vertical}
     * in blocks per tick: strongest at the centre, falling linearly to nothing at {@link #shoveRadius}; never damage.
     */
    public static float[] shove(float distance, float strength) {
        float r = shoveRadius(strength), d = fin(distance);
        if (d < 0 || d >= r) return new float[]{0F, 0F};
        float k = 1 - d / r, s = clamp(strength);
        return new float[]{k * (0.55F + 0.85F * s), k * (0.22F + 0.2F * s)};
    }

    private static float clamp(float v) { return Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }

    private static float fin(float v) { return Float.isFinite(v) ? v : 0F; }
}
