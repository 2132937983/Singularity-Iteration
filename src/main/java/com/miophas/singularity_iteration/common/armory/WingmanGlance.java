// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 0.1.11.52 (gaze and Iron Man phase P0, decision three A): the idle glances of one Wingman, in place of the slow head
 * waves of 0.1.11.47 (which only depended on the age, so a squad deployed together turned its heads together).
 *
 * <p>Each Wingman waits its own random {@link #MIN_PAUSE}..{@link #MAX_PAUSE} ticks, then glances at something (the
 * client picks it: a creature that was just hurt, the nearest creature or the owner, else a random direction) for
 * {@link #MIN_HOLD}..{@link #MAX_HOLD} ticks and turns back. A glance never starts within {@link #SPACING} ticks of
 * another Wingman's glance ({@link Shared}): it waits a little instead. Outside idle (a target, speed) no glance starts
 * and a running one ends. Pure logic, so tests can run a squad offline.
 */
public final class WingmanGlance {
    /** Ticks between two glances of one Wingman (4 to 10 seconds). */
    public static final int MIN_PAUSE = 80, MAX_PAUSE = 200;
    /** Ticks a glance holds (1 to 2 seconds). */
    public static final int MIN_HOLD = 20, MAX_HOLD = 40;
    /** Fewest ticks between the starts of two glances in a squad. */
    public static final int SPACING = 20;
    /** Widest glance from the body, radians (yaw), and up or down (pitch). */
    public static final float MAX_YAW = 1.1F, MAX_PITCH = 0.5F;
    /** Fraction of the way to the wanted head angle per tick. */
    public static final float SMOOTH = 0.15F;

    /** The start of the last glance in a squad (one per client). */
    public static final class Shared {
        public long lastStart = Long.MIN_VALUE / 2;
    }

    private long next = Long.MIN_VALUE, until = Long.MIN_VALUE;
    private float wantYaw, wantPitch, yaw, pitch, yawO, pitchO;

    /** Picks what to look at: {yaw, pitch} relative to the body in radians, or null for a random direction. */
    @FunctionalInterface
    public interface Picker { float[] pick(); }

    /**
     * One tick. {@code idle} 0..1 (no target and no speed is 1); a glance starts only when idle is above 0.5. Returns true
     * when a glance started in this tick.
     */
    public boolean tick(long now, float idle, RandomSource random, Shared shared, Picker picker) {
        yawO = yaw;
        pitchO = pitch;
        boolean started = false;
        if (next == Long.MIN_VALUE) next = now + MIN_PAUSE / 2 + random.nextInt(MAX_PAUSE);   // a first glance at a random time
        boolean calm = idle > 0.5F;
        if (!calm) {
            until = Math.min(until, now);
            if (next < now + MIN_PAUSE / 2) next = now + MIN_PAUSE / 2 + random.nextInt(MIN_PAUSE);
        } else if (now >= next && now >= until) {
            if (now - shared.lastStart < SPACING) {
                next = shared.lastStart + SPACING + random.nextInt(SPACING);   // another Wingman just glanced: a little later
            } else {
                float[] at = picker == null ? null : picker.pick();
                if (at == null) at = new float[]{(random.nextFloat() * 2 - 1) * MAX_YAW * 0.8F, (random.nextFloat() - 0.6F) * 0.3F};
                wantYaw = Mth.clamp(at[0], -MAX_YAW, MAX_YAW);
                wantPitch = Mth.clamp(at[1], -MAX_PITCH, MAX_PITCH);
                until = now + MIN_HOLD + random.nextInt(MAX_HOLD - MIN_HOLD + 1);
                next = until + MIN_PAUSE + random.nextInt(MAX_PAUSE - MIN_PAUSE + 1);
                shared.lastStart = now;
                started = true;
            }
        }
        boolean looking = calm && now < until;
        float k = SMOOTH * Mth.clamp(idle, 0, 1) + (looking ? 0 : 0.1F);
        yaw += ((looking ? wantYaw : 0) - yaw) * Math.min(1, k);
        pitch += ((looking ? wantPitch : 0) - pitch) * Math.min(1, k);
        return started;
    }

    /** The head offset {yaw, pitch} in radians for rendering. */
    public float[] offset(float partialTick) {
        return new float[]{Mth.lerp(partialTick, yawO, yaw), Mth.lerp(partialTick, pitchO, pitch)};
    }

    /** True while a glance holds (for tests). */
    public boolean looking(long now) { return now < until; }
}
