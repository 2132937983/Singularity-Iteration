// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;

/**
 * {@code /mio_icif grid} (0.1.11.30, cable audit T-3; permission level 2): the health of the SI energy grid, which was
 * only visible in the log: topology rebuilds and isolated failures, chunks in quarantine and the last reason; per
 * dimension the conductors, endpoints and FE ports, how many chunks with conductors do not tick now (a line is cut
 * there), settlement passes stopped by the time cap, and the EU dropped at FE ports on chunk unload.
 */
public final class GridCommand {
    private GridCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("grid")
            .requires(source -> source.hasPermission(2))
            .executes(context -> report(context.getSource()))));
    }

    private static int report(CommandSourceStack source) {
        IndependentSiEnergy engine = IndependentSiEnergy.current(source.getServer());
        if (engine == null) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.perf.none"), false);
            return 0;
        }
        var recovery = engine.topologyRecoveryMetrics();
        source.sendSuccess(() -> Component.translatable("command.mio_icif.grid.header", recovery.rebuilds(), recovery.isolatedFailures(),
            recovery.recoveringLevels(), recovery.quarantinedChunks(),
            recovery.lastReason() == null || recovery.lastReason().isEmpty() ? "-" : recovery.lastReason()), false);
        int shown = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            var health = engine.gridHealth(level);
            if (health.conductors() == 0 && health.endpoints() == 0) continue;
            shown++;
            String name = level.dimension().location().toString();
            source.sendSuccess(() -> Component.translatable("command.mio_icif.grid.level", name, health.conductors(), health.endpoints(),
                health.foreignPorts(), health.conductorChunks(), health.idleChunks(), health.timeCapped(),
                String.format(Locale.ROOT, "%.1f", health.foreignDroppedEu())), false);
            // 0.1.11.32 (cable audit E-1): blocks left disconnected at the grid's bounds
            if (health.overLimit() > 0)
                source.sendSuccess(() -> Component.translatable("command.mio_icif.grid.over_limit", name, health.overLimit()), false);
        }
        if (shown == 0) source.sendSuccess(() -> Component.translatable("command.mio_icif.grid.empty"), false);
        return 1;
    }
}
