// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

/** Reservations prevent nested/repeated calls from spending the same tick allowance. */
public final class TickTransferBudget {
    private final int limit;
    private long tick;
    private boolean initialized;
    private int remaining;

    public TickTransferBudget(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Positive transfer limit required");
        this.limit = limit;
    }
    public void begin(long currentTick) {
        if (!initialized || tick != currentTick) {
            initialized = true; tick = currentTick; remaining = limit;
        }
    }
    public int remaining() { return remaining; }
    public int reserve(int requested) {
        int granted = Math.min(remaining, Math.max(0, requested));
        remaining -= granted;
        return granted;
    }
    /** Only a known receipt permits releasing a reservation. */
    public void settle(int reserved, int accepted) {
        if (accepted < 0 || accepted > reserved || reserved < 0 || reserved > limit - remaining)
            throw new IllegalArgumentException("Invalid transfer receipt");
        remaining += reserved - accepted;
    }
}
