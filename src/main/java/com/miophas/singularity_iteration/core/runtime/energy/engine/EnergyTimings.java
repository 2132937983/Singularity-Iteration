package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Arrays;

/** Bounded diagnostic ring; aggregates all settlement phases in one server tick. */
public final class EnergyTimings {
    private final long[] samples = new long[256];
    private int count, cursor;
    private long currentTick = Long.MIN_VALUE, currentNanos;
    public void add(long tick, long nanos) {
        if (currentTick != tick) {
            if (currentTick != Long.MIN_VALUE) {
                samples[cursor] = currentNanos; cursor = (cursor + 1) % samples.length;
                count = Math.min(count + 1, samples.length);
            }
            currentTick = tick; currentNanos = 0;
        }
        currentNanos += Math.max(0, nanos);
    }
    public record Snapshot(int samples, long p50Nanos, long p95Nanos, long p99Nanos, long maxNanos) { }
    public Snapshot snapshot() {
        if (count == 0) return new Snapshot(0, 0, 0, 0, 0);
        long[] sorted = Arrays.copyOf(samples, count); Arrays.sort(sorted);
        return new Snapshot(count, percentile(sorted, .50), percentile(sorted, .95), percentile(sorted, .99), sorted[count - 1]);
    }
    private static long percentile(long[] values, double quantile) { return values[(int) Math.ceil(values.length * quantile) - 1]; }
}
