// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.20 (FCS P3): fire-control solution for a moving target. Pure arithmetic; units are blocks and ticks.
 *
 * <p>The projectile follows the vanilla order per tick: move, drag, gravity ({@code BallisticSolver}). The
 * solution is found by the aim-point iteration of fire-control computers (the "pseudo-target"): aim the launch
 * direction at an aim point, fly the shot, measure where it passes the target position at the time of flight,
 * move the aim point by the miss, and repeat. The outer loop moves the target position to the time of flight of
 * the latest shot. The result is the launch direction that hits the predicted target position, so the HUD pipper
 * on this direction includes the drop, the drag, the shooter's own velocity and the target motion during the
 * flight. Before 0.1.10.20 the lead point was the target position after a flight time that ignored gravity and
 * the needed pitch, computed once (no iteration).
 *
 * <p>A warm start (the aim point of the last tick) makes the usual case 1 to 3 shots per tick.
 */
public final class Intercept {
    private Intercept() {}

    /** Most ticks of flight considered (12 s, as {@code BallisticSolver.MAX_STEPS}). */
    public static final int MAX_TICKS = 240;
    /** The solution is accepted below this miss distance (blocks). */
    public static final double TOLERANCE = 0.05;
    private static final int MAX_SHOTS = 24;

    /** Launch profile: speed (blocks per tick), gravity (blocks per tick per tick), drag (factor per tick). */
    public record Profile(double speed, double gravity, double drag) { }

    /** The position of the target after {@code ticks} ticks (ticks may be fractional). */
    public interface TargetPath {
        void at(double ticks, double[] out);
    }

    /**
     * A solution.
     *
     * @param dx dy dz  unit launch direction
     * @param ticks     time of flight to the target
     * @param miss      distance between the shot and the target at that time (blocks)
     * @param ax ay az  the aim point used (for the warm start of the next tick)
     * @param shots     shots simulated
     */
    public record Solution(double dx, double dy, double dz, double ticks, double miss, double ax, double ay, double az, int shots) { }

    /**
     * Solves the shot from {@code launch} with the shooter velocity {@code shooterVelocity} (added to the launch
     * velocity, as vanilla does), or returns null when the target is out of reach.
     *
     * @param warm the solution of the last tick for the same target, or null
     */
    public static Solution solve(Profile profile, double[] launch, double[] shooterVelocity, TargetPath target, Solution warm) {
        if (!(profile.speed() > 1.0E-3)) return null;
        double[] goal = new double[3], aim = new double[3];
        target.at(0, goal);
        double t;
        if (warm != null) {
            aim[0] = warm.ax(); aim[1] = warm.ay(); aim[2] = warm.az(); t = warm.ticks();
        } else {
            aim[0] = goal[0]; aim[1] = goal[1]; aim[2] = goal[2];
            t = distance(launch, goal) / profile.speed();
        }
        double[] closest = new double[4];
        int shots = 0;
        double lastMiss = Double.POSITIVE_INFINITY;
        for (int outer = 0; outer < 8; outer++) {
            target.at(t, goal);
            if (warm == null && outer == 0) { aim[0] = goal[0]; aim[1] = goal[1]; aim[2] = goal[2]; }
            for (int inner = 0; inner < 6 && shots < MAX_SHOTS; inner++) {
                double[] dir = unit(aim[0] - launch[0], aim[1] - launch[1], aim[2] - launch[2]);
                if (dir == null) return null;
                shots++;
                if (!fly(profile, launch, shooterVelocity, dir, goal, closest)) return null;
                double ex = goal[0] - closest[0], ey = goal[1] - closest[1], ez = goal[2] - closest[2];
                lastMiss = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (lastMiss < TOLERANCE) break;
                aim[0] += ex; aim[1] += ey; aim[2] += ez;
            }
            double nextT = closest[3];
            boolean settled = Math.abs(nextT - t) < 0.25;
            t = nextT;
            if (settled && lastMiss < TOLERANCE || shots >= MAX_SHOTS) break;
        }
        if (!(lastMiss < 0.5)) return null;
        double[] dir = unit(aim[0] - launch[0], aim[1] - launch[1], aim[2] - launch[2]);
        if (dir == null) return null;
        return new Solution(dir[0], dir[1], dir[2], t, lastMiss, aim[0], aim[1], aim[2], shots);
    }

    /**
     * Flies one shot and finds the point of closest approach to {@code goal}: {@code out} = x, y, z and the
     * (fractional) tick. False when the shot never comes near (falls away or exceeds {@link #MAX_TICKS}).
     */
    static boolean fly(Profile p, double[] launch, double[] shooter, double[] dir, double[] goal, double[] out) {
        double x = launch[0], y = launch[1], z = launch[2];
        double vx = dir[0] * p.speed() + shooter[0], vy = dir[1] * p.speed() + shooter[1], vz = dir[2] * p.speed() + shooter[2];
        double best = Double.POSITIVE_INFINITY;
        boolean found = false;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            double nx = x + vx, ny = y + vy, nz = z + vz;
            // closest point of the segment (x, y, z) -> (nx, ny, nz) to the goal
            double sx = nx - x, sy = ny - y, sz = nz - z;
            double len2 = sx * sx + sy * sy + sz * sz;
            double f = len2 < 1.0E-12 ? 0 : ((goal[0] - x) * sx + (goal[1] - y) * sy + (goal[2] - z) * sz) / len2;
            f = Math.max(0, Math.min(1, f));
            double cx = x + sx * f, cy = y + sy * f, cz = z + sz * f;
            double d2 = (goal[0] - cx) * (goal[0] - cx) + (goal[1] - cy) * (goal[1] - cy) + (goal[2] - cz) * (goal[2] - cz);
            if (d2 < best) {
                best = d2; out[0] = cx; out[1] = cy; out[2] = cz; out[3] = tick + f; found = true;
            } else if (found && tick > 2) {
                return true;                         // past the closest approach
            }
            x = nx; y = ny; z = nz;
            vx *= p.drag(); vy *= p.drag(); vz *= p.drag();
            vy -= p.gravity();
        }
        return found && out[3] < MAX_TICKS - 1;
    }

    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] unit(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-9 ? null : new double[] {x / len, y / len, z / len};
    }
}
