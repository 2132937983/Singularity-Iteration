// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.core.runtime.PerfCounters;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;

/**
 * {@code /mio_icif perf} (0.1.10.7, permission level 2): the cost of the SI energy network on this
 * server, to compare before and after a change without a profiler.
 * <ul>
 *   <li>the average server tick and TPS;</li>
 *   <li>per dimension with an energy grid: the settlement time per tick (median, 95th percentile,
 *       maximum over the last 256 ticks) and the route plans built, resumed and stopped by the
 *       search budget;</li>
 *   <li>since the server started: deliveries, deliveries settled from a cached path profile and
 *       deliveries that visited their path, idle passes answered from the memo, commit validations
 *       reused;</li>
 *   <li>(0.1.10.8) native windows taken from the route plan cache, and machine ticks skipped by
 *       the idle sleep since the game started;</li>
 *   <li>(0.1.10.10) per tick over the last 100 ticks: setChanged calls, save marks, client syncs
 *       sent and requested, lamp checks, neighbour capability lookups and how many came from the
 *       cache, machine ticks skipped by the sleep; and the saturated windows since the start.</li>
 * </ul>
 */
public final class PerfCommand {
    private PerfCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("perf")
            .requires(source -> source.hasPermission(2))
            .executes(context -> report(context.getSource()))));
    }

    private static int report(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        double tickMs = server.getAverageTickTimeNanos() / 1.0E6;
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, tickMs));
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.header", ms(tickMs), String.format(Locale.ROOT, "%.1f", tps)), false);
        IndependentSiEnergy engine = IndependentSiEnergy.current(server);
        if (engine == null) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.none"), false);
            return 1;
        }
        for (ServerLevel level : server.getAllLevels()) {
            var timing = engine.performanceMetrics(level);
            if (timing.samples() == 0) continue;
            var routing = engine.routingMetrics(level);
            String name = level.dimension().location().toString();
            source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.level", name,
                ms(timing.p50Nanos() / 1.0E6), ms(timing.p95Nanos() / 1.0E6), ms(timing.maxNanos() / 1.0E6), timing.samples(),
                routing.plansBuilt(), routing.plansResumed(), routing.budgetYields()), false);
        }
        var metrics = engine.metrics();
        long[] counters = engine.optimizationCounters();
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.paths", metrics.deliveryCount(),
            counters[0], counters[1], counters[2], counters[3]), false);
        // 0.1.10.8: native windows from the plan cache, machine ticks skipped by idle sleep
        long skipped = com.miophas.singularity_iteration.core.runtime.processing.MachineSleep.skippedTicks();
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.reuse", counters[4], skipped), false);
        // 0.1.10.20: grid zones (G1) and steady windows (G2)
        long[] zones = engine.gridReuseCounters();
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.zones", zones[0], zones[1], zones[2]), false);
        // 0.1.10.10 (D7): the load of the last 100 ticks, per tick
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.load",
            avg(PerfCounters.Kind.SET_CHANGED), avg(PerfCounters.Kind.SAVE_MARKS),
            avg(PerfCounters.Kind.SYNC_SENT), avg(PerfCounters.Kind.SYNC_REQUESTS),
            avg(PerfCounters.Kind.LAMP_CHECKS), avg(PerfCounters.Kind.CAP_LOOKUPS), avg(PerfCounters.Kind.CAP_CACHED),
            avg(PerfCounters.Kind.SLEEP_SKIPS), counters[5]), false);
        // 0.1.10.38: processing machines per tick - full ticks, ticks asleep, lit state changes, redstone reads
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.machines",
            avg(PerfCounters.Kind.MACHINE_TICKS), avg(PerfCounters.Kind.SLEEP_SKIPS),
            avg(PerfCounters.Kind.LIT_FLIPS), avg(PerfCounters.Kind.REDSTONE_READS)), false);
        // 0.1.10.39: FE per tick (universal cables) - into cables, exported, imported, neighbour probes (skipped)
        source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.fe",
            avg(PerfCounters.Kind.FE_INTO_CABLES), avg(PerfCounters.Kind.FE_EXPORTED), avg(PerfCounters.Kind.FE_IMPORTED),
            avg(PerfCounters.Kind.FE_PROBES), avg(PerfCounters.Kind.FE_PROBES_SKIPPED)), false);
        return 1;
    }

    /** Average per tick of the last window with one decimal, or "-" before the first window. */
    private static String avg(PerfCounters.Kind kind) {
        double value = PerfCounters.perTick(kind);
        return value < 0 ? "-" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String ms(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
