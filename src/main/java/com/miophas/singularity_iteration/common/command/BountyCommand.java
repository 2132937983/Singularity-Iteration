// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.exchange.Bounties;
import com.miophas.singularity_iteration.common.exchange.BountyBoard;
import com.miophas.singularity_iteration.common.exchange.BountyTables;
import com.miophas.singularity_iteration.common.exchange.EconomySettings;
import com.miophas.singularity_iteration.common.exchange.ExchangeData;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.ExchangeTables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * 0.1.10.26 (X3): {@code /mio_icif bounty} for operators and pack makers (permission level 2).
 * <ul>
 *   <li>{@code list}: the open bounties (needs, rewards and their sources, days left) and the weekly budgets.</li>
 *   <li>{@code tables}: the bounty tables of the datapacks and whether each may be posted now.</li>
 *   <li>{@code post <table>}: posts a table now (weight, cooldown and conditions are not checked; the rewards must be
 *       payable), to test a datapack.</li>
 *   <li>{@code cancel <id>}: removes a bounty; unpaid reserved items go back to the treasury and the vault.</li>
 *   <li>{@code errors}: the problems of the {@code si_exchange} datapacks found by the last reload.</li>
 *   <li>{@code settings}: every economy setting with its value and where it comes from (config, datapack, default).</li>
 * </ul>
 */
public final class BountyCommand {
    private BountyCommand() {}

    private static final String K = "command.mio_icif.bounty.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("bounty").requires(s -> s.hasPermission(2))
            .then(Commands.literal("list").executes(c -> list(c.getSource())))
            .then(Commands.literal("tables").executes(c -> tables(c.getSource())))
            .then(Commands.literal("post")
                .then(Commands.argument("table", ResourceLocationArgument.id())
                    .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                        ExchangeTables.tables().tables().keySet().stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull), b))
                    .executes(c -> post(c.getSource(), ResourceLocationArgument.getId(c, "table").toString()))))
            .then(Commands.literal("cancel")
                .then(Commands.argument("id", LongArgumentType.longArg(1))
                    .executes(c -> cancel(c.getSource(), LongArgumentType.getLong(c, "id")))))
            .then(Commands.literal("errors").executes(c -> errors(c.getSource())))
            .then(Commands.literal("settings").executes(c -> settings(c.getSource())))));
    }

    static String needs(BountyBoard.Bounty b) {
        StringBuilder s = new StringBuilder();
        for (BountyBoard.Need n : b.needs) {
            if (s.length() > 0) s.append(", ");
            s.append(n.item).append(' ').append(n.delivered).append('/').append(n.count);
        }
        return s.toString();
    }

    static String rewards(BountyBoard.Bounty b) {
        StringBuilder s = new StringBuilder();
        for (BountyBoard.Line l : b.lines) {
            if (s.length() > 0) s.append(", ");
            switch (l.kind) {
                case COINS -> s.append(l.amount).append(" coins");
                case UU -> s.append(l.amount).append(" mB UU");
                case ITEM -> s.append(l.item).append(" x").append(l.amount);
            }
            s.append(" (").append(l.source.name().toLowerCase(Locale.ROOT));
            if (l.paid > 0) s.append(", paid ").append(l.paid);
            s.append(')');
        }
        return s.toString();
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        BountyBoard board = d.board;
        BountyBoard.Rules r = Bounties.rules(server);
        source.sendSuccess(() -> Component.translatable(K + "budget", day + 1,
            board.issueLeft(BountyTables.Tier.ADVANCED, r, day), r.issueAdvanced(), board.issueLeft(BountyTables.Tier.SPECIAL, r, day), r.issueSpecial(),
            board.uuLeft(r, day), r.uuPerWeek(), board.vault.size()), false);
        if (board.bounties.isEmpty()) source.sendSuccess(() -> Component.translatable(K + "none"), false);
        for (BountyBoard.Bounty b : List.copyOf(board.bounties)) {
            String table = b.table.isEmpty() ? "-" : b.table;
            source.sendSuccess(() -> Component.translatable(K + "line", b.id, b.tier.id(), table, needs(b), rewards(b), Math.max(0, b.expiry - day),
                b.split.name().toLowerCase(Locale.ROOT), b.contribution.size()), false);
        }
        return board.bounties.size();
    }

    private static int tables(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        var parsed = ExchangeTables.tables();
        source.sendSuccess(() -> Component.translatable(K + "tables", parsed.tables().size(), parsed.pools().size()), false);
        for (BountyTables.Table t : parsed.tables().values()) {
            boolean now = d.board.eligible(t, day);
            source.sendSuccess(() -> Component.translatable(K + "table", t.id(), t.tier().id(), t.weight(), t.requirements().size(),
                t.pool().isEmpty() ? "-" : t.pool(), Component.translatable(now ? K + "eligible" : K + "waiting")), false);
        }
        return parsed.tables().size();
    }

    private static int post(CommandSourceStack source, String table) {
        if (!ExchangeTables.tables().tables().containsKey(table)) {
            source.sendFailure(Component.translatable(K + "no_table", table));
            return 0;
        }
        long id = Bounties.post(source.getServer(), table);
        if (id < 0) {
            source.sendFailure(Component.translatable(K + "not_posted", table));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(K + "posted", table, id), true);
        return 1;
    }

    private static int cancel(CommandSourceStack source, long id) {
        if (!Bounties.cancel(source.getServer(), id)) {
            source.sendFailure(Component.translatable(K + "no_bounty", id));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(K + "cancelled", id), true);
        return 1;
    }

    private static int errors(CommandSourceStack source) {
        List<String> errors = ExchangeTables.current().errors();
        source.sendSuccess(() -> Component.translatable(K + "errors", errors.size()), false);
        for (String e : errors) source.sendSuccess(() -> Component.literal(" - " + e), false);
        return errors.size();
    }

    private static int settings(CommandSourceStack source) {
        List<String> lines = EconomySettings.describe();
        source.sendSuccess(() -> Component.translatable(K + "settings", lines.size()), false);
        for (String l : lines) source.sendSuccess(() -> Component.literal(" " + l), false);
        return lines.size();
    }
}
