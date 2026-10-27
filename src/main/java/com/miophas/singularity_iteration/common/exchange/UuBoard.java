// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.28 (X4): the coin &lt;-&gt; UU board (proposal B6, Q7). Pure constants and arithmetic (the offline harness runs it).
 *
 * <p>UU-matter is a market of the order book between players: one unit is a lot of {@link #LOT_MB} mB of the UU account, the
 * price is in coins per lot. A sell order holds the lots of the seller's UU account, a buy order the coins at its limit; a
 * trade moves the UU-matter to the buyer's account and the coins, minus the exchange fee (destroyed), to the seller. The
 * market itself never buys or sells UU-matter: a market that bought UU-matter for coins would turn energy into coins without
 * a limit (EU -&gt; UU -&gt; coins). The rate is what players agree on.
 */
public final class UuBoard {
    private UuBoard() {}

    /** The id of the market (not an item; the panel shows the UU-matter bucket). */
    public static final String MARKET = "mio_icif:uu";
    /** mB of UU-matter in one lot. */
    public static final int LOT_MB = 100;
    /** The highest price of a lot (coins). */
    public static final long HIGH = 10_000_000;

    public static boolean is(String id) { return MARKET.equals(id); }

    /** mB of {@code lots} lots. */
    public static long mb(long lots) { return Math.multiplyExact(lots, (long) LOT_MB); }

    /** The coins of a trade of {@code lots} at {@code price}: {value, fee destroyed, coins to the seller}. */
    public static long[] settle(long price, long lots, double fee) {
        long value = Math.multiplyExact(price, lots);
        long cut = (long) Math.ceil(value * fee);
        return new long[] {value, cut, value - cut};
    }

    /** A reference price of a lot from the coin value of a bucket (the calibration), at least 1. */
    public static long reference(double coinsPerBucket) {
        if (!(coinsPerBucket > 0) || !Double.isFinite(coinsPerBucket)) return 1;
        return Math.max(1, Math.min(HIGH, Math.round(coinsPerBucket * LOT_MB / 1000.0)));
    }
}
