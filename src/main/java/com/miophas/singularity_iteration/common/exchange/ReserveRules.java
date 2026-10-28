// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.35 (item 4): the treasury's coin reserve (pure; the offline harness runs it).
 *
 * <p>Fees were destroyed: the exchange fee of trades between players, the posting fee of commissions, the cost of a bounty
 * refresh. Now they go into a reserve of coins outside circulation, up to a cap (what is above the cap is destroyed as
 * before), and the coin rewards of bounties come from the reserve first: only the rest is new coins. In circulation nothing
 * changes (a fee leaves circulation and a bounty brings coins back either way); the reserve shows how much of the bounty
 * coins are recycled fees and pays later uses (bond interest, prizes).
 *
 * <p>Cap: {@code weeks x 7 x NORMAL postings a day for the active players x the NORMAL value x the multiplier}, the NORMAL
 * bounty coins of {@code weeks} market weeks at the most (one player, defaults: 4 x 7 x 6 x 4,000 x 1.15 = 772,800).
 * {@code weeks} 0: no reserve, every fee is destroyed (0.1.10.34).
 */
public final class ReserveRules {
    private ReserveRules() {}

    /** The cap of the reserve. */
    public static long cap(int weeks, int postsPerDay, long normalValue, double multiplier) {
        if (weeks <= 0 || postsPerDay <= 0 || normalValue <= 0 || !(multiplier > 0)) return 0;
        double c = (double) weeks * 7 * postsPerDay * normalValue * multiplier;
        // (a hair above the product: 4 x 7 x 6 x 4,000 x 1.15 is 772,799.99... in doubles)
        return c >= Long.MAX_VALUE / 2.0 ? Long.MAX_VALUE / 2 : (long) Math.floor(c * (1 + 1.0E-12));
    }

    /** A fee of {@code coins} into a reserve of {@code reserve} under {@code cap}: {kept in the reserve, destroyed}. */
    public static long[] deposit(long reserve, long coins, long cap) {
        if (coins <= 0) return new long[] {0, 0};
        long room = Math.max(0, cap - Math.max(0, reserve));
        long kept = Math.min(room, coins);
        return new long[] {kept, coins - kept};
    }

    /** Coins of a payment of {@code coins} that a reserve of {@code reserve} pays (the rest is new coins). */
    public static long draw(long reserve, long coins) {
        if (coins <= 0 || reserve <= 0) return 0;
        return Math.min(reserve, coins);
    }

    /** The change of the coins in circulation of a day: new coins and the reserve's payments in, destroyed and the fees kept out. */
    public static long circulation(long minted, long burned, long reserveIn, long reserveOut) {
        return minted + reserveOut - burned - reserveIn;
    }
}
