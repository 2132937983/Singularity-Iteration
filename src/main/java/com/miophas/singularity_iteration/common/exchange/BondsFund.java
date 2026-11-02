// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.BountyBoard.Payout;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketQuote;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.1.10.43 (exchange proposal idea 5, Q9): the treasury bonds ({@link BondRules}) and the treasury fund ({@link FundRules}) in
 * the game: purchases and exits, subscriptions and redemptions, the market day (maturities, the fund's week, settlements and
 * payments), the realised profit and loss of the treasury the fund shares, and the view of the treasury tab.
 *
 * <p>Ledger ({@link ExchangeData.LedgerDay}): bond principal and fund subscriptions are frozen ({@code frozenIn}) until they are
 * paid back ({@code frozenOut}); bond interest comes from the treasury reserve first, the rest is new coins
 * ({@code bondInterest}, {@code bondReserve}); dividends are new coins ({@code fundDividend}); fund losses are frozen coins that
 * never come back ({@code fundLoss}); the early exit penalty and the redemption fee go into the reserve. Payments go to the
 * coin account.
 */
public final class BondsFund {
    private BondsFund() {}

    // ------------------------------------------------------------------ bonds

    /** The weekly rate of a bond bought now (ppm): the stabiliser's net issue of the last 7 closed days against its target. */
    public static int rateNow(ExchangeData d, long day) {
        ExchangeView.StabilizerLine s = ExchangeViews.stabilizerLine(d, day);
        return BondRules.weeklyRatePpm(s.enabled(), s.net7(), s.target());
    }

    /** The server's cap of all bond principal now ({@link Long#MAX_VALUE}: none). */
    public static long serverCap(ExchangeData d) { return BondRules.serverCap(d.supply, EconomySettings.bondMaxSupplyPercent()); }

    /** Coins of bond principal {@code player} may still buy. */
    public static long room(ExchangeData d, UUID player) {
        return BondRules.room(BondRules.held(d.bonds, player), EconomySettings.bondMaxPerPlayer(), BondRules.outstanding(d.bonds), serverCap(d));
    }

