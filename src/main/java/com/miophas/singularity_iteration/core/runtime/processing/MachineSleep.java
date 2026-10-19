// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

/**
 * 0.1.10.8: idle sleep of processing machines. A machine that allows it
 * ({@code AbstractProcessingMachineBlockEntity.idleSleepAllowed()}) and stood idle for
 * {@link #QUIET_TICKS} ticks in a row (no work, no progress, redstone allows work, nothing marked
 * changed, stored energy unchanged, empty battery slot) skips its production checks (recipe search,
 * output test) until its inventory or saved state changes, its stored energy changes, or at the latest
 * {@link #RECHECK_TICKS} ticks later. Its automation upgrades keep running. The common config can
 * switch it off (then every machine runs every tick, as before 0.1.10.8).
 */
public final class MachineSleep {
    private MachineSleep() { }

    /** Quiet idle ticks in a row before a machine sleeps. */
    public static final int QUIET_TICKS = 20;
    /** Longest sleep without a wake event: the machine then runs one full tick again. */
    public static final int RECHECK_TICKS = 20;

    private static volatile boolean enabled = true;
    /** Machine ticks skipped by sleep since the game started (server thread). */
    private static long skippedTicks;

    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean value) { enabled = value; }

    /** Machine ticks skipped by sleep since the game started. Read on the server thread. */
    public static long skippedTicks() { return skippedTicks; }

    /** Counts one skipped machine tick (server thread). */
    public static void noteSkipped() { skippedTicks++; }
}
