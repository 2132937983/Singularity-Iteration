package com.miophas.singularity_iteration.core.api.energy;

import java.util.Objects;

/** Value-only snapshot. Routing/timing are per dimension; recovery counters are per server. */
public record EnergyDiagnostics(boolean available, Routing routing, Recovery recovery, Timing timing) {
    public EnergyDiagnostics {
        Objects.requireNonNull(routing);
        Objects.requireNonNull(recovery);
        Objects.requireNonNull(timing);
    }
    public record Routing(long topologyGenerations, long plansBuilt, long plansResumed, long budgetYields) { }
    public record Recovery(long rebuilds, long isolatedFailures, int recoveringLevels,
                           int quarantinedChunks, String lastReason) {
        public Recovery { Objects.requireNonNull(lastReason); }
    }
    /** Nanoseconds, aggregated over at most 256 completed ticks (START + END). */
    public record Timing(int samples, long p50Nanos, long p95Nanos, long p99Nanos, long maxNanos) { }
    public static EnergyDiagnostics unavailable() {
        return new EnergyDiagnostics(false, new Routing(0, 0, 0, 0),
            new Recovery(0, 0, 0, 0, ""), new Timing(0, 0, 0, 0, 0));
    }
}
