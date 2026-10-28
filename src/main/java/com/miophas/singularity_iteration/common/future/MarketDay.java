// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.future;

/**
 * 0.1.10.33: the market day never goes back (pure; the offline harness runs it).
 *
 * <p>Before, the market day was the overworld's day ({@code dayTime / 24000}), so {@code /time set day} moved it back to 0:
 * a bounty posted on day 290 showed 293 days left, and orders, commissions, futures, the sell allowance and the bounty
 * cooldowns stopped until the world reached day 290 again. Now the market keeps its own day and only follows the
 * <i>changes</i> of the world's day: forward by the difference, and by one day when the world's day went back (setting the
 * time to the morning skipped the night: a day passed).
 *
 * <p>A world whose day went back under an older version is repaired once at the first check after loading
 * ({@link #repaired}): the latest day stamp kept in the exchange (bounties, orders, positions, ledger, ...) is a day the
 * market already lived, so the market day moves up to it.
 */
public final class MarketDay {
    private MarketDay() {}

    /** No world day seen yet (a save of an older version, or a new market). */
    public static final long UNSEEN = Long.MIN_VALUE;

    /** The market day after the world's day changed from {@code rawSeen} to {@code rawNow}. */
    public static long next(long market, long rawSeen, long rawNow) {
        if (rawSeen == UNSEEN || rawNow == rawSeen) return market;
        return rawNow > rawSeen ? market + (rawNow - rawSeen) : market + 1;
    }

    /** The world's day went back since it was last seen. */
    public static boolean wentBack(long rawSeen, long rawNow) {
        return rawSeen != UNSEEN && rawNow < rawSeen;
    }

    /** The market day after the repair: the latest stamp when it is later than the market day ({@link #UNSEEN}: none). */
    public static long repaired(long market, long latestStamp) {
        return latestStamp == UNSEEN ? market : Math.max(market, latestStamp);
    }

    /**
     * The shift of a record made after the world's day went back (its start stamp at or before the old market day
     * {@code oldDay}): it keeps its age and its remaining days on the repaired day {@code newDay}. Records of the old
     * timeline (started later than {@code oldDay}) keep their stamps.
     */
    public static long shift(long stamp, long start, long oldDay, long newDay) {
        if (newDay <= oldDay || start == UNSEEN || start > oldDay) return stamp;
        return stamp == UNSEEN ? stamp : stamp + (newDay - oldDay);
    }
}
