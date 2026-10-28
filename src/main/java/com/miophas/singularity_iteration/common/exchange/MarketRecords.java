// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.1.10.33: records kept from now on for features of later versions and not shown yet (pure; {@link ExchangeData} saves them):
 *
 * <ul>
 *   <li><b>Player results</b> per market week (this week, last week) and in total: coins received from and paid to the market
 *       and other players, the realised profit or loss of futures, the value delivered to bounties. The wealth ranking ranks
 *       by results, not volume, so these are what it needs from its first week on.</li>
 *   <li><b>Cost basis of the treasury stock</b>: the coins the market paid for the items it holds. Items enter with what was
 *       paid for them (sales to the market, the auction, bounty deliveries at their value); items that leave take their share
 *       of the cost (average cost); items that came back without a price (reserved bounty rewards returned at the expiry) are
 *       valued at the market's bid at the day change. A treasury fund needs the realised profit of the stock.</li>
 * </ul>
 */
public final class MarketRecords {
    /** The fields of a player's results. */
    public static final int TRADE_IN = 0, TRADE_OUT = 1, FUTURES = 2, BOUNTY = 3, FIELDS = 4;
    /** At most this many players are recorded (a bound for a damaged save). */
    public static final int MAX_PLAYERS = 65_536;

    /** The market week of {@link #thisWeek}. */
    public long week = Long.MIN_VALUE;
    public final Map<UUID, long[]> thisWeek = new HashMap<>();
    public final Map<UUID, long[]> lastWeek = new HashMap<>();
    public final Map<UUID, long[]> total = new HashMap<>();
    /** Commodity id to {coins paid for the stock, the stock that cost refers to}. */
    public final Map<String, double[]> cost = new HashMap<>();

    // ------------------------------------------------------------------ player results

    /** Starts a new week when {@code day} is in a later market week (this week becomes last week). */
    public void rollWeek(long day) {
        long w = BountyMath.week(day);
        if (w == week) return;
        lastWeek.clear();
        if (week != Long.MIN_VALUE && w == week + 1) lastWeek.putAll(thisWeek);
        thisWeek.clear();
        week = w;
    }

    /** Adds {@code amount} to a field of {@code player}'s results this week and in total. */
    public void add(UUID player, long day, int field, long amount) {
        if (player == null || amount == 0 || field < 0 || field >= FIELDS) return;
        rollWeek(day);
        if (!total.containsKey(player) && total.size() >= MAX_PLAYERS) return;
        thisWeek.computeIfAbsent(player, p -> new long[FIELDS])[field] += amount;
        total.computeIfAbsent(player, p -> new long[FIELDS])[field] += amount;
    }

    /** A player's results this week (zeros when none; {@code day} rolls the week first). */
    public long[] week(UUID player, long day) {
        rollWeek(day);
        long[] v = thisWeek.get(player);
        return v == null ? new long[FIELDS] : v.clone();
    }

    // ------------------------------------------------------------------ cost basis of the treasury

    /**
     * {@code items} entered the treasury of {@code id} for {@code coins}; {@code stockBefore} is the stock before they came. A
     * change of the stock since the last record is settled first (fewer: at the average cost; more: at {@code coins / items}).
     */
    public void costIn(String id, long stockBefore, long items, double coins) {
        if (items <= 0) return;
        double unit = Math.max(0, coins) / items;
        double[] c = reconcile(cost.get(id), stockBefore, unit);
        c[0] += Math.max(0, coins);
        c[1] += items;
        cost.put(id, c);
    }

    /** Settles the stock of {@code id} as it is now ({@code unitPrice}: the value of items that came without a price). */
    public void reconcile(String id, long stock, double unitPrice) {
        double[] c = reconcile(cost.get(id), stock, unitPrice);
        if (c[1] <= 0) cost.remove(id); else cost.put(id, c);
    }

    /** The cost record after the stock became {@code stock}: fewer items take their share of the cost; more come at {@code unit}. */
    public static double[] reconcile(double[] c, long stock, double unit) {
        double[] x = c == null ? new double[] {0, 0} : c.clone();
        long s = Math.max(0, stock);
        if (s < x[1]) {
            x[0] = x[1] <= 0 ? 0 : x[0] * s / x[1];
            x[1] = s;
        } else if (s > x[1]) {
            x[0] += (s - x[1]) * Math.max(0, unit);
            x[1] = s;
        }
        return x;
    }

    /** The coins paid for the stock of {@code id} now (0 without a record). */
    public double costOf(String id) {
        double[] c = cost.get(id);
        return c == null ? 0 : c[0];
    }
}
