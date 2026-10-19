// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.reactor.audit.ReactorIncidents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /mio_icif reactor audit} (0.1.10.12): the reactor accident audit.
 * <ul>
 *   <li>(no argument): the recorded incidents, newest first (id, time, place, kind, main cause); a click
 *       on a line opens its report. An operator sees all incidents; a player the incidents of their own
 *       reactors and those they were near.</li>
 *   <li>{@code <id>}: opens the report of one incident.</li>
 *   <li>{@code clear} (permission level 2): empties the log.</li>
 * </ul>
 */
public final class ReactorAuditCommand {
    private ReactorAuditCommand() {}
    private static final int MAX_LINES = 20;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("reactor").then(Commands.literal("audit")
            .executes(context -> list(context.getSource()))
            .then(Commands.literal("clear").requires(source -> source.hasPermission(2))
                .executes(context -> clear(context.getSource())))
            .then(Commands.argument("id", IntegerArgumentType.integer(1))
                .executes(context -> open(context.getSource(), IntegerArgumentType.getInteger(context, "id")))))));
    }

    private static boolean visible(CommandSourceStack source, CompoundTag incident) {
        return !(source.getEntity() instanceof ServerPlayer player) ? source.hasPermission(2) : ReactorIncidents.mayView(player, incident);
    }

    private static int list(CommandSourceStack source) {
        var incidents = ReactorIncidents.all(source.getServer()).stream().filter(i -> visible(source, i)).toList();
        if (incidents.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.mio_icif.reactor_audit.none"), false);
            return 0;
        }
        int total = incidents.size();
        source.sendSuccess(() -> Component.translatable("command.mio_icif.reactor_audit.header", total), false);
        var format = new SimpleDateFormat("MM-dd HH:mm:ss");
        int shown = 0;
        for (var incident : incidents) {
            if (shown++ >= MAX_LINES) {
                int rest = total - MAX_LINES;
                source.sendSuccess(() -> Component.translatable("command.mio_icif.reactor_audit.more", rest), false);
                break;
            }
            int id = incident.getInt("Id");
            MutableComponent line = Component.translatable("command.mio_icif.reactor_audit.entry", id,
                format.format(new Date(incident.getLong("Time"))),
                incident.getString("Dim") + " " + incident.getInt("X") + " " + incident.getInt("Y") + " " + incident.getInt("Z"),
                Component.translatable("reactor_audit.type." + incident.getString("Type")),
                Component.translatable("reactor_audit.cause." + ReactorIncidents.mainCause(incident)).withStyle(ChatFormatting.RED));
            line.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mio_icif reactor audit " + id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.mio_icif.reactor_audit.open"))));
            source.sendSuccess(() -> line, false);
        }
        return total;
    }

    private static int open(CommandSourceStack source, int id) {
        var incident = ReactorIncidents.get(source.getServer(), id);
        if (incident == null || !visible(source, incident)) {
            source.sendFailure(Component.translatable("command.mio_icif.reactor_audit.not_found", id));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            // console: the main cause as text
            source.sendSuccess(() -> Component.translatable("command.mio_icif.reactor_audit.console", id,
                Component.translatable("reactor_audit.cause." + ReactorIncidents.mainCause(incident))), false);
            return 1;
        }
        ReactorIncidents.open(player, incident);
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        int n = ReactorIncidents.clear(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.mio_icif.reactor_audit.cleared", n), true);
        return n;
    }
}
