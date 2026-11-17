// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

/**
 * 0.1.11.19: a strip-chart recorder for the generator screens: {@link #SAMPLES} samples, one every {@link #EVERY}
 * ticks (12 seconds in all). A sample is a value times 4 plus a two-bit mark (0 none; the machines say what 1 and 2
 * mean). Not saved: a reloaded machine starts a new chart.
 */
public final class Recorder {
    public static final int SAMPLES = 48, EVERY = 5;
    private final int[] samples = new int[SAMPLES];
    private int head, mark;
    private double sum;
    private int count;

    /** Adds a value for this tick; every {@link #EVERY} ticks the mean of them is written with the strongest mark. */
    public void tick(long gameTime, double value) {
        sum += value;
        count++;
        if (gameTime % EVERY != 0) return;
        int v = (int) Math.max(0, Math.min(Integer.MAX_VALUE / 4 - 1, Math.round(sum / Math.max(1, count))));
        samples[head] = v * 4 + mark;
        head = (head + 1) % SAMPLES;
        sum = 0;
        count = 0;
        mark = 0;
    }

    public void mark(int m) { mark = Math.max(mark, m & 3); }

    /** Sample {@code i} counted from the oldest. */
    public int get(int i) { return samples[(head + i) % SAMPLES]; }

    public static int value(int sample) { return sample >>> 2; }
    public static int markOf(int sample) { return sample & 3; }
}
