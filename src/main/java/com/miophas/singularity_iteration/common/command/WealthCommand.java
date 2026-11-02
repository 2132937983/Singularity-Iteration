// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.exchange.EconomySettings;
import com.miophas.singularity_iteration.common.exchange.ExchangeData;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.MarketRecords;
import com.miophas.singularity_iteration.common.exchange.WealthRanking;
import com.miophas.singularity_iteration.common.exchange.WealthRules;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.1.10.42: {@code /mio_icif market wealth}, the wealth ranking of the server ({@link WealthRanking}).
 * <ul>
 *   <li>{@code wealth [total|trade|futures|bounty]} (everyone): the top ten of the category and the caller's place.</li>
 *   <li>{@code wealth hide|show} (players): keeps the caller's name out of the ranking, or shows it again.</li>
 *   <li>{@code wealth player <player>} (permission 2): a player's total assets by part, place and this week's results.</li>
 *   <li>{@code wealth refresh} (permission 2): counts the assets again now (else at most every 10 seconds).</li>
 * </ul>
 */
public final class WealthCommand {
    private WealthCommand() {}

    private static final String K = "command.mio_icif.wealth.";
    private static final String G = "gui.mio_icif.exchange.ranking.";
    static final String[] CATEGORIES = {"total", "trade", "futures", "bounty"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> wealth = Commands.literal("wealth").executes(c -> top(c.getSource(), WealthRules.TOTAL));
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int category = i;
            wealth.then(Commands.literal(CATEGORIES[i]).executes(c -> top(c.getSource(), category)));
        }
        wealth.then(Commands.literal("hide").executes(c -> hide(c.getSource(), true)))
            .then(Commands.literal("show").executes(c -> hide(c.getSource(), false)))
            .then(Commands.literal("player").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                    .executes(c -> player(c.getSource(), GameProfileArgument.getGameProfiles(c, "player")))))
            .then(Commands.literal("refresh").requires(s -> s.hasPermission(2)).executes(c -> refresh(c.getSource())));
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("market").then(wealth)));
    }

    static String number(long v) { return String.format(Locale.ROOT, "%,d", v); }

    /** The top ten of {@code category} and the caller's place (a player). */
    static int top(CommandSourceStack source, int category) {
        MinecraftServer server = source.getServer();
        if (!EconomySettings.wealthRanking()) {
            source.sendFailure(Component.translatable(K + "off"));
            return 0;
        }
        ServerPlayer me = source.getPlayer();
        WealthRules.Ranked r = WealthRanking.rank(server, me == null ? null : me.getUUID(), category, WealthRules.TOP);
        Component name = Component.translatable(G + "category." + category);
        source.sendSuccess(() -> Component.translatable(K + "header", name, r.players()), false);
        if (r.top().isEmpty()) source.sendSuccess(() -> Component.translatable(K + "empty"), false);
        for (WealthRules.Place p : r.top()) {
            Object who = p.name().isEmpty() ? Component.translatable(G + "anonymous") : p.name();
            source.sendSuccess(() -> Component.translatable(K + "row", p.rank(), who, number(p.value())), false);
        }
        if (me != null) {
            source.sendSuccess(() -> r.myRank() > 0 ? Component.translatable(K + "mine", r.myRank(), r.players(), number(r.myValue()))
                : Component.translatable(K + "unranked"), false);
        }
        return r.players();
    }

    static int hide(CommandSourceStack source, boolean hidden) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ExchangeService.Result r = WealthRanking.setHidden(p, hidden);
        source.sendSuccess(() -> Component.translatable(r.key(), r.args()), false);
        return 1;
    }

    /** A player's assets by part, their place in total assets and this week's results (operators). */
    static int player(CommandSourceStack source, Collection<GameProfile> profiles) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        Map<java.util.UUID, long[]> all = WealthRanking.assets(server);
        int count = 0;
        for (GameProfile gp : profiles) {
            long[] parts = all.getOrDefault(gp.getId(), new long[WealthRules.PARTS]).clone();
            long total = WealthRules.total(parts);
            WealthRules.Ranked r = WealthRanking.rank(server, gp.getId(), WealthRules.TOTAL, 0);
            boolean hidden = d.rankingHidden.contains(gp.getId());
            source.sendSuccess(() -> Component.translatable(K + "player", gp.getName(), number(total), r.myRank(), r.players(),
                Component.translatable(K + (hidden ? "hidden" : "listed"))), false);
            source.sendSuccess(() -> Component.translatable(K + "parts", number(parts[WealthRules.PART_COINS]), number(parts[WealthRules.PART_UU]),
                number(parts[WealthRules.PART_GOODS]), number(parts[WealthRules.PART_ESCROW]), number(parts[WealthRules.PART_FUTURES])), false);
            source.sendSuccess(() -> Component.translatable(K + "parts_bonds", number(parts[WealthRules.PART_BONDS])), false);   // 0.1.10.43
            long[] week = d.records.week(gp.getId(), day);
            source.sendSuccess(() -> Component.translatable(K + "week", number(week[MarketRecords.TRADE_IN] - week[MarketRecords.TRADE_OUT]),
                number(week[MarketRecords.FUTURES]), number(week[MarketRecords.BOUNTY])), false);
            count++;
        }
        return count;
    }

    /** Counts the assets again now. */
    static int refresh(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        long t0 = System.nanoTime();
        WealthRanking.invalidate();
        int players = WealthRanking.assets(server).size();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        source.sendSuccess(() -> Component.translatable(K + "refreshed", players, ms), true);
        return players;
    }
}
