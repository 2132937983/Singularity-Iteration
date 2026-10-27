// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.29: the minimum-price sale. Pure arithmetic, no game types (the offline harness runs it; the panels use it for their
 * previews, the server for the sales).
 *
 * <p>The seller names the lowest price per item, in coins or in percent of the bid without pressure ({@link SellDepth.Curve#unit},
 * the reference of the auction minimum). The sale takes the most items Q* whose own price is still at least that minimum
 * ({@link SellDepth#maxForItemPrice}: the bid falls by the same factor from item to item, so Q* is one logarithm, not a walk
 * over the items) and pays the sum of their prices at once, rounded down once ({@link SellDepth#payout}). Every sold item is
 * paid the minimum or more, so the batch pays at least Q* times the minimum; selling the same items one by one pays the
 * same prices but rounds every item down.
 */
public final class MinPrice {
    private MinPrice() {}

    /** The highest percent of a minimum (a minimum above the bid without pressure never sells). */
    public static final int MAX_PERCENT = 100;

    /** The lowest price per item in coins (at least 1): {@code value} coins, or {@code value} % of {@code unit} rounded up. */
    public static long limit(double unit, boolean percent, long value) {
        if (!percent) return Math.max(1, value);
        long p = Math.max(0, Math.min(MAX_PERCENT, value));
        if (p == 0 || !(unit > 0)) return 1;
        return Math.max(1, (long) Math.ceil(unit * p / 100.0 - 1e-9));
    }

    /** The auction minimum (percent of {@code unit}, rounded up, 0..100) of a coin minimum. */
    public static int percentOf(double unit, long coins) {
        if (coins <= 0) return 0;
        if (!(unit > 0)) return MAX_PERCENT;
        return (int) Math.max(0, Math.min(MAX_PERCENT, Math.ceil(coins * 100.0 / unit - 1e-9)));
    }

    /**
     * What an instant sale of up to {@code upTo} items sells at the minimum {@code limit}: the quantity Q*, the coins (rounded
     * down once), the price of the last sold item and of the next one. {@code allowance}: the coins the seller may still
     * receive ({@link Long#MAX_VALUE} for no limit).
     */
    public record Quote(long quantity, long coins, double last, double next) { }

    public static Quote quote(SellDepth.Curve c, double mine, double pressure, long limit, long upTo, long allowance) {
        long q = SellDepth.maxForItemPrice(c, mine, pressure, Math.max(1, limit), Math.max(0, upTo));
        if (allowance != Long.MAX_VALUE) q = SellDepth.maxForCoins(c, mine, pressure, Math.max(0, allowance), q);
        long coins = q <= 0 ? 0 : (long) Math.floor(SellDepth.payout(c, mine, pressure, q));
        return new Quote(q, coins, q > 0 ? SellDepth.itemPrice(c, mine, pressure, q) : 0, SellDepth.itemPrice(c, mine, pressure, q + 1));
    }
}
