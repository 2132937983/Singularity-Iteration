// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.34: the pace of the system bounties (pure; the offline harness runs it): how long a bounty stays open (rarity,
 * effort, rush bonus), how many are open for the players of the server, how fast the board refills during a market day,
 * and what a refresh of the board costs.
 *
 * <ul>
 *   <li><b>Days</b>: a rarity gives a range (common 3-7, uncommon 2-5, rare 1-4, legendary 1-3 days); a large request gets
 *       at least {@code 1 + floor(log2(value / 4,000))} days; the days are drawn in the range. A bounty posted for fewer
 *       than four days pays a rush bonus of 5 % a missing day.</li>
 *   <li><b>Players</b> ({@code n} active: online, or used the exchange in the last three market days): NORMAL requests
 *       open {@code min + ceil(1.5 (n - 1))} (at most 16) up to that plus the width of the configured range; ADVANCED a
 *       week x {@code (1 + 0.5 (n - 1))} (at most 21); SPECIAL open {@code max + floor((n - 1) / 4)} (at most 4); the weekly
 *       issue caps x {@code sqrt(n)}.</li>
 *   <li><b>Refill</b>: below the day's NORMAL target the board posts one request when {@code 6 min / (1 + 0.5 (online -
 *       1))} (at least one minute) passed since the last posting, at most {@code 6 + 4 (n - 1)} NORMAL postings a market
 *       day.</li>
 *   <li><b>Refresh</b>: {@code max(500, 5 %)} of the coin value of the replaced bounties, doubled for each earlier refresh
 *       of the player that market day.</li>
 * </ul>
 */
public final class BountyPacing {
    private BountyPacing() {}

    /** The rarity of a bounty: its range of days. */
    public enum Rarity {
        COMMON(3, 7), UNCOMMON(2, 5), RARE(1, 4), LEGENDARY(1, 3);

        public final int minDays, maxDays;

        Rarity(int minDays, int maxDays) {
            this.minDays = minDays;
            this.maxDays = maxDays;
        }

        /** The rarity of a name ("common", ...), or null. */
        public static Rarity of(String name) {
            if (name == null) return null;
            for (Rarity r : values()) if (r.name().equalsIgnoreCase(name.trim())) return r;
            return null;
        }
    }

    /** Value of a request that gets one day of effort; each doubling adds one. */
    public static final double EFFORT_VALUE = 4000;
    /** Rush bonus a missing day below {@link #RUSH_DAYS} (percent). */
    public static final int RUSH_PERCENT = 5, RUSH_DAYS = 4;

    /**
     * The rarity of a tier and table weight: NORMAL weight 4 and more (and generated requests) common, NORMAL below 4 and
     * ADVANCED 3 and more uncommon, ADVANCED below 3 rare, SPECIAL legendary. {@code tier}: 0 normal, 1 advanced, 2 special.
     */
    public static Rarity rarity(int tier, int weight) {
        return switch (tier) {
            case 0 -> weight >= 4 ? Rarity.COMMON : Rarity.UNCOMMON;
            case 1 -> weight >= 3 ? Rarity.UNCOMMON : Rarity.RARE;
            default -> Rarity.LEGENDARY;
        };
    }

    /** The fewest days of a request worth {@code value} coins: {@code 1 + floor(log2(value / 4,000))}, at least 1. */
    public static int effortDays(double value) {
        if (!(value >= EFFORT_VALUE * 2)) return 1;
        return 1 + (int) Math.floor(Math.log(value / EFFORT_VALUE) / Math.log(2));
    }

    /**
     * The days of a bounty: the range {@code lo..hi} with its low end raised to the effort ({@link #effortDays}, never above
     * {@code hi}), within {@code minDays..maxDays}; drawn with {@code roll} in [0, 1).
     */
    public static int days(int lo, int hi, double value, int minDays, int maxDays, double roll) {
        int max = Math.max(1, Math.max(minDays, maxDays));
        int min = Math.max(1, Math.min(minDays, max));
        int h = Math.max(min, Math.min(max, Math.max(lo, hi)));
        int l = Math.max(min, Math.min(h, Math.max(Math.min(lo, hi), effortDays(value))));
        double r = Math.max(0, Math.min(0.999999999, roll));
        return l + (int) Math.floor(r * (h - l + 1));
    }

    /** The rush bonus of a bounty posted for {@code days} (percent): 5 % a day below four (1 day 15 %), else 0. */
    public static int rushPercent(int days) {
        return days >= RUSH_DAYS || days < 1 ? 0 : RUSH_PERCENT * (RUSH_DAYS - days);
    }

    /** Market days a player counts as active after a login or a use of the exchange (today and the two before). */
    public static final int ACTIVE_DAYS = 3;
    /** Whole market days a claimant may be away before the claim is released. */
    public static final int ABSENT_DAYS = 2;

