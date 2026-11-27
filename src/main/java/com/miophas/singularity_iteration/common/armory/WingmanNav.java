// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.222 (cave overhaul G2): the navigator of one Wingman. Every steer asks it where to fly this tick ({@link #aim}).
 *
 * <ul>
 *   <li>When the body fits all along the straight line to the goal (a body sweep, kept {@link #DIRECT_TICKS} ticks), the
 *       answer is the goal: in the open nothing changes.</li>
 *   <li>Otherwise it searches a path ({@link WingmanPathing}) to the goal's cell, a slice of at most {@link #TICK_BUDGET}
 *       nodes per tick and {@link #SERVER_BUDGET} for all Wingmen of a server tick, and follows it: the answer is the
 *       farthest of the next {@link #LOOK_AHEAD} waypoints that the body reaches in a straight line (the path pulled tight).
 *       Under a ceiling the search makes every block of air under the feet cost {@link #HEIGHT_COST}, so the path keeps to
 *       the floor.</li>
 *   <li>A path is kept while the goal stays within {@link #GOAL_SLACK} cells of its end and for at most {@link #PATH_TICKS}
 *       ticks (a moving goal, such as the approach point of a melee pass, shifts by a block or two all the time). When it is
 *       stale a new search starts and the old path is flown on until the new one is found; a Wingman that cannot reach any
 *       of its next waypoints drops its path and searches again.</li>
 *   <li>A goal without a path (the search failed) is remembered as unreachable for {@link #UNREACHABLE_TICKS} ticks, so it
 *       is not searched again every tick; meanwhile the answer is the goal itself (the avoidance takes over), and
 *       {@link #unreachable} tells the target choice (G4).</li>
 * </ul>
 */
public final class WingmanNav {
    public static final int TICK_BUDGET = 256, SERVER_BUDGET = 2048;
    public static final int DIRECT_TICKS = 3, PATH_TICKS = 100, UNREACHABLE_TICKS = 40, LOOK_AHEAD = 8;
    /** Cosine of the turn at a waypoint beyond which the Wingman brakes for it (about 45 degrees). */
    public static final double CORNER = 0.7;
    /** Near enough to a goal at the end of its path not to search again (blocks). */
    public static final double ARRIVED = 2.0;
    public static final double GOAL_SLACK = 3.0, HEIGHT_COST = 0.6, REACHED = 0.8, DIRECT_MAX = 32;

    private static long budgetTick = Long.MIN_VALUE;
    private static int budgetLeft;

    private final WingmanEntity w;
    @Nullable private Vec3 directGoal;
    private boolean direct;
    private int directAt = Integer.MIN_VALUE;
    @Nullable private WingmanPathing.Search search;
    @Nullable private List<Vec3> path;
    @Nullable private BlockPos pathGoal;
    private int index, pathAt, aimed = -1;
    private double pathCost;
    @Nullable private BlockPos lostGoal;
    private int lostAt;
    /** Searches started, found and failed (for tests and the debug view). */
    int searches, found, failed;

    WingmanNav(WingmanEntity w) { this.w = w; }

    /** The current path (feet places), or null when flying straight or without one. */
    @Nullable public List<Vec3> path() { return path; }

    /** The index of the next waypoint on {@link #path}. */
    public int index() { return index; }

    /**
     * Distance left to fly to {@code goal} along the current path from the waypoint the last {@link #aim} gave (the steer
     * brakes by it, not by the distance to that waypoint), or -1 when flying straight.
     */
    public double remaining(Vec3 goal) {
        if (path == null || aimed < 0) return -1;
        double d = w.position().distanceTo(path.get(aimed));
        // a turn at the waypoint: brake for the corner
        if (aimed < path.size() - 1) {
            Vec3 in = path.get(aimed).subtract(w.position()), out = path.get(aimed + 1).subtract(path.get(aimed));
            if (in.lengthSqr() > 1.0E-4 && out.lengthSqr() > 1.0E-4 && in.normalize().dot(out.normalize()) < CORNER) return d;
        }
        for (int i = aimed; i < path.size() - 1; i++) d += path.get(i).distanceTo(path.get(i + 1));
        return d + path.get(path.size() - 1).distanceTo(goal);
    }

    /** The cost of the current path (length plus height cost), or -1. */
    public double pathCost() { return path == null ? -1 : pathCost; }

    /** True when the last search for a goal near {@code goal} failed within the last {@link #UNREACHABLE_TICKS} ticks. */
    public boolean unreachable(Vec3 goal) {
        return lostGoal != null && w.tickCount - lostAt < UNREACHABLE_TICKS && lostGoal.distToCenterSqr(goal) < 2.5 * 2.5;
    }

    /** Drops the path and the search (a new order, a teleport). */
    void reset() {
        path = null;
        search = null;
        pathGoal = null;
        directGoal = null;
    }

    private static int take(Level level, int want) {
        long now = level.getGameTime();
        if (now != budgetTick) { budgetTick = now; budgetLeft = SERVER_BUDGET; }
        int n = Math.min(want, budgetLeft);
        budgetLeft -= n;
        return n;
    }

    /** Where to fly this tick on the way to {@code goal}. */
    Vec3 aim(Vec3 goal) {
        Level level = w.level();
        int now = w.tickCount;
        if (directGoal == null || now - directAt >= DIRECT_TICKS || now < directAt || directGoal.distanceToSqr(goal) > 0.5 * 0.5) {
            direct = goal.distanceToSqr(w.position()) <= DIRECT_MAX * DIRECT_MAX && WingmanSpace.reaches(level, w, goal);
            directGoal = goal;
            directAt = now;
        }
        if (direct) {
            path = null;
            search = null;
            aimed = -1;
            return goal;
        }
        BlockPos goalCell = BlockPos.containing(goal.x, goal.y + 1.0E-3, goal.z);
        boolean stale = path == null || pathGoal == null || pathGoal.distSqr(goalCell) > GOAL_SLACK * GOAL_SLACK
            || now - pathAt > PATH_TICKS || now < pathAt
            || index >= path.size() - 1 && goal.distanceToSqr(w.position()) > ARRIVED * ARRIVED;   // at the end, near the goal: done
        if (stale && !unreachable(goal)) {
            if (search == null || search.goal.distSqr(goalCell) > GOAL_SLACK * GOAL_SLACK) {
                double heightCost = WingmanSpace.of(w).kind().confined() ? HEIGHT_COST : 0;
                search = WingmanPathing.search(level, w, goal, heightCost);
                searches++;
                if (search == null) { lose(goalCell); return goal; }
            }
            int budget = take(level, TICK_BUDGET);
            if (budget > 0) {
                WingmanPathing.Status s = search.step(budget);
                if (s == WingmanPathing.Status.FOUND) {
                    path = WingmanPathing.waypoints(search);
                    pathCost = search.cost();
                    pathGoal = goalCell;
                    pathAt = now;
                    index = 0;
                    search = null;
                    found++;
                } else if (s == WingmanPathing.Status.FAILED) {
                    lose(goalCell);
                    search = null;
                }
            }
        }
        aimed = -1;
        if (path == null) return goal;
        return follow(level);
    }

    private void lose(BlockPos goalCell) {
        lostGoal = goalCell;
        lostAt = w.tickCount;
        failed++;
    }

    private Vec3 follow(Level level) {
        List<Vec3> p = path;
        // progress: the nearest waypoint ahead (a fast Wingman passes the early ones without touching them)
        int near = index;
        double nearD = Double.MAX_VALUE;
        for (int i = index; i < Math.min(p.size(), index + 2 * LOOK_AHEAD); i++) {
            double d = p.get(i).distanceToSqr(w.position());
            if (d < nearD) { nearD = d; near = i; }
        }
        index = near;
        if (index < p.size() - 1 && nearD < REACHED * REACHED) index++;
        int last = Math.min(p.size() - 1, index + LOOK_AHEAD);
        for (int i = last; i >= index; i--) {
            if (WingmanSpace.reaches(level, w, p.get(i))) { aimed = i; return p.get(i); }
        }
        // off the path (pushed, knocked back): search again next tick
        path = null;
        return p.get(index);
    }
}
