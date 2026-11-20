// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.122: the recent path of one nozzle, for its light trail ({@link LightTrailRules}). The client samples the nozzle every
 * frame: the newest sample is the head of the trail; the first sample of each game tick is also kept as a point, newest
 * first, at most {@link LightTrailRules#MAX_POINTS}, and points older than the trail are dropped. Times are game ticks
 * with the partial tick ({@code gameTime + partial}).
 */
public final class LightTrail {
    private final Vec3[] points = new Vec3[LightTrailRules.MAX_POINTS];
    private final double[] times = new double[LightTrailRules.MAX_POINTS];
    private final float[] levels = new float[LightTrailRules.MAX_POINTS];
    private int count;
    private long lastTick = Long.MIN_VALUE;
    private Vec3 head;
    private double headTime;
    private float headLevel;

    /** Takes the nozzle at {@code pos} at time {@code time} with the trail's strength {@code level} then. */
    public void sample(double time, Vec3 pos, float level) {
        if (head != null && time + 1.0E-6 < headTime) clear();   // time went back (a new world or a rewind)
        head = pos;
        headTime = time;
        headLevel = level;
        long tick = (long) Math.floor(time);
        if (tick != lastTick) {
            lastTick = tick;
            System.arraycopy(points, 0, points, 1, LightTrailRules.MAX_POINTS - 1);
            System.arraycopy(times, 0, times, 1, LightTrailRules.MAX_POINTS - 1);
            System.arraycopy(levels, 0, levels, 1, LightTrailRules.MAX_POINTS - 1);
            points[0] = pos;
            times[0] = time;
            levels[0] = level;
            count = Math.min(count + 1, LightTrailRules.MAX_POINTS);
        }
        // drop the points the trail has left behind (one stays past the end so the fade reaches zero between samples)
        while (count > 1 && time - times[count - 2] > LightTrailRules.MAX_AGE_TICKS) count--;
    }

    /** Forgets the path. */
    public void clear() {
        count = 0;
        head = null;
        lastTick = Long.MIN_VALUE;
    }

    /** The time of the newest sample, or NaN before the first. */
    public double headTime() { return head == null ? Double.NaN : headTime; }

    /**
     * The trail's points, newest first: the head, then the kept points older than it (the point of the head's own tick is
     * left out when it is the head itself). Each row is {x, y, z, time, level}.
     */
    public double[][] path() {
        if (head == null) return new double[0][];
        double[][] out = new double[count + 1][];
        int n = 0;
        out[n++] = new double[]{head.x, head.y, head.z, headTime, headLevel};
        for (int i = 0; i < count; i++) {
            if (headTime - times[i] < 1.0E-6) continue;
            out[n++] = new double[]{points[i].x, points[i].y, points[i].z, times[i], levels[i]};
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    /** The number of kept points (not counting the head). */
    public int size() { return count; }
}
