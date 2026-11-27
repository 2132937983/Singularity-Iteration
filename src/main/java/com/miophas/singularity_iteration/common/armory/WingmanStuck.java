// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.224 (cave overhaul G5): the stall monitor of one Wingman, pure bookkeeping so that tests can run it offline.
 *
 * <p>Every steer to a goal reports the distance left (along the path when there is one). When that distance has not beaten
 * its best for this goal by {@link #PROGRESS} blocks in {@link #STALL_TICKS} ticks, and the Wingman is farther than
 * {@link #NEAR} from the goal, the monitor takes the next step: {@link Action#REPLAN} (search again round the place where
 * it stalled), {@link Action#BACK_OFF} (fly back to where it was some ticks ago), {@link Action#GIVE_UP} (drop the target,
 * or the order whose point cannot be reached), and from then on {@link Action#RECALL} (decision six A: the jump back to the
 * owner when the owner does not see it). Only a new best (or a new goal, or arriving) starts again from the first step, so
 * the way back of a back-off, which closes in again, does not reset it.
 *
 * <p>Before, only the formation flight watched for this (60 ticks, then the catch-up jump); in a fight or on an order a
 * Wingman could stay stuck for ever.
 */
public final class WingmanStuck {
    /** What to do about a stall. */
    public enum Action { NONE, REPLAN, BACK_OFF, GIVE_UP, RECALL }

    public static final int STALL_TICKS = 30, BACK_OFF_TICKS = 20, CRUMB_EVERY = 5, CRUMBS = 8;
    public static final double PROGRESS = 0.5, NEAR = 2.5, NEW_GOAL = 3.0;

    @Nullable private Vec3 goal;
    private double best = Double.MAX_VALUE;
    private int stall, level, backOff;
    /** Places of the last {@link #CRUMBS} x {@link #CRUMB_EVERY} ticks, oldest first once full. */
    private final Vec3[] crumbs = new Vec3[CRUMBS];
    private int crumbNext, crumbCount, tick;
    @Nullable private Vec3 backTo;
    /** Steps taken, for the tests and the debug view. */
    int stalls;

    /** Forgets the goal (a teleport, a new state). */
    public void reset() {
        goal = null;
        best = Double.MAX_VALUE;
        stall = 0;
        level = 0;
        backOff = 0;
        backTo = null;
    }

    /** The place to fly to while a back-off runs, else null. */
    @Nullable public Vec3 backOff() { return backOff > 0 ? backTo : null; }

    /** The step reached for the current goal (0: none yet). */
    public int level() { return level; }

    /**
     * One tick of a steer: the Wingman at {@code at} flies to {@code to} with {@code left} blocks to go. Returns what to do.
     */
    public Action track(Vec3 at, Vec3 to, double left) {
        if (tick++ % CRUMB_EVERY == 0) {
            crumbs[crumbNext] = at;
            crumbNext = (crumbNext + 1) % CRUMBS;
            crumbCount = Math.min(CRUMBS, crumbCount + 1);
        }
        if (goal == null || goal.distanceToSqr(to) > NEW_GOAL * NEW_GOAL) {
            reset();
            goal = to;
        }
        if (backOff > 0) { backOff--; return Action.NONE; }
        if (left <= NEAR) { best = left; stall = 0; level = 0; return Action.NONE; }
        if (left < best - PROGRESS) { best = left; stall = 0; level = 0; return Action.NONE; }
        if (++stall < STALL_TICKS) return Action.NONE;
        stall = 0;
        level++;
        stalls++;
        switch (level) {
            case 1 -> { return Action.REPLAN; }
            case 2 -> {
                backTo = crumbCount == 0 ? at : crumbs[crumbCount < CRUMBS ? 0 : crumbNext];
                backOff = BACK_OFF_TICKS;
                return Action.BACK_OFF;
            }
            case 3 -> { return Action.GIVE_UP; }
            default -> { return Action.RECALL; }
        }
    }
}
