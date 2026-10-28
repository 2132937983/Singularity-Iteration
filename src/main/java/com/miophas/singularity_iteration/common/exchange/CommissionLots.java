// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.33: deliveries to a commission with an item or UU-matter reward come in whole reward lots (pure; the client and
 * the offline harness use it too).
 *
 * <p>A delivery pays {@code floor(reward x delivered / quantity)} in total. With a coarse reward, such as "64 redstone for
 * 1 diamond", a player who delivered 63 got nothing and the one who delivered the 64th item got the diamond. Now a
 * delivery is a multiple of the lot {@code quantity / gcd(quantity, reward)} items (64 here; "64 for 4 diamonds": 16), so
 * every delivery is paid exactly; what is left of the commission may always be delivered (it is a whole number of lots,
 * except in a commission partly delivered before 0.1.10.33). Coin rewards keep single items (a coin is the smallest unit).
 */
public final class CommissionLots {
    private CommissionLots() {}

    /** Items of one reward lot: {@code quantity / gcd(quantity, reward)}; 1 for coin rewards or bad values. */
    public static long lot(long quantity, long reward, boolean coins) {
        if (coins || quantity <= 0 || reward <= 0) return 1;
        return quantity / gcd(quantity, reward);
    }

    /** The items a delivery of up to {@code wanted} takes when {@code left} are still needed: whole lots, or all that is left. */
    public static long take(long wanted, long left, long lot) {
        long n = Math.min(wanted, left);
        if (n <= 0) return 0;
        if (n >= left || lot <= 1) return n;
        return n - n % lot;
    }

    /** The reward of one lot: {@code reward x lot / quantity} (an integer by construction). */
    public static long perLot(long quantity, long reward, long lot) {
        if (quantity <= 0) return 0;
        return java.math.BigInteger.valueOf(reward).multiply(java.math.BigInteger.valueOf(lot))
            .divide(java.math.BigInteger.valueOf(quantity)).longValue();
    }

    static long gcd(long a, long b) {
        while (b != 0) { long t = a % b; a = b; b = t; }
        return Math.abs(a);
    }
}
