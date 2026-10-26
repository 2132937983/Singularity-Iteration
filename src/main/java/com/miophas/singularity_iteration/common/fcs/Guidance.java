// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P4): steering laws of a homing projectile (the Smart Guidance aim assist), one steering step per
 * tick. Pure arithmetic; units are blocks, ticks and radians. Vectors are {@code double[3]} (x, y, z).
 *
 * <ul>
 *   <li><b>Proportional navigation (PN)</b>: the projectile turns at {@code N * Vc} times the rotation rate of the line
 *       of sight ({@code a = N * Vc * (Omega x r^)}, {@code Omega = (r x v_rel) / |r|^2}, {@code Vc} the closing speed).
 *       A line of sight that does not rotate means a collision course, so the projectile holds its heading; it flies
 *       a smooth arc instead of turning hard at the start and again at the end. {@link #NAV} = 4.</li>
 *   <li><b>Gravity term</b>: the command gets {@code +g} upward (the speed lost to gravity in this tick). Without it PN
 *       answers the drop only after the line of sight has turned and turns about 7 % more than pursuit.</li>
 *   <li><b>Augmented PN (APN)</b>: plus {@code N / 2} times the target acceleration across the line of sight, for a
 *       target that turns (the turn rate of its {@link TargetTracker}).</li>
 *   <li><b>Terminal phase</b>: with less than {@link #TERMINAL_TICKS} ticks to go, or when the range opens, the
 *       projectile flies straight at the intercept point (lead pursuit), as before 0.1.10.23 (the PN command grows
 *       without bound at short range; without this phase PN turned up to 25 % more at 24 degrees per tick).</li>
 *   <li><b>Pursuit</b> ({@link #pursue}): the law before 0.1.10.23: turn toward the lead point (the target after the
 *       straight-line time of flight).</li>
 * </ul>
 * Every step turns the heading by at most {@code maxTurn} radians and keeps the speed (the caller sets the speed).
 *
 * <p><b>Which law</b> ({@link #choose}, from the offline comparison {@code verify-si1123}, 2,400 shots per case on
 * the same seeds, six target motions): on the shots that both laws hit, PN+g turned 1.0 degree less per full bow shot
 * than pursuit at the same hit rate (APN+g 1.4 less), and 2 to 3 degrees less at 2.0 blocks per tick; this held for
 * every ground motion, both gravities (0.05, 0.03) and every launch speed from 2.0 blocks per tick. Not adopted where
 * it was worse: PN+g turned 0.2 to 0.9 degrees more against a flying target (APN+g did not), below 2.0 blocks per tick
 * (a short bow draw, snowballs) PN+g missed 0.3 to 2 % more shots, and without gravity PN turned more than pursuit.
 * Those shots keep pursuit.
 */
public final class Guidance {
    private Guidance() {}

    /** Navigation constant. */
    public static final double NAV = 4.0;
    /** Ticks to go below which the projectile pursues the intercept point. */
    public static final double TERMINAL_TICKS = 3.0;
    /** Slowest launch speed (blocks per tick) that uses PN / APN; slower shots keep pursuit. */
    public static final double PN_MIN_SPEED = 2.0;

    /** Steering law of one shot. */
    public enum Law { PURSUIT, PN, APN }

    /**
     * The law for a shot launched at {@code launchSpeed} with {@code gravity} per tick: APN with the gravity term when
     * the target turn rate is used ({@code augmented}: the Predictive Guidance synergy), PN with the gravity term for a
     * target that does not fly, pursuit otherwise (see the class comment for the numbers).
     */
    public static Law choose(double launchSpeed, double gravity, boolean augmented, boolean targetFlies) {
        if (gravity <= 0 || !(launchSpeed >= PN_MIN_SPEED)) return Law.PURSUIT;
        if (augmented) return Law.APN;
        return targetFlies ? Law.PURSUIT : Law.PN;
    }

    /**
     * One PN / APN step. {@code dir} is the unit heading now; the new unit heading goes to {@code out}.
     *
     * @param pos the projectile position
     * @param dir the unit heading
     * @param speed the projectile speed (blocks per tick)
     * @param target the target centre
     * @param targetVel the target velocity (blocks per tick)
     * @param targetAcc the target acceleration (blocks per tick squared) for APN, or null for PN
     * @param maxTurn the largest heading change in this tick (radians)
     * @param out the new heading (may be {@code dir})
     * @return true in the PN phase, false in the terminal (pursuit) phase
     */
    public static boolean navigate(double[] pos, double[] dir, double speed, double[] target, double[] targetVel, double[] targetAcc,
                                   double maxTurn, double[] out) {
        return navigate(pos, dir, speed, target, targetVel, targetAcc, NAV, TERMINAL_TICKS, 0, maxTurn, out);
    }

    /**
     * One PN / APN step with an explicit navigation constant, terminal threshold and gravity compensation: the command
     * gets {@code +gravity} upward (the speed the projectile lost to gravity in this tick), so the line of sight does
     * not have to rotate before PN answers the drop. {@code terminalTicks} 0 = pursuit only when the range opens.
     */
    public static boolean navigate(double[] pos, double[] dir, double speed, double[] target, double[] targetVel, double[] targetAcc,
                                   double nav, double terminalTicks, double gravity, double maxTurn, double[] out) {
        double rx = target[0] - pos[0], ry = target[1] - pos[1], rz = target[2] - pos[2];
        double range = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (range < 1.0E-6) { copy(dir, out); return false; }
        double vx = targetVel[0] - dir[0] * speed, vy = targetVel[1] - dir[1] * speed, vz = targetVel[2] - dir[2] * speed;
        double closing = -(rx * vx + ry * vy + rz * vz) / range;
        double togo = range / Math.max(closing, 0.05 * Math.max(speed, 0.05));
        if (closing <= 0 || togo <= terminalTicks) {
            pursueLead(pos, dir, speed, target, targetVel, targetAcc, togo, maxTurn, out);
            return false;
        }
        // LOS rate Omega = (r x v_rel) / |r|^2
        double r2 = range * range;
        double ox = (ry * vz - rz * vy) / r2, oy = (rz * vx - rx * vz) / r2, oz = (rx * vy - ry * vx) / r2;
        double ux = rx / range, uy = ry / range, uz = rz / range;
        // a = N Vc (Omega x r^)
        double k = nav * closing;
        double ax = k * (oy * uz - oz * uy), ay = k * (oz * ux - ox * uz) + gravity, az = k * (ox * uy - oy * ux);
        if (targetAcc != null) {
            // APN: + N/2 times the target acceleration across the line of sight
            double along = targetAcc[0] * ux + targetAcc[1] * uy + targetAcc[2] * uz;
            ax += nav / 2 * (targetAcc[0] - along * ux);
            ay += nav / 2 * (targetAcc[1] - along * uy);
            az += nav / 2 * (targetAcc[2] - along * uz);
        }
        double nx = dir[0] * speed + ax, ny = dir[1] * speed + ay, nz = dir[2] * speed + az;
        double n = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (n < 1.0E-9) { copy(dir, out); return true; }
        turn(dir, nx / n, ny / n, nz / n, maxTurn, out);
        return true;
    }

    /**
     * The law before 0.1.10.23: turn toward the lead point, the target position after the straight-line time of
     * flight {@code |r| / speed} (capped at 40 ticks) with its velocity.
     */
    public static void pursue(double[] pos, double[] dir, double speed, double[] target, double[] targetVel, double maxTurn, double[] out) {
        double rx = target[0] - pos[0], ry = target[1] - pos[1], rz = target[2] - pos[2];
        double t = Math.min(40, Math.sqrt(rx * rx + ry * ry + rz * rz) / Math.max(0.05, speed));
        aimAt(pos, dir, target[0] + targetVel[0] * t, target[1] + targetVel[1] * t, target[2] + targetVel[2] * t, maxTurn, out);
    }

    /** Terminal phase: straight at the intercept point after {@code togo} ticks (with the acceleration when known). */
    private static void pursueLead(double[] pos, double[] dir, double speed, double[] target, double[] targetVel, double[] targetAcc,
                                   double togo, double maxTurn, double[] out) {
        double t = Math.min(40, Math.max(0, Double.isFinite(togo) ? togo : 0));
        double half = targetAcc == null ? 0 : 0.5 * t * t;
        double ax = targetAcc == null ? 0 : targetAcc[0], ay = targetAcc == null ? 0 : targetAcc[1], az = targetAcc == null ? 0 : targetAcc[2];
        aimAt(pos, dir, target[0] + targetVel[0] * t + ax * half, target[1] + targetVel[1] * t + ay * half,
            target[2] + targetVel[2] * t + az * half, maxTurn, out);
    }

    private static void aimAt(double[] pos, double[] dir, double x, double y, double z, double maxTurn, double[] out) {
        double wx = x - pos[0], wy = y - pos[1], wz = z - pos[2];
        double w = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (w < 1.0E-6) { copy(dir, out); return; }
        turn(dir, wx / w, wy / w, wz / w, maxTurn, out);
    }

    /** Turns the unit heading {@code dir} toward the unit vector {@code (gx, gy, gz)} by at most {@code maxTurn}. */
    public static void turn(double[] dir, double gx, double gy, double gz, double maxTurn, double[] out) {
        double dot = Math.max(-1, Math.min(1, dir[0] * gx + dir[1] * gy + dir[2] * gz));
        double angle = Math.acos(dot);
        if (angle <= maxTurn) { out[0] = gx; out[1] = gy; out[2] = gz; return; }
        // the same blend as SmartGuidance before 0.1.10.23, then normalised
        double f = maxTurn / angle;
        double x = dir[0] * (1 - f) + gx * f, y = dir[1] * (1 - f) + gy * f, z = dir[2] * (1 - f) + gz * f;
        double n = Math.sqrt(x * x + y * y + z * z);
        if (n < 1.0E-9) { copy(dir, out); return; }
        out[0] = x / n; out[1] = y / n; out[2] = z / n;
    }

    /** Angle between two unit vectors (radians). */
    public static double angle(double[] a, double[] b) {
        return Math.acos(Math.max(-1, Math.min(1, a[0] * b[0] + a[1] * b[1] + a[2] * b[2])));
    }

    private static void copy(double[] from, double[] to) {
        if (from != to) { to[0] = from[0]; to[1] = from[1]; to[2] = from[2]; }
    }
}
