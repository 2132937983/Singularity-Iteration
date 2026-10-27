// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.10.25 (X1): what the market pays when players sell it a commodity. Pure arithmetic, no game types.
 *
 * <p>The bid of the next item is {@code unit x exp(-mine / lp) x exp(-pressure / lg)}: {@code unit} is the price minus half
 * the spread, {@code mine} the items this player sold of the commodity today, {@code pressure} the items all players sold
 * that the market has not passed on yet (it falls by the export share each day). A sale of any size is possible; the
 * price falls with it, and one sale can never pay more than {@code unit / (1/lp + 1/lg)} (one player per day) or
 * {@code unit x lg} (all players, any number of accounts). Before 0.1.10.25 each player could trade 100 items a day
 * (buying and selling together, any commodity) and the market bought at most 256 of a commodity a day.
 *
 * <p>The daily auction ({@link #clear}) sells the day's auction orders of a commodity together: every seller sells at the
 * same time, each at the rate of its own quantity, so the order of the orders does not matter. A seller alone gets
 * exactly the payout of an instant sale of the same quantity.
 */
public final class SellDepth {
    private SellDepth() {}

    /** The bid of one commodity: {@code unit} = price x (1 - spread / 2); depths in items. */
    public record Curve(double unit, double lp, double lg) {
        public Curve {
            if (!(lp > 0) || !(lg > 0)) throw new IllegalArgumentException("depths must be positive");
        }

        double k() { return 1.0 / lp + 1.0 / lg; }
    }

    /** The factor of the bid before a sale. */
    public static double start(Curve c, double mine, double pressure) {
        return Math.exp(-Math.max(0, mine) / c.lp() - Math.max(0, pressure) / c.lg());
    }

    /** The bid of the next item. */
    public static double marginal(Curve c, double mine, double pressure) {
        return Math.max(0, c.unit()) * start(c, mine, pressure);
    }

    /** Coins (not rounded) that selling {@code q} items pays now. */
    public static double payout(Curve c, double mine, double pressure, double q) {
        if (q <= 0 || c.unit() <= 0) return 0;
        double k = c.k();
        return c.unit() * start(c, mine, pressure) * -Math.expm1(-k * q) / k;
    }

    /** Coins of an endless sale: the most one sale can pay from this state. */
    public static double ceiling(Curve c, double mine, double pressure) {
        return marginal(c, mine, pressure) / c.k();
    }

    /** The most items (at most {@code upTo}) whose payout, rounded down, stays within {@code coins}. */
    public static long maxForCoins(Curve c, double mine, double pressure, double coins, long upTo) {
        if (upTo <= 0 || coins < 0) return 0;
        if (Math.floor(payout(c, mine, pressure, upTo)) <= coins) return upTo;
        long lo = 0, hi = upTo;   // payout is increasing in q
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (Math.floor(payout(c, mine, pressure, mid)) <= coins) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    /** The most items (at most {@code upTo}) whose average price, rounded down on the total, is at least {@code limit}. */
    public static long maxForAverage(Curve c, double mine, double pressure, double limit, long upTo) {
        if (upTo <= 0) return 0;
        if (limit <= 0) return upTo;
        if (Math.floor(payout(c, mine, pressure, upTo)) >= limit * upTo) return upTo;
        long lo = 0, hi = upTo;   // the average falls with q
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (Math.floor(payout(c, mine, pressure, mid)) >= limit * mid) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    /**
     * The most items (at most {@code upTo}) of a limit sale: every item goes at a bid of at least {@code limit} (the bid
     * at the start of the item). The coins are still the exact {@link #payout}.
     */
    public static long maxForMarginal(Curve c, double mine, double pressure, double limit, long upTo) {
        if (upTo <= 0) return 0;
        if (limit <= 0) return upTo;
        double first = marginal(c, mine, pressure);
        if (first < limit) return 0;
        double last = Math.floor(Math.log(first / limit) / c.k());   // the bid before item j is first x exp(-k j)
        return (long) Math.min(upTo, last + 1);
    }

    /**
     * 0.1.10.29: the price of the {@code j}-th item of a sale from this state ({@code j} from 1): its share of {@link #payout}
     * (the bid between the start and the end of the item). The prices fall by the factor exp(-k) from item to item.
     */
    public static double itemPrice(Curve c, double mine, double pressure, long j) {
        if (j < 1 || c.unit() <= 0) return 0;
        double k = c.k();
        return marginal(c, mine, pressure) * Math.exp(-k * (j - 1)) * -Math.expm1(-k) / k;
    }

    /**
     * 0.1.10.29: the most items (at most {@code upTo}) of a minimum-price sale: every item is paid at least {@code limit}
     * ({@link #itemPrice}), so the average is too. Closed form (the logarithm of the first item price over the limit, in
     * steps of k), checked against rounding on the last and the next item. {@link #maxForMarginal} counted the bid at the start
     * of each item, so the last item could be paid up to about k/2 below the limit.
     */
    public static long maxForItemPrice(Curve c, double mine, double pressure, double limit, long upTo) {
        if (upTo <= 0) return 0;
        if (limit <= 0) return upTo;
        double first = itemPrice(c, mine, pressure, 1);
        if (!(first >= limit)) return 0;
        double n = Math.floor(Math.log(first / limit) / c.k()) + 1;
        long q = n >= upTo ? upTo : (long) Math.max(1, n);
        while (q > 0 && itemPrice(c, mine, pressure, q) < limit) q--;
        while (q < upTo && itemPrice(c, mine, pressure, q + 1) >= limit) q++;
        return q;
    }

    // ------------------------------------------------------------------ daily auction

    /**
     * One auction order: the seller's sales of the commodity today before the auction ({@code mine}), the quantity, the
     * lowest average price the seller accepts ({@code minUnit}, 0 for any) and the coins the seller may still receive
     * from the market today ({@code capCoins}, {@link Double#POSITIVE_INFINITY} for no limit).
     */
    public record Bid(double mine, long quantity, double minUnit, double capCoins) { }

    /** The result of one order: items sold (the rest goes back) and coins paid. */
    public record Fill(long quantity, long coins) { }

    /** Coins of seller {@code i} when all sell at once: {@code q} of its own out of {@code total}, from {@code pressure}. */
    static double simultaneous(Curve c, double pressure, double mine, double q, double total) {
        if (q <= 0 || c.unit() <= 0) return 0;
        double rate = q / c.lp() + total / c.lg();
        return c.unit() * q * start(c, mine, pressure) * -Math.expm1(-rate) / rate;
    }

    /**
     * Clears the auction orders of one commodity. An order whose average price would be below its minimum does not sell
     * (all of it goes back); an order above the seller's coin limit sells only what fits. Each round decides every order
     * from the totals at the start of the round and then applies all changes together, so the result does not depend
     * on the order of the orders. Removing or cutting an order raises the price of the others, so the rounds repeat
     * until nothing changes (quantities only fall: it ends).
     */
    public static List<Fill> clear(Curve c, double pressure, List<Bid> bids) {
        int n = bids.size();
        long[] q = new long[n];
        for (int i = 0; i < n; i++) q[i] = Math.max(0, bids.get(i).quantity());
        for (int round = 0; round < 64; round++) {
            boolean changed = false;
            long total = 0;
            for (long x : q) total += x;
            long[] next = q.clone();
            for (int i = 0; i < n; i++) {
                if (q[i] == 0) continue;
                Bid b = bids.get(i);
                double pay = simultaneous(c, pressure, b.mine(), q[i], total);
                if (Math.floor(pay) < b.minUnit() * q[i]) {
                    next[i] = 0;
                    changed = true;
                } else if (Math.floor(pay) > b.capCoins()) {
                    long others = total - q[i];
                    long lo = 0, hi = q[i] - 1;
                    while (lo < hi) {
                        long mid = lo + (hi - lo + 1) / 2;
                        if (Math.floor(simultaneous(c, pressure, b.mine(), mid, others + mid)) <= b.capCoins()) lo = mid; else hi = mid - 1;
                    }
                    next[i] = lo;
                    changed = true;
                }
            }
            q = next;
            if (!changed) break;
        }
        long total = 0;
        for (long x : q) total += x;
        List<Fill> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Bid b = bids.get(i);
            long coins = (long) Math.floor(simultaneous(c, pressure, b.mine(), q[i], total));
            if (coins > b.capCoins()) coins = (long) Math.floor(b.capCoins());
            out.add(new Fill(q[i], Math.max(0, coins)));
        }
        return out;
    }

    // ------------------------------------------------------------------ daily allowance and export

    /**
     * The coins a player may still receive from the market: {@code balance} on day {@code day}, refilled by
     * {@code perDay} each day up to {@code keepDays} days' worth. A new player starts with one day.
     */
    public static long allowance(long balance, long day, long today, long perDay, int keepDays) {
        if (perDay <= 0) return Long.MAX_VALUE;
        long max = perDay * Math.max(1, keepDays);
        if (today <= day) return Math.min(balance, max);
        long days = Math.min(today - day, Math.max(1, keepDays));
        return Math.min(max, balance + perDay * days);
    }

    /** The pressure after {@code days} days of exporting {@code share} of it each day. */
    public static double export(double pressure, double share, long days) {
        if (pressure <= 0 || days <= 0) return Math.max(0, pressure);
        double keep = Math.pow(1.0 - Math.max(0, Math.min(1, share)), Math.min(days, 10_000));
        double left = pressure * keep;
        return left < 1.0 ? 0 : left;   // less than one item: gone
    }
}
