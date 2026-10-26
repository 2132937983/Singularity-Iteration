// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.20 (FCS P1): motion estimate of one tracked creature from one client position sample per tick, with a
 * coordinated-turn mode. Pure arithmetic; units are blocks, ticks and radians.
 *
 * <ul>
 *   <li><b>Velocity</b>: the mean of the last three position steps. The client moves an entity in straight
 *       three-tick segments (the server sends positions every three ticks and the client interpolates in three
 *       steps), and the three-step mean follows exactly that motion. 0.1.10.20 tested alpha-beta and
 *       alpha-beta-gamma filters against it ({@code verify-si1120}): none was better on straight, zigzag,
 *       circling, start-stop and random paths, and the acceleration term made the one-second prediction swing.</li>
 *   <li><b>Turn rate</b>: the heading change between three-tick chords, over the last {@value #TURN_WINDOW}
 *       chords. When the rates agree (spread at most {@value #TURN_SPREAD} rad per tick) and the mean is at least
 *       {@value #MIN_TURN} rad per tick, the creature circles: the prediction follows the arc (the coordinated-turn
 *       model of an IMM tracker). A single change of direction does not agree with itself over six chords, so a
 *       zigzag or a random walk keeps the straight prediction. In the test the error of the one-second prediction of
 *       a circling creature fell from 2.8 to 0.1 blocks; no other path got worse.</li>
 * </ul>
 */
public final class TargetTracker {
    /** Chords in the turn test. */
    public static final int TURN_WINDOW = 6;
    /** Largest standard deviation of the chord turn rates for the turn mode (rad per tick). */
    public static final double TURN_SPREAD = 0.004;
    /** Smallest mean turn rate for the turn mode (rad per tick). */
    public static final double MIN_TURN = 0.01;
    /** Samples kept: the oldest chord reaches back {@code TURN_WINDOW - 1 + 7} samples. */
    private static final int SIZE = TURN_WINDOW + 8;

    private final double[] xs = new double[SIZE], ys = new double[SIZE], zs = new double[SIZE];
    private int count, head;

    /** Adds the position sample of this tick. */
    public void update(double x, double y, double z) {
        head = (head + 1) % SIZE;
        xs[head] = x; ys[head] = y; zs[head] = z;
        if (count < SIZE) count++;
    }

    /**
     * 0.1.10.23: starts a new estimate that already moves with {@code (vx, vy, vz)} blocks per tick: four samples on
     * the line that ends at {@code (x, y, z)} (the server knows a mob's velocity when a guided shot starts; the turn
     * rate still needs {@value #TURN_WINDOW} + 7 real samples).
     */
    public void prime(double x, double y, double z, double vx, double vy, double vz) {
        count = 0;
        head = 0;
        for (int k = 3; k >= 0; k--) update(x - vx * k, y - vy * k, z - vz * k);
    }

    /** Samples kept (at most {@value #TURN_WINDOW} + 8). */
    public int samples() { return count; }

    private int at(int back) { return Math.floorMod(head - back, SIZE); }

    /** Velocity in blocks per tick (zero with fewer than two samples): {@code out} = x, y, z. */
    public void velocity(double[] out) {
        if (count < 2) { out[0] = out[1] = out[2] = 0; return; }
        int n = Math.min(3, count - 1);
        int now = at(0), then = at(n);
        out[0] = (xs[now] - xs[then]) / n; out[1] = (ys[now] - ys[then]) / n; out[2] = (zs[now] - zs[then]) / n;
    }

    /** The turn rate in rad per tick when the creature circles, else 0. */
    public double turnRate() {
        if (count < TURN_WINDOW + 7) return 0;
        double sum = 0, sumSq = 0;
        for (int k = 0; k < TURN_WINDOW; k++) {
            // chords (back k+7 -> k+4) and (back k+3 -> k): their midpoints are 4 ticks apart
            double h2 = heading(k + 3, k), h1 = heading(k + 7, k + 4);
            if (Double.isNaN(h1) || Double.isNaN(h2)) return 0;
            double d = h2 - h1;
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            double rate = d / 4;
            sum += rate; sumSq += rate * rate;
        }
        double mean = sum / TURN_WINDOW, spread = Math.sqrt(Math.max(0, sumSq / TURN_WINDOW - mean * mean));
        return Math.abs(mean) >= MIN_TURN && spread <= TURN_SPREAD ? mean : 0;
    }

    /** Heading of the chord from {@code back} samples ago to {@code toBack} samples ago, NaN when it is too short. */
    private double heading(int back, int toBack) {
        int a = at(back), b = at(toBack);
        double dx = xs[b] - xs[a], dz = zs[b] - zs[a];
        return dx * dx + dz * dz < 1.0E-6 ? Double.NaN : Math.atan2(dz, dx);
    }

    /**
     * The horizontal position after {@code ticks} (fractional allowed), from the last sample: straight with the
     * velocity, or along the arc in the turn mode. {@code out[0]} = x, {@code out[1]} = z.
     */
    public void predictHorizontal(double ticks, double[] out) {
        double[] v = new double[3];
        velocity(v);
        int now = at(0);
        double x = xs[now], z = zs[now], t = Math.max(0, ticks);
        double rate = turnRate(), speed = Math.hypot(v[0], v[2]);
        if (rate == 0 || speed < 0.02) {
            out[0] = x + v[0] * t; out[1] = z + v[2] * t;
            return;
        }
        // the three-step mean points 1.5 steps back along the arc
        double heading = Math.atan2(v[2], v[0]) + rate * 1.5;
        int whole = (int) Math.floor(t);
        for (int k = 1; k <= whole; k++) { x += Math.cos(heading + k * rate) * speed; z += Math.sin(heading + k * rate) * speed; }
        double f = t - whole;
        if (f > 0) { x += Math.cos(heading + (whole + 1) * rate) * speed * f; z += Math.sin(heading + (whole + 1) * rate) * speed * f; }
        out[0] = x; out[1] = z;
    }
}