    /** Buys {@code units} bonds of {@code term} market days at today's rate with the player's coins (account first). */
    public static Result buy(ServerPlayer player, int term, long units) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        if (!EconomySettings.bondsEnabled()) return Result.fail("bond_off");
        if (!BondRules.validTerm(term) || units < 1 || units > BondRules.MAX_UNITS) return Result.fail("bad_quantity");
        UUID me = player.getUUID();
        long day = ExchangeService.today(server);
        long principal = BondRules.principal(units);
        long mine = BondRules.held(d.bonds, me), max = EconomySettings.bondMaxPerPlayer();
        long outstanding = BondRules.outstanding(d.bonds), cap = serverCap(d);
        if (principal > BondRules.room(mine, max, outstanding, cap)) {
            long playerRest = max <= 0 ? Long.MAX_VALUE : Math.max(0, max - mine);
            long serverRest = cap == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, cap - outstanding);
            if (serverRest < playerRest) return Result.fail("bond_server_cap", cap, BondRules.unitsRoom(serverRest));
            return Result.fail("bond_player_cap", max, BondRules.unitsRoom(playerRest));
        }
        int rate = rateNow(d, day);
        if (!BondRules.canAdd(d.bonds, me, term, rate, day + term)) return Result.fail("bond_holdings_full", BondRules.MAX_HOLDINGS);
        if (d.bonds.size() >= BondFundNbt.MAX_BONDS) return Result.fail("bond_holdings_full", BondRules.MAX_HOLDINGS);
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (!ExchangeService.takeCoins(plan, d, me, principal)) return Result.fail("not_enough_coins");
        plan.commit();
        BondRules.Bond b = BondRules.add(d.bonds, d.newId(), me, player.getGameProfile().getName(), units, term, rate, day);
        d.ledger(day).frozenIn += principal;
        d.setDirty();
        return Result.done("bond_bought", units, principal, BondRules.percent(rate), BondRules.interest(principal, rate, term), b.maturity + 1);
    }

    /** Ends a holding before its maturity: the principal less the penalty to the coin account, no interest. */
    public static Result exit(ServerPlayer player, long id) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        UUID me = player.getUUID();
        BondRules.Bond found = null;
        for (BondRules.Bond b : d.bonds) if (b.id == id && b.owner.equals(me)) found = b;
        if (found == null) return Result.fail("no_bond");
        long day = ExchangeService.today(server);
        long principal = found.principal(), penalty = BondRules.penalty(principal), back = principal - penalty;
        d.bonds.remove(found);
        ExchangeService.credit(d, me, Currency.COIN, back);
        d.ledger(day).frozenOut += principal;
        ExchangeService.toReserve(server, d, day, penalty);
        d.setDirty();
        return Result.done("bond_exited", found.units, back, penalty);
    }

    /** The market day: matured bonds pay their principal and interest to the coin account. */
    static void maturities(MinecraftServer server, ExchangeData d, long day) {
        List<BondRules.Bond> due = BondRules.due(d.bonds, day);
        if (due.isEmpty()) return;
        ExchangeData.LedgerDay l = d.ledger(day);
        Map<UUID, long[]> told = new LinkedHashMap<>();
        for (BondRules.Bond b : due) {
            long principal = b.principal(), interest = b.interest();
            ExchangeService.credit(d, b.owner, Currency.COIN, WealthRules.add(principal, interest));
            l.frozenOut += principal;
            long fromReserve = ExchangeService.fromReserve(d, day, interest);
            l.minted += interest - fromReserve;
            l.bondInterest += interest;
            l.bondReserve += fromReserve;
            long[] t = told.computeIfAbsent(b.owner, k -> new long[2]);
            t[0] = WealthRules.add(t[0], principal);
            t[1] = WealthRules.add(t[1], interest);
            d.bonds.remove(b);
        }
        told.forEach((id, t) -> {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) online.sendSystemMessage(Component.translatable("message.mio_icif.exchange.bond_matured", t[0], t[1]));
        });
        d.setDirty();
    }

    // ------------------------------------------------------------------ the fund

    /** Subscribes {@code units} fund units with the player's coins (account first); they settle at the next market day. */
    public static Result subscribe(ServerPlayer player, long units) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        if (!EconomySettings.treasuryFund()) return Result.fail("fund_off");
        if (units < 1 || units > FundRules.MAX_UNITS) return Result.fail("bad_quantity");
        UUID me = player.getUUID();
        FundRules.Investor inv = d.fund.investors.get(me);
        if (inv == null && d.fund.investors.size() >= MarketRecords.MAX_PLAYERS) return Result.fail("fund_full");
        long have = inv == null ? 0 : inv.units() + inv.pending;
        if (have + units > FundRules.MAX_UNITS) return Result.fail("fund_cap", FundRules.MAX_UNITS);
        if (inv != null && inv.pending == 0 && inv.lots.size() >= FundRules.MAX_LOTS) return Result.fail("fund_lots_full", FundRules.MAX_LOTS);
        long coins = Math.multiplyExact(units, FundRules.UNIT);
        long day = ExchangeService.today(server);
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (!ExchangeService.takeCoins(plan, d, me, coins)) return Result.fail("not_enough_coins");
        plan.commit();
        inv = d.fund.investor(me);
        inv.name = player.getGameProfile().getName();
        inv.pending += units;
        d.ledger(day).frozenIn += coins;
        d.setDirty();
        return Result.done("fund_subscribed", units, coins, day + 2);
    }

    /** Takes back the units waiting for the next market day (their coins to the coin account). */
    public static Result cancel(ServerPlayer player) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        FundRules.Investor inv = d.fund.investors.get(player.getUUID());
        if (inv == null || inv.pending <= 0) return Result.fail("fund_no_pending");
        long units = inv.pending, coins = Math.multiplyExact(units, FundRules.UNIT);
        inv.pending = 0;
        ExchangeService.credit(d, player.getUUID(), Currency.COIN, coins);
        d.ledger(ExchangeService.today(server)).frozenOut += coins;
        FundRules.prune(d.fund, BountyMath.week(ExchangeService.today(server)));
        d.setDirty();
        return Result.done("fund_cancelled", units, coins);
    }

    /** Books {@code units} of the player's units for redemption ({@link FundRules#book}); paid at the pay day, less the fee. */
    public static Result redeem(ServerPlayer player, long units) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        FundRules.Investor inv = d.fund.investors.get(player.getUUID());
        if (units < 1) return Result.fail("bad_quantity");
        if (inv == null || inv.free() < units) return Result.fail("fund_not_enough", inv == null ? 0 : inv.free());
        long day = ExchangeService.today(server);
        long last = FundRules.book(inv, units, day);
        if (last < 0) return Result.fail("fund_lots_full", FundRules.MAX_LOTS);
        d.setDirty();
        return Result.done("fund_redeem_booked", units, last + 1);
    }

    /**
     * The market day of the fund: the closed day counts for the lots' weights; at the start of a market week the realised
     * profit or loss of the week that ended is distributed (dividends to the coin accounts, losses from the principal); then
     * the units waiting settle and the booked redemptions due are paid (less the fee, into the reserve).
     */
    static void fundDay(MinecraftServer server, ExchangeData d, long day, long closing) {
        FundRules.Fund f = d.fund;
        FundRules.addDay(f, closing);
        long w = BountyMath.week(closing);
        if (BountyMath.week(day) != w) {
            f.pl.headMap(w).clear();   // weeks the server did not close (a jump of the day)
            FundRules.Distribution r = FundRules.distribute(f, w, d.records.totalCost());
            ExchangeData.LedgerDay l = d.ledger(day);
            r.dividends().forEach((id, coins) -> ExchangeService.credit(d, id, Currency.COIN, coins));
            l.minted += r.week().dividends();
            l.fundDividend += r.week().dividends();
            l.fundLoss += r.week().losses();
            r.dividends().forEach((id, coins) -> tell(server, id, "fund_dividend", coins));
            r.losses().forEach((id, coins) -> tell(server, id, "fund_loss", coins));
        }
        FundRules.settle(f, day);
        for (Object[] x : FundRules.due(f, day)) {
            UUID id = (UUID) x[0];
            FundRules.Lot lot = (FundRules.Lot) x[1];
            long fee = Math.min(lot.principal, FundRules.fee(lot.principal)), back = lot.principal - fee;
            ExchangeService.credit(d, id, Currency.COIN, back);
            d.ledger(day).frozenOut += lot.principal;
            ExchangeService.toReserve(server, d, day, fee);
            tell(server, id, "fund_redeemed", lot.units, back, fee);
        }
        FundRules.prune(f, BountyMath.week(day));
        d.setDirty();
    }

    static void tell(MinecraftServer server, UUID id, String key, Object... args) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) online.sendSystemMessage(Component.translatable("message.mio_icif.exchange." + key, args));
    }

    /** Called by {@link ExchangeService#onNewDay} after the cost basis is settled: maturities, then the fund. */
    static void onNewDay(MinecraftServer server, ExchangeData d, long day, long closing) {
        restoreCost(d);
        maturities(server, d, day);
        fundDay(server, d, day, closing);
    }

    // ------------------------------------------------------------------ the realised profit and loss of the treasury

    /**
     * A purchase from the market of {@code take} items for {@code cost} coins, {@code fromStock} of them from the treasury stock
     * of {@code stockBefore}: the stock sold takes its cost out of the record; above its cost it is profit, below it loss; the
     * import premium is profit.
     */
    static void recordSale(MinecraftServer server, ExchangeData d, ExchangeService.Market m, String id, long stockBefore, int fromStock,
                           int take, long cost, long day) {
        if (take <= 0) return;
        double spread = EconomySettings.spread();
        int imported = take - fromStock;
        long stockBase = fromStock > 0 ? MarketQuote.buyCost((int) m.price(), fromStock, spread) : 0;
        long importBase = imported > 0 ? MarketQuote.buyCost((int) m.price(), imported, spread) : 0;
        long importCost = imported > 0 ? (long) Math.ceil(importBase * (1 + ExchangeService.importPremium(m))) : 0;
        double[] split = FundRules.saleSplit(cost, stockBase, importBase, importCost, fromStock, take);
        if (fromStock > 0) {
            FutureCommodity c = m.commodity();
            double unit = c == null ? 0 : ExchangeService.bidUnit(server, c, (int) m.price());
            double out = d.records.costOut(id, stockBefore, fromStock, unit);
            double[] s = FundRules.sale(split[0], out);
            FundRules.record(d.fund, day, (int) s[0], s[1]);
        }
        FundRules.record(d.fund, day, FundRules.IMPORT_FEE, split[1]);
    }

    /** Treasury items paid out as a bounty reward: their cost is a loss of the treasury. */
    static void bountyCost(ExchangeData d, Payout x, long day) {
        double unit = x.unitCost() > 0 ? x.unitCost() : d.records.unitCost(x.item());
        FundRules.record(d.fund, day, FundRules.BOUNTY_COST, unit * Math.max(0, x.amount()));
    }

    /** What one item of the treasury stock of {@code item} cost: its cost record, else the market's bid without pressure. */
    static double unitCost(MinecraftServer server, ExchangeData d, String item) {
        double u = d.records.unitCost(item);
        if (u > 0) return u;
        FutureCommodity c = FutureCommodityManager.getCommodityById(item);
        if (c == null) return 0;
        return ExchangeService.bidUnit(server, c, FutureMarketData.get(server.overworld()).getPrice(item, c.getBasePrice()));
    }

    /** Reserved treasury items that came back unpaid take their cost back into the record ({@link BountyBoard#returned}). */
    static void restoreCost(ExchangeData d) {
        if (d.board.returned.isEmpty()) return;
        Map<String, double[]> by = new LinkedHashMap<>();
        for (Payout x : d.board.returned) {
            if (!(x.unitCost() > 0) || x.amount() <= 0) continue;
            double[] v = by.computeIfAbsent(x.item(), k -> new double[2]);
            v[0] += x.amount();
            v[1] += x.unitCost() * x.amount();
        }
        d.board.returned.clear();
        by.forEach((item, v) -> {
            long n = (long) v[0];
            long stock = d.treasury.getOrDefault(item, 0L);
            d.records.costIn(item, Math.max(0, stock - n), n, v[1]);
        });
    }

    // ------------------------------------------------------------------ the view

    /** The bonds and the fund of {@code player} for the treasury tab. */
    public static BondFundView view(ServerPlayer player, ExchangeData d, long day, ExchangeView.StabilizerLine st) {
        UUID me = player.getUUID();
        int rate = BondRules.weeklyRatePpm(st.enabled(), st.net7(), st.target());
        long mine = BondRules.held(d.bonds, me), outstanding = BondRules.outstanding(d.bonds), cap = serverCap(d);
        long max = EconomySettings.bondMaxPerPlayer();
        List<BondFundView.Holding> holdings = new ArrayList<>();
        for (BondRules.Bond b : BondRules.of(d.bonds, me)) {
            if (holdings.size() >= BondFundView.MAX_HOLDINGS) break;
            holdings.add(new BondFundView.Holding(b.id, b.units, b.term, b.ratePpm, b.day, b.maturity, b.interest()));
        }
        BondFundView.Bonds bonds = new BondFundView.Bonds(EconomySettings.bondsEnabled(), st.enabled(), rate, st.net7(), st.target(), mine, max,
            outstanding, cap == Long.MAX_VALUE ? -1 : cap, BondRules.unitsRoom(BondRules.room(mine, max, outstanding, cap)), holdings);
        FundRules.Fund f = d.fund;
        FundRules.Investor inv = f.investors.get(me);
        List<BondFundView.Lot> lots = new ArrayList<>();
        if (inv != null) for (FundRules.Lot l : inv.lots) {
            if (lots.size() >= BondFundView.MAX_LOTS) break;
            lots.add(new BondFundView.Lot(l.units, l.principal, l.settled, l.redeemDay));
        }
        long invested = f.invested();
        double stockCost = d.records.totalCost();
        int share = (int) Math.round(FundRules.share(invested, 1, stockCost) * 1_000_000);
        double[] pl = f.pl.get(BountyMath.week(day));
        List<Long> thisWeek = new ArrayList<>();
        for (int k = 0; k < FundRules.KINDS; k++) thisWeek.add(pl == null ? 0L : Math.round(pl[k]));
        List<BondFundView.Week> weeks = new ArrayList<>();
        List<FundRules.Week> hist = new ArrayList<>(f.history);
        for (int i = hist.size() - 1; i >= 0 && weeks.size() < BondFundView.MAX_WEEKS; i--) {
            FundRules.Week w = hist.get(i);
            long my = 0;
            if (inv != null) for (long[] h : inv.history) if (h[0] == w.week()) my += h[1];
            weeks.add(new BondFundView.Week(w.week(), w.profit(), w.loss(), w.invested(), w.stockCost(), w.sharePpm(), w.dividends(), w.losses(),
                w.kept(), my));
        }
        int investors = 0;
        for (FundRules.Investor i : f.investors.values()) if (!i.lots.isEmpty() || i.pending > 0) investors++;
        BondFundView.Fund fund = new BondFundView.Fund(EconomySettings.treasuryFund(), inv == null ? 0 : inv.pending, lots,
            inv == null ? 0 : inv.paid, inv == null ? 0 : inv.lost, invested, f.pending(), Math.round(stockCost), share, thisWeek, weeks, investors);
        return new BondFundView(bonds, fund);
    }
}
