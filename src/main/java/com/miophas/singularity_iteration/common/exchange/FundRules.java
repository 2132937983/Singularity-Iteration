// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 0.1.10.43 (exchange proposal idea 5, Q9): the treasury fund. Pure (the offline harness runs it).
 *
 * <ul>
 *   <li><b>Face value fixed</b>: a unit costs {@link #UNIT} coins whatever the fund did; the stock is never valued at market
 *       prices (a daily valuation would let players buy after a fall and earn the mean reversion of the prices). A
 *       subscription is paid at once and becomes a lot at the next market day ({@link #settle}); a lot is held at least
 *       {@link #MIN_HOLD_DAYS} market days; a redemption is booked {@link #NOTICE_DAYS} market days ahead, cannot be taken back,
 *       and pays the lot's principal less {@link #FEE_PERMILLE} per mille (1 %, to the treasury reserve).</li>
 *   <li><b>Profit and loss</b>: only what the treasury realised in a market week ({@link #record}): stock sold to players above
 *       its cost and the import premium are profit; stock sold below its cost and stock paid out as bounty rewards (at its
 *       cost) are loss.</li>
 *   <li><b>The investors' share</b> of a week ({@link #share}): invested principal / (invested principal + cost of the treasury
 *       stock), at most {@link #MAX_SHARE}. The invested principal is the week's average (each lot's principal x the closed days
 *       it was in, divided by the days counted), so a lot that joins on the last day gets a seventh. A profit pays
 *       {@link #PAYOUT} of the investors' share as dividends by those weights (the rest stays destroyed); a loss takes the
 *       investors' share from their principal by the same weights ({@link #distribute}). A lot paid out during a week leaves
 *       that week: its days count for neither.</li>
 * </ul>
 */
public final class FundRules {
    private FundRules() {}

    public static final long UNIT = 1_000;
    public static final int MIN_HOLD_DAYS = 7, NOTICE_DAYS = 7, FEE_PERMILLE = 10;
    public static final double MAX_SHARE = 0.5, PAYOUT = 0.5;
    /** Lots per player (subscriptions settled on the same day are one lot; a partial redemption splits one). */
    public static final int MAX_LOTS = 32;
    /** Units per player at the most (a bound for the arithmetic, not a policy: 1,000,000,000 coins). */
    public static final long MAX_UNITS = 1_000_000;
    /** Market weeks of history kept (the fund's and each investor's). */
    public static final int HISTORY = 8;
    /** The kinds of the realised profit and loss of the treasury. */
    public static final int SALE_PROFIT = 0, IMPORT_FEE = 1, SALE_LOSS = 2, BOUNTY_COST = 3, KINDS = 4;

    /** Units bought together and settled on one day. {@code weight}: principal x closed days in the current week. */
    public static final class Lot {
        public long units, principal, settled;
        /** The market day the lot is paid out (-1 = not booked). */
        public long redeemDay = -1;
        public long weight;
    }

    public static final class Investor {
        public String name = "";
        /** Units paid for that become a lot at the next market day. */
        public long pending;
        public final List<Lot> lots = new ArrayList<>();
        /** Dividends received and losses taken, in total. */
        public long paid, lost;
        /** {market week, coins (+ dividend, - loss)} of the last {@link #HISTORY} weeks with a result. */
        public final ArrayDeque<long[]> history = new ArrayDeque<>();

        public long units() { long s = 0; for (Lot l : lots) s += l.units; return s; }

        public long principal() { long s = 0; for (Lot l : lots) s = WealthRules.add(s, l.principal); return s; }

        public long weight() { long s = 0; for (Lot l : lots) s = WealthRules.add(s, l.weight); return s; }

        /** Units not booked for redemption. */
        public long free() { long s = 0; for (Lot l : lots) if (l.redeemDay < 0) s += l.units; return s; }

        public boolean empty() { return pending == 0 && lots.isEmpty(); }
    }

    /** One distributed week of the fund. */
    public record Week(long week, long profit, long loss, long invested, long stockCost, int sharePpm, long dividends, long losses, long kept) { }

    /** The state of the fund. */
    public static final class Fund {
        public final Map<UUID, Investor> investors = new LinkedHashMap<>();
        /** The realised profit and loss of the treasury per market week (the current and the last ones; {@link #KINDS}). */
        public final TreeMap<Long, double[]> pl = new TreeMap<>();
        /** The market week of the lots' weights and the closed days counted in them. */
        public long weightWeek = Long.MIN_VALUE;
        public int weightDays;
        public final ArrayDeque<Week> history = new ArrayDeque<>();

        public Investor investor(UUID id) { return investors.computeIfAbsent(id, k -> new Investor()); }

        /** Settled principal of all investors. */
        public long invested() { long s = 0; for (Investor i : investors.values()) s = WealthRules.add(s, i.principal()); return s; }

        /** Units of all investors waiting for the next market day. */
        public long pending() { long s = 0; for (Investor i : investors.values()) s += i.pending; return s; }
    }

    // ------------------------------------------------------------------ realised profit and loss of the treasury

    /** Adds {@code coins} (positive, finite) of {@code kind} to the market week of {@code day}; keeps the last three weeks. */
    public static void record(Fund f, long day, int kind, double coins) {
        if (kind < 0 || kind >= KINDS || !(coins > 0) || !Double.isFinite(coins)) return;
        long w = BountyMath.week(day);
        f.pl.computeIfAbsent(w, k -> new double[KINDS])[kind] += coins;
        while (f.pl.size() > 3) f.pl.pollFirstEntry();
    }

    /** The profit or loss of stock sold for {@code revenue} that cost {@code cost}: {kind, coins}. */
    public static double[] sale(double revenue, double cost) {
        double r = Math.max(0, revenue), c = Math.max(0, cost);
        return r >= c ? new double[] {SALE_PROFIT, r - c} : new double[] {SALE_LOSS, c - r};
    }

    /**
     * The parts of a purchase from the market of {@code take} items for {@code cost} coins: {@code fromStock} items of stock
     * (worth {@code stockBase} at the price), the rest imported (worth {@code importBase} at the price, {@code importCost} with
     * the import premium). What the bounty floor added above both is shared by the items. Returns {revenue of the stock items,
     * the import premium}.
     */
    public static double[] saleSplit(long cost, long stockBase, long importBase, long importCost, long fromStock, long take) {
        if (take <= 0 || cost <= 0) return new double[] {0, 0};
        double uplift = Math.max(0, cost - (double) stockBase - importCost);
        double stockRevenue = stockBase + uplift * fromStock / take;
        double importRevenue = Math.max(0, cost - stockRevenue);
        return new double[] {stockRevenue, Math.max(0, importRevenue - importBase)};
    }

    /** Profit (sales above cost, import premium) and loss (sales below cost, bounty rewards) of a week's record. */
    public static double profit(double[] pl) { return pl == null ? 0 : pl[SALE_PROFIT] + pl[IMPORT_FEE]; }

    public static double loss(double[] pl) { return pl == null ? 0 : pl[SALE_LOSS] + pl[BOUNTY_COST]; }

    // ------------------------------------------------------------------ the investors' share

    /** The investors' share of a week: average invested / (average invested + stock cost), at most {@link #MAX_SHARE}. */
    public static double share(long weights, int days, double stockCost) {
        if (weights <= 0 || days <= 0) return 0;
        double invested = weights / (double) days;
        double s = invested / (invested + Math.max(0, stockCost));
        return Math.min(MAX_SHARE, s);
    }

    /** The result of a distribution: the week and the coins per investor (dividends to pay, losses already taken). */
    public record Distribution(Week week, Map<UUID, Long> dividends, Map<UUID, Long> losses) { }

    /**
     * Distributes the realised profit or loss of {@code week} (its record is removed) by the lots' weights: dividends are
     * returned for the caller to pay, losses are taken from the lots at once. Resets the weights for the next week. With no
     * weights (no investor) nothing is distributed; the week is still recorded when it had a result.
     */
    public static Distribution distribute(Fund f, long week, double stockCost) {
        double[] pl = f.pl.remove(week);
        double profit = profit(pl), loss = loss(pl);
        long net = Math.round(profit - loss);
        List<UUID> ids = new ArrayList<>();
        List<Long> w = new ArrayList<>();
        long weights = 0;
        for (Map.Entry<UUID, Investor> e : f.investors.entrySet()) {
            long x = e.getValue().weight();
            if (x <= 0) continue;
            ids.add(e.getKey());
            w.add(x);
            weights = WealthRules.add(weights, x);
        }
        int days = Math.max(1, f.weightDays);
        double share = share(weights, days, stockCost);
        long dividends = 0, losses = 0, kept = Math.max(0, net);
        Map<UUID, Long> paid = new LinkedHashMap<>(), taken = new LinkedHashMap<>();
        long[] weightArr = new long[w.size()];
        for (int i = 0; i < weightArr.length; i++) weightArr[i] = w.get(i);
        if (net > 0 && share > 0) {
            long pool = (long) Math.floor(net * share * PAYOUT);
            long[] parts = BountyMath.apportion(pool, weightArr);
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] <= 0) continue;
                Investor inv = f.investors.get(ids.get(i));
                inv.paid = WealthRules.add(inv.paid, parts[i]);
                history(inv, week, parts[i]);
                paid.put(ids.get(i), parts[i]);
                dividends += parts[i];
            }
            kept = net - dividends;
        } else if (net < 0 && share > 0) {
            long pool = (long) Math.ceil(-net * share);
            long[] parts = BountyMath.apportion(pool, weightArr);
            for (int i = 0; i < parts.length; i++) {
                Investor inv = f.investors.get(ids.get(i));
                long take = takeLoss(inv, parts[i]);
                if (take <= 0) continue;
                inv.lost = WealthRules.add(inv.lost, take);
                history(inv, week, -take);
                taken.put(ids.get(i), take);
                losses += take;
            }
            kept = 0;
        }
        for (Investor inv : f.investors.values()) for (Lot l : inv.lots) l.weight = 0;
        long invested = Math.round(weights / (double) days);
        Week result = new Week(week, Math.round(profit), Math.round(loss), invested, Math.round(Math.max(0, stockCost)),
            (int) Math.round(share * 1_000_000), dividends, losses, kept);
        if (pl != null || weights > 0) {
            f.history.addLast(result);
            while (f.history.size() > HISTORY) f.history.removeFirst();
        }
        f.weightDays = 0;
        return new Distribution(result, paid, taken);
    }

    /** Takes {@code coins} (at most the principal) from the investor's lots by their principal; returns what was taken. */
    static long takeLoss(Investor inv, long coins) {
        long have = inv.principal();
        long take = Math.min(Math.max(0, coins), have);
        if (take <= 0) return 0;
        long[] weights = new long[inv.lots.size()];
        for (int i = 0; i < weights.length; i++) weights[i] = inv.lots.get(i).principal;
        long[] parts = BountyMath.apportion(take, weights);
        for (int i = 0; i < parts.length; i++) inv.lots.get(i).principal -= Math.min(parts[i], inv.lots.get(i).principal);
        return take;
    }

    static void history(Investor inv, long week, long coins) {
        inv.history.addLast(new long[] {week, coins});
        while (inv.history.size() > HISTORY) inv.history.removeFirst();
    }

    // ------------------------------------------------------------------ the market day

    /** Adds the closed day {@code closing} to the weights of the lots settled by then (a new week starts from zero). */
    public static void addDay(Fund f, long closing) {
        long w = BountyMath.week(closing);
        if (f.weightWeek != w) {
            for (Investor inv : f.investors.values()) for (Lot l : inv.lots) l.weight = 0;
            f.weightWeek = w;
            f.weightDays = 0;
        }
        for (Investor inv : f.investors.values())
            for (Lot l : inv.lots) if (l.settled <= closing) l.weight = WealthRules.add(l.weight, l.principal);
        f.weightDays++;
    }

    /** The pending units of every investor become lots settled on {@code day} (one lot a day). */
    public static void settle(Fund f, long day) {
        for (Investor inv : f.investors.values()) {
            if (inv.pending <= 0) continue;
            Lot same = null;
            for (Lot l : inv.lots) if (l.settled == day && l.redeemDay < 0) same = l;
            if (same == null) {
                same = new Lot();
                same.settled = day;
                inv.lots.add(same);
            }
            same.units += inv.pending;
            same.principal = WealthRules.add(same.principal, Math.multiplyExact(inv.pending, UNIT));
            inv.pending = 0;
        }
    }

    /** The lots due for payment on {@code day}, removed from their investors: {investor, lot} pairs in order. */
    public static List<Object[]> due(Fund f, long day) {
        List<Object[]> out = new ArrayList<>();
        for (Map.Entry<UUID, Investor> e : f.investors.entrySet()) {
            for (java.util.Iterator<Lot> it = e.getValue().lots.iterator(); it.hasNext(); ) {
                Lot l = it.next();
                if (l.redeemDay >= 0 && l.redeemDay <= day) {
                    out.add(new Object[] {e.getKey(), l});
                    it.remove();
                }
            }
        }
        return out;
    }

    /** The fee of a payment of {@code coins} (1 %, rounded up). */
    public static long fee(long coins) {
        if (coins <= 0) return 0;
        return Math.addExact(Math.multiplyExact(coins / 1000, FEE_PERMILLE), (coins % 1000 * FEE_PERMILLE + 999) / 1000);
    }

    /** The day a lot settled on {@code settled} and booked on {@code today} is paid out. */
    public static long payDay(long settled, long today) {
        return Math.max(today + NOTICE_DAYS, settled + MIN_HOLD_DAYS);
    }

    /**
     * Books {@code units} of the investor's free units for redemption, the oldest lots first (a lot is split when only part of
     * it is booked). Returns the last pay day booked, or -1 when the investor has fewer free units or the lots would exceed
     * {@link #MAX_LOTS} with the lot the waiting units become (nothing is booked then).
     */
    public static long book(Investor inv, long units, long today) {
        if (units <= 0 || inv.free() < units) return -1;
        int splits = 0;
        long left = units;
        List<Lot> order = new ArrayList<>(inv.lots);
        order.sort((a, b) -> Long.compare(a.settled, b.settled));
        for (Lot l : order) {
            if (left <= 0) break;
            if (l.redeemDay >= 0) continue;
            if (l.units > left) splits++;
            left -= Math.min(left, l.units);
        }
        // the units waiting for the next market day become a lot of their own then
        if (inv.lots.size() + splits + (inv.pending > 0 ? 1 : 0) > MAX_LOTS) return -1;
        left = units;
        long last = -1;
        for (Lot l : order) {
            if (left <= 0) break;
            if (l.redeemDay >= 0) continue;
            if (l.units > left) {
                Lot rest = new Lot();
                rest.settled = l.settled;
                rest.units = l.units - left;
                rest.principal = l.principal - l.principal * left / l.units;
                rest.weight = l.weight - l.weight * left / l.units;
                l.principal -= rest.principal;
                l.weight -= rest.weight;
                l.units = left;
                inv.lots.add(inv.lots.indexOf(l) + 1, rest);
            }
            l.redeemDay = payDay(l.settled, today);
            last = Math.max(last, l.redeemDay);
            left -= l.units;
        }
        return last;
    }

    /** Removes investors with nothing left (no lot, nothing pending, no history newer than {@link #HISTORY} weeks). */
    public static void prune(Fund f, long week) {
        f.investors.values().removeIf(i -> i.empty() && (i.history.isEmpty() || i.history.peekLast()[0] < week - HISTORY));
    }
}
