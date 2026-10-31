// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 0.1.10.10 (D7): load counters of SI for {@code /mio_icif perf}. Each counter is a plain total
 * written on the server thread (a client-side call is not counted). Every {@link #WINDOW} server
 * ticks the counters of the last window become per-tick averages, so the command shows the current
 * load, not the load since the start.
 *
 * <p>Diagnostics only: no game logic reads these values.
 */
public final class PerfCounters {
    private PerfCounters() { }

    /** What is counted. */
    public enum Kind {
        /** {@code setChanged()} of SI energy block entities (each one updates up to 12 neighbour blocks). */
        SET_CHANGED,
        /** Save marks of a tick that need no neighbour update (the chunk is only marked for saving). */
        SAVE_MARKS,
        /** Client sync requests through {@code ClientSyncThrottle}. */
        SYNC_REQUESTS,
        /** Block entity updates that {@code ClientSyncThrottle} sent. */
        SYNC_SENT,
        /** Status lamp evaluations of {@code MachineStatusTracker}. */
        LAMP_CHECKS,
        /** Neighbour capability lookups of {@code NeighbourCaps}, and those answered by a cache. */
        CAP_LOOKUPS, CAP_CACHED,
        /** Machine ticks skipped by the idle sleep. */
        SLEEP_SKIPS,
        /** 0.1.10.38: full ticks of processing machines (not skipped by the idle sleep). */
        MACHINE_TICKS,
        /** 0.1.10.38: lit state changes of processing machines (each one is a full block update). */
        LIT_FLIPS,
        /** 0.1.10.38: redstone input reads of processing machines (six neighbour signals each). */
        REDSTONE_READS
    }

    /** Ticks of one averaging window. */
    public static final int WINDOW = 100;

    private static final Kind[] KINDS = Kind.values();
    private static final long[] TOTALS = new long[KINDS.length];
    private static final long[] WINDOW_START = new long[KINDS.length];
    private static final double[] PER_TICK = new double[KINDS.length];
    private static long ticks;
    private static boolean windowDone;
    private static boolean installed;

    /** Counts one event of {@code kind} (server thread). */
    public static void add(Kind kind) {
        TOTALS[kind.ordinal()]++;
        if (!installed) install();
    }

    /** Total since the game started. */
    public static long total(Kind kind) {
        return TOTALS[kind.ordinal()];
    }

    /** Average per tick over the last complete window, or -1 before the first window ended. */
    public static double perTick(Kind kind) {
        return windowDone ? PER_TICK[kind.ordinal()] : -1;
    }

    private static void tick(ServerTickEvent.Post event) {
        if (++ticks % WINDOW != 0) return;
        for (int i = 0; i < KINDS.length; i++) {
            PER_TICK[i] = (TOTALS[i] - WINDOW_START[i]) / (double) WINDOW;
            WINDOW_START[i] = TOTALS[i];
        }
        windowDone = true;
    }

    private static synchronized void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(PerfCounters::tick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            java.util.Arrays.fill(WINDOW_START, 0);
            System.arraycopy(TOTALS, 0, WINDOW_START, 0, TOTALS.length);
            java.util.Arrays.fill(PER_TICK, 0);
            windowDone = false;
            ticks = 0;
        });
    }
}
