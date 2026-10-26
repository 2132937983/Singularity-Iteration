// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P5): the track of one locked creature for the Smart Guidance lock frame: it keeps following a creature
 * that went behind cover or out of the frame at its predicted position ("memory"), takes it back when it shows up again
 * near that position, and gives up on a track whose predictions keep missing. Pure arithmetic; units are blocks and
 * ticks; positions are the feet of the creature.
 *
 * <ul>
 *   <li>{@link #observe}: the creature is seen this tick. The prediction error (the distance between where the track
 *       expected it and where it is) goes into an average ({@link #error}, weight 0.3).</li>
 *   <li>{@link #coast}: not seen this tick; {@link #predicted} moves on with the motion estimate (straight, or along the
 *       arc of a circling creature, see {@link TargetTracker#predictHorizontal}).</li>
 *   <li>{@link #accepts}: a creature seen again is the same track when it is inside the gate around the predicted
 *       position: {@code 1.5 + 2.5 x error + 0.15 x ticks unseen} blocks. An enderman that teleported is not.</li>
 *   <li>{@link #lost}: unseen longer than the memory, or a poor track ({@link #quality} below 0.2 after 4 sightings).</li>
 * </ul>
 */
public final class MobTrack {
    /** Ticks a lock stays in memory: Mk.I, Mk.II. */
    public static final int MEMORY_MK1 = 20, MEMORY_MK2 = 40;

    private final TargetTracker tracker = new TargetTracker();
    private final double[] v = new double[3], h = new double[2];
    private double x, y, z, error;
    private int seen, coast;
    private final boolean flies;

    /** @param flies the creature flies: its height is predicted with its vertical motion (else it stays on its level) */
    public MobTrack(boolean flies) { this.flies = flies; }

    /** Ticks since it was last seen (0 = seen this tick). */
    public int unseen() { return coast; }

    /** Ticks it was seen in a row. */
    public int sightings() { return seen; }

    /** Average prediction error in blocks. */
    public double error() { return error; }

    /** The creature is seen at {@code (ox, oy, oz)} this tick. */
    public void observe(double ox, double oy, double oz) {
        if (seen > 0 || coast > 0) {
            double[] p = new double[3];
            predicted(1, p);
            double e = Math.sqrt((p[0] - ox) * (p[0] - ox) + (p[1] - oy) * (p[1] - oy) + (p[2] - oz) * (p[2] - oz));
            error = 0.7 * error + 0.3 * e;
        }
        if (coast > 1) {
            // seen again after a gap: keep the velocity, start the history at the new place
            tracker.velocity(v);
            tracker.prime(ox, oy, oz, v[0], flies ? v[1] : 0, v[2]);
            seen = 1;
        } else {
            tracker.update(ox, oy, oz);
            seen++;
        }
        coast = 0;
        x = ox; y = oy; z = oz;
    }

    /** The creature is not seen this tick. */
    public void coast() {
        if (seen > 0) coast++;
    }

    /** Where the creature should be now ({@code out} = x, y, z). */
    public void predicted(double[] out) { predicted(0, out); }

    /** Ticks of vertical motion a flyer is predicted for (flyers bob up and down: a longer straight line misses). */
    public static final int FLYER_RISE_TICKS = 5;

    /** Where it should be {@code ahead} ticks after now. */
    private void predicted(int ahead, double[] out) {
        int t = coast + ahead;
        tracker.predictHorizontal(t, h);
        tracker.velocity(v);
        out[0] = h[0]; out[1] = y + (flies ? v[1] * Math.min(t, FLYER_RISE_TICKS) : 0); out[2] = h[1];
    }

    /** The gate radius in blocks. */
    public double gate() { return 1.5 + 2.5 * error + 0.15 * coast; }

    /** A creature seen at {@code (ox, oy, oz)} this tick (before {@link #observe}) belongs to this track. */
    public boolean accepts(double ox, double oy, double oz) {
        if (seen == 0) return true;
        double[] p = new double[3];
        predicted(1, p);
        double d = Math.sqrt((p[0] - ox) * (p[0] - ox) + (p[1] - oy) * (p[1] - oy) + (p[2] - oz) * (p[2] - oz));
        return d <= gate();
    }

    /** 0 (useless) to 1 (seen for 8 ticks, predictions within a few tenths of a block). */
    public double quality() {
        return Math.min(1, seen / 8.0) / (1 + error / 0.4);
    }

    /** The track should be dropped (memory of {@code memory} ticks). */
    public boolean lost(int memory) {
        return coast > memory || seen >= 4 && quality() < 0.2;
    }
}
