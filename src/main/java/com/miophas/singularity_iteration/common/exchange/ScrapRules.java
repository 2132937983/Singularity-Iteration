// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.28 (X4): the scrap sink of a large sale (proposal A5). Pure arithmetic, no game types (the offline harness runs it).
 *
 * <p>With the sink, an instant sale first pays coins exactly as without it (every item whose bid is at least one coin, within
 * the allowance). The items it leaves are recycled instead when the market pays almost nothing for them: when the next bid is
 * below the floor, {@code max(1 coin, threshold x the bid without pressure)}. They go at the rate of the SI recycler (the
 * recipe's product and chance, else scrap with the IC2 chance of 1/8), paid as the expectation without chance:
 * {@code k = floor(n x chance)} products for {@code ceil(k / chance)} items; the remainder stays with the seller.
 *
 * <p>So the sink creates no coins (the coins of a sale with and without it are the same; Harness1128 rejected a variant that
 * paid coins only down to the floor: a seller could sell without the sink first and then with it, so the floor took nothing
 * and only confused) and no item beyond the recycler's own rate. A sale stopped by the allowance while the bid is still above
 * the floor gives no scrap: those items are worth coins tomorrow.
 */
public final class ScrapRules {
    private ScrapRules() {}

    /** The IC2 recycler's chance of scrap per item. */
    public static final double RECYCLER_CHANCE = 0.125;

    /** The floor (coins) below which unsold items may go to scrap; 0 when the sink is off (threshold 0). */
    public static double floor(double unit, double threshold) {
        if (!(threshold > 0) || !(unit > 0)) return 0;
        return Math.max(1.0, threshold * unit);
    }

    /** True when the unsold items of a sale may go to scrap: the sink is on and the next bid is below the floor. */
    public static boolean eligible(double nextBid, double floor) {
        return floor > 0 && nextBid < floor;
    }

    /** Products for {@code n} items at {@code chance}: {products, items consumed}. */
    public static long[] yield(long n, double chance) {
        if (n <= 0 || !(chance > 0)) return new long[] {0, 0};
        double c = Math.min(1.0, chance);
        long k = (long) Math.floor(n * c + 1e-9);
        if (k <= 0) return new long[] {0, 0};
        long used = Math.min(n, (long) Math.ceil(k / c - 1e-9));
        return new long[] {k, used};
    }
}
