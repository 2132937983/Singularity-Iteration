// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.exchange.ExchangeData;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /mio_icif market audit [pairs]} (0.1.10.21, permission level 2): the state of the exchange for operators.
 * <ul>
 *   <li>Listings, resting orders, commissions, open futures and their collateral.</li>
 *   <li>Coins created and destroyed by the market over the last 7 days (the ledger of the treasury tab).</li>
 *   <li>The pairs of players that traded most with each other in the last 7 days (default 5, at most 20): items, trades,
 *       value and share of all trades between players. A pair far ahead of the rest is worth a look (wash trading,
 *       transfers between accounts); the exchange itself already refuses trades within one player or team.</li>
 * </ul>
 */
public final class MarketAuditCommand {
    private MarketAuditCommand() {}

    private static final String K = "command.mio_icif.market_audit.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("market").then(Commands.literal("audit")
            .requires(source -> source.hasPermission(2))
            .executes(context -> audit(context.getSource(), 5))
            .then(Commands.argument("pairs", IntegerArgumentType.integer(1, 20))
                .executes(context -> audit(context.getSource(), IntegerArgumentType.getInteger(context, "pairs")))))));
    }

    private static int audit(CommandSourceStack source, int top) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        int orders = 0;
        for (ExchangeData.Book b : d.books.values()) orders += b.book.size();
        long collateral = 0;
        for (ExchangeData.Position p : d.positions) collateral += p.price * p.quantity;
        final int orderCount = orders;
        final long held = collateral;
        source.sendSuccess(() -> Component.translatable(K + "header", day + 1, d.listings.size(), orderCount, d.commissions.size(),
            d.positions.size(), held), false);
        long minted = 0, burned = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (day - l.day > 6) continue;
            minted += l.created();   // 0.1.10.35: into / out of circulation
            burned += l.destroyed();
        }
        final long m = minted, b = burned;
        source.sendSuccess(() -> Component.translatable(K + "ledger", m, b, m - b), false);
        long[] total = new long[1];
        var pairs = ExchangeService.auditPairs(server, top, total);
        if (pairs.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(K + "no_pairs"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(K + "pairs", total[0]), false);
        for (ExchangeService.PairVolume p : pairs) {
            String share = String.format(java.util.Locale.ROOT, "%.0f%%", 100.0 * p.quantity() / Math.max(1, total[0]));
            source.sendSuccess(() -> Component.translatable(K + "pair", name(server, p.a()), name(server, p.b()), p.quantity(), p.trades(),
                p.value(), share), false);
        }
        return pairs.size();
    }

    private static String name(MinecraftServer server, String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            var online = server.getPlayerList().getPlayer(id);
            if (online != null) return online.getGameProfile().getName();
            var cache = server.getProfileCache();
            if (cache != null) {
                var profile = cache.get(id);
                if (profile.isPresent()) return profile.get().getName();
            }
        } catch (IllegalArgumentException ignored) {
            // not a player id
        }
        return uuid;
    }
}
