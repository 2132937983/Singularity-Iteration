// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.exchange.BondRules;
import com.miophas.singularity_iteration.common.exchange.BondsFund;
import com.miophas.singularity_iteration.common.exchange.BountyMath;
import com.miophas.singularity_iteration.common.exchange.EconomySettings;
import com.miophas.singularity_iteration.common.exchange.ExchangeData;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.FundRules;
import com.miophas.singularity_iteration.common.exchange.WealthRules;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.1.10.43 (Q9): {@code /mio_icif market bonds} and {@code /mio_icif market fund}, the treasury bonds and the treasury fund
 * ({@link BondsFund}).
 * <ul>
 *   <li>{@code bonds} (everyone): the weekly rate of a purchase now, the caller's holdings and what they may still buy.</li>
 *   <li>{@code bonds all} (permission 2): all bonds: principal, holders, the server's cap, what matures in the next 7 market
 *       days, the interest of the last 7 days (and how much of it the reserve paid).</li>
 *   <li>{@code fund} (everyone): the fund's invested principal, stock cost and investors' share, this week's realised profit and
 *       loss, the last distributed weeks, the caller's units.</li>
 *   <li>{@code fund all} (permission 2): investors, waiting units, dividends and losses in total, the coins kept destroyed.</li>
 * </ul>
 */
public final class BondsCommand {
    private BondsCommand() {}

    private static final String K = "command.mio_icif.bonds.", F = "command.mio_icif.fund.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("market")
            .then(Commands.literal("bonds").executes(c -> bonds(c.getSource()))
                .then(Commands.literal("all").requires(s -> s.hasPermission(2)).executes(c -> bondsAll(c.getSource()))))
            .then(Commands.literal("fund").executes(c -> fund(c.getSource()))
                .then(Commands.literal("all").requires(s -> s.hasPermission(2)).executes(c -> fundAll(c.getSource()))))));
    }

    static String number(long v) { return String.format(Locale.ROOT, "%,d", v); }

    static void say(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable(key, args), false);
    }

    /** The rate now, the caller's holdings and room. */
    static int bonds(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        say(source, K + "rate", BondRules.percent(BondsFund.rateNow(d, day)), Component.translatable(K + (EconomySettings.bondsEnabled() ? "on" : "off")));
        ServerPlayer me = source.getPlayer();
        if (me == null) return 1;
        List<BondRules.Bond> mine = BondRules.of(d.bonds, me.getUUID());
        if (mine.isEmpty()) say(source, K + "none");
        for (BondRules.Bond b : mine)
            say(source, K + "row", number(b.units), b.term, BondRules.percent(b.ratePpm), b.maturity + 1, number(b.interest()));
        say(source, K + "room", number(BondRules.held(d.bonds, me.getUUID())), number(BondRules.unitsRoom(BondsFund.room(d, me.getUUID()))));
        return Math.max(1, mine.size());
    }

    /** All bonds (operators). */
    static int bondsAll(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        Set<UUID> holders = new HashSet<>();
        long duePrincipal = 0, dueInterest = 0;
        for (BondRules.Bond b : d.bonds) {
            holders.add(b.owner);
            // the 7 market days after today (the bonds due today were paid when the day began)
            if (b.maturity - day <= 7) {
                duePrincipal = WealthRules.add(duePrincipal, b.principal());
                dueInterest = WealthRules.add(dueInterest, b.interest());
            }
        }
        long cap = BondsFund.serverCap(d);
        say(source, K + "all", number(BondRules.outstanding(d.bonds)), holders.size(), d.bonds.size(),
            cap == Long.MAX_VALUE ? Component.translatable(K + "no_cap") : number(cap), number(d.supply));
        say(source, K + "all_due", number(duePrincipal), number(dueInterest));
        long interest = 0, reserve = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (day - l.day >= 7) continue;   // today and the 6 days before it
            interest += l.bondInterest;
            reserve += l.bondReserve;
        }
        say(source, K + "all_paid", number(interest), number(reserve));
        return d.bonds.size();
    }

    /** The fund's results and the caller's units. */
    static int fund(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        FundRules.Fund f = d.fund;
        long invested = f.invested();
        double stockCost = d.records.totalCost();
        say(source, F + "status", number(invested), number(Math.round(stockCost)),
            BondRules.percent((int) Math.round(FundRules.share(invested, 1, stockCost) * 1_000_000)),
            Component.translatable(F + (EconomySettings.treasuryFund() ? "on" : "off")));
        double[] pl = f.pl.get(BountyMath.week(day));
        say(source, F + "week", number(Math.round(FundRules.profit(pl))), number(Math.round(FundRules.loss(pl))));
        List<FundRules.Week> weeks = new ArrayList<>(f.history);
        for (int i = weeks.size() - 1, n = 0; i >= 0 && n < 4; i--, n++) {
            FundRules.Week w = weeks.get(i);
            say(source, F + "row", w.week() + 1, number(w.profit()), number(w.loss()), BondRules.percent(w.sharePpm()), number(w.dividends()),
                number(w.losses()));
        }
        ServerPlayer me = source.getPlayer();
        if (me == null) return 1;
        FundRules.Investor inv = f.investors.get(me.getUUID());
        if (inv == null) { say(source, F + "none"); return 1; }
        say(source, F + "mine", number(inv.units()), number(inv.principal()), number(inv.pending), number(inv.units() - inv.free()),
            number(inv.paid), number(inv.lost));
        return 1;
    }

    /** The fund in total (operators). */
    static int fundAll(CommandSourceStack source) {
        ExchangeData d = ExchangeData.get(source.getServer());
        FundRules.Fund f = d.fund;
        long paid = 0, lost = 0, kept = 0;
        int investors = 0;
        for (FundRules.Investor i : f.investors.values()) {
            paid = WealthRules.add(paid, i.paid);
            lost = WealthRules.add(lost, i.lost);
            if (!i.lots.isEmpty() || i.pending > 0) investors++;
        }
        for (FundRules.Week w : f.history) kept = WealthRules.add(kept, w.kept());
        say(source, F + "all", investors, number(f.invested()), number(f.pending()), number(paid), number(lost), number(kept));
        return investors;
    }
}