    /** A player last seen on market day {@code seen} is active on {@code day}. */
    public static boolean active(long seen, long day) {
        return day - seen < ACTIVE_DAYS && seen <= day;
    }

    /** A claim made or last confirmed on market day {@code seen} is released at the start of {@code day}. */
    public static boolean absent(long seen, long day) {
        return day - seen > ABSENT_DAYS;
    }

    /** A claimed bounty with one day left gets one more day (once). */
    public static long claimExpiry(long expiry, long day, boolean extended) {
        return !extended && expiry - day <= 1 ? day + 2 : expiry;
    }

    // ------------------------------------------------------------------ players

    /** Most NORMAL requests a server keeps open (the low end of the range). */
    public static final int NORMAL_CAP = 16;
    /** Most ADVANCED requests a week and SPECIAL ones open. */
    public static final int ADVANCED_CAP = 21, SPECIAL_CAP = 4;
    /** Extra NORMAL requests for each active player after the first. */
    public static final double PLAYER_FACTOR = 1.5;

    /** The NORMAL range for {@code active} players: {@code {lo, hi}} from the configured {@code min..max}. */
    public static int[] normalRange(int min, int max, int active) {
        int n = Math.max(1, active);
        int width = Math.max(0, max - min);
        int lo = (int) Math.min(NORMAL_CAP, min + (long) Math.ceil(PLAYER_FACTOR * (n - 1)));
        lo = Math.max(min, lo);
        return new int[] {lo, lo + width};
    }

    /** ADVANCED requests a week for {@code active} players: {@code weekly x (1 + 0.5 (n - 1))}, rounded, at most 21. */
    public static int advancedWeekly(int weekly, int active) {
        int n = Math.max(1, active);
        if (n == 1) return weekly;
        return (int) Math.min(ADVANCED_CAP, Math.max(weekly, Math.round(weekly * (1 + 0.5 * (n - 1)))));
    }

    /** SPECIAL requests open at most for {@code active} players: {@code max + floor((n - 1) / 4)}, at most 4. */
    public static int specialOpen(int max, int active) {
        int n = Math.max(1, active);
        return Math.max(max, Math.min(SPECIAL_CAP, max + (n - 1) / 4));
    }

    /** A weekly issue cap for {@code active} players: {@code cap x sqrt(n)} ({@code cap < 0}: no limit). */
    public static long issueCap(long cap, int active) {
        if (cap <= 0) return cap;
        int n = Math.max(1, active);
        return (long) Math.floor(cap * Math.sqrt(n));
    }

    // ------------------------------------------------------------------ refill

    /** Ticks of the refill wait with one player online (6 minutes) and the shortest wait (1 minute). */
    public static final long REFILL_TICKS = 7200, REFILL_MIN_TICKS = 1200;
    /** NORMAL postings a market day: 6 with one active player, 4 more for each other. */
    public static final int DAILY_POSTS = 6, DAILY_POSTS_PER_PLAYER = 4;

    /** The wait between two refills with {@code online} players ({@link Long#MAX_VALUE} when nobody is online). */
    public static long refillWait(int online) {
        if (online < 1) return Long.MAX_VALUE;
        return Math.max(REFILL_MIN_TICKS, Math.round(REFILL_TICKS / (1 + 0.5 * (online - 1))));
    }

    /** NORMAL postings allowed in a market day for {@code active} players. */
    public static int dailyPosts(int active) {
        return DAILY_POSTS + DAILY_POSTS_PER_PLAYER * (Math.max(1, active) - 1);
    }

    /** A refill is due: below the target, the wait over since the last posting, the day's postings not used up. */
    public static boolean refillDue(int open, int target, long now, long lastPost, int online, int postedToday, int active) {
        if (open >= target || postedToday >= dailyPosts(active)) return false;
        long wait = refillWait(online);
        return wait != Long.MAX_VALUE && (lastPost == Long.MIN_VALUE || now - lastPost >= wait);
    }

    // ------------------------------------------------------------------ refresh

    /** Least coins of a refresh, its share of the replaced rewards (percent), refreshes a day, the wait between two. */
    public static final long REFRESH_BASE = 500;
    public static final int REFRESH_PERCENT = 5, REFRESH_PER_DAY = 3;
    public static final long REFRESH_WAIT_TICKS = 6000;

    /** The coins of the next refresh after {@code done} this market day of bounties worth {@code value} coins; -1 when none is left. */
    public static long refreshCost(double value, int done) {
        if (done < 0 || done >= REFRESH_PER_DAY) return -1;
        long base = Math.max(REFRESH_BASE, (long) Math.ceil(Math.max(0, value) * REFRESH_PERCENT / 100.0));
        return Math.multiplyExact(base, 1L << done);
    }
}
