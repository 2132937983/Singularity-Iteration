// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.math.BigInteger;
import java.util.SplittableRandom;

/**
 * 0.1.10.26 (X3): the arithmetic of the system bounties (pure Java; the offline harness runs it).
 * <ul>
 *   <li>Value targeting: a request of the NORMAL tier asks for {@code round(value / ask)} items within a band, a reward in
 *       items is {@code round(value × multiplier / unit)} items.</li>
 *   <li>Payment: a reward line is paid in proportion to the delivered value ({@link #due}); a contribution split uses the
 *       largest remainder ({@link #apportion}).</li>
 *   <li>Fairness: the per-player share of the ADVANCED tier ({@link #room}, {@link #capped}).</li>
 *   <li>Budgets per market week ({@link #week}, {@link #left}) and the posting rhythm ({@link #normalTarget},
 *       {@link #advancedTarget}, {@link #advancedDue}).</li>
 * </ul>
 */
public final class BountyMath {
    private BountyMath() {}

    /** Market days in a week. */
    public static final int WEEK = 7;

    /** Market week of a market day. */
    public static long week(long day) { return Math.floorDiv(day, WEEK); }

    /** Items of {@code unit} coins each worth {@code value × multiplier}, to the nearest item (0 below half an item). */
    public static long rewardCount(double value, double multiplier, double unit) {
        if (!(value > 0) || !(multiplier > 0) || !(unit > 0)) return 0;
        double n = value * multiplier / unit;
        return n >= Long.MAX_VALUE / 2.0 ? Long.MAX_VALUE / 2 : Math.round(n);
    }

    /** Quantity of a request worth {@code value} at {@code ask} coins an item, or -1 outside {@code min..max}. */
    public static int quantity(double value, double ask, int min, int max) {
        if (!(value > 0) || !(ask > 0)) return -1;
        double q = Math.rint(value / ask);
        return q < min || q > max ? -1 : (int) q;
    }

    /**
     * 0.1.10.33: the quantity of a generated NORMAL request of an item that stacks to {@code stack}: none for an item that does
     * not stack (a request for 39-117 mushroom stews filled the inventory several times); below a stack of 64 at most two
     * stacks (the value is cut to fit, never raised; below {@code min} as before none); else {@link #quantity}.
     */
    public static int generatedQuantity(double value, double ask, int min, int max, int stack) {
        if (stack <= 1) return -1;
        if (stack >= 64) return quantity(value, ask, min, max);
        if (!(value > 0) || !(ask > 0)) return -1;
        int cap = Math.min(max, 2 * stack);
        double q = Math.rint(value / ask);
        if (q < Math.min(min, cap)) return -1;
        return (int) Math.min(q, cap);
    }

    /** 0.1.10.33: the most a datapack table may ask of an item that stacks to {@code stack}: eight inventory slots. */
    public static long tableCount(long count, int stack) {
        return Math.min(count, TABLE_SLOTS * (long) Math.max(1, stack));
    }

    /** 0.1.10.33: inventory slots a table requirement may fill at most. */
    public static final int TABLE_SLOTS = 8;

    /** What is due of {@code total} once {@code done} of {@code all} is delivered, minus what was {@code paid}. */
    public static long due(long total, long done, long all, long paid) {
        if (total <= 0 || all <= 0 || done <= 0) return 0;
        long d = Math.min(done, all);
        long owed = BigInteger.valueOf(total).multiply(BigInteger.valueOf(d)).divide(BigInteger.valueOf(all)).longValueExact();
        return Math.max(0, owed - paid);
    }

    /**
     * Splits {@code total} by {@code weights} with the largest remainder: each gets the floor of its share, the units left
     * go to the largest remainders (ties to the lower index). Weights of 0 get nothing; all 0 gives nothing.
     */
    public static long[] apportion(long total, long[] weights) {
        int n = weights.length;
        long[] out = new long[n];
        BigInteger sum = BigInteger.ZERO;
        for (long w : weights) if (w > 0) sum = sum.add(BigInteger.valueOf(w));
        if (total <= 0 || sum.signum() == 0) return out;
        BigInteger t = BigInteger.valueOf(total);
        BigInteger[] rest = new BigInteger[n];
        long given = 0;
        for (int i = 0; i < n; i++) {
            if (weights[i] <= 0) { rest[i] = BigInteger.valueOf(-1); continue; }
            BigInteger[] qr = t.multiply(BigInteger.valueOf(weights[i])).divideAndRemainder(sum);
            out[i] = qr[0].longValueExact();
            rest[i] = qr[1];
            given += out[i];
        }
        for (long left = total - given; left > 0; left--) {
            int best = -1;
            for (int i = 0; i < n; i++) if (rest[i].signum() >= 0 && (best < 0 || rest[i].compareTo(rest[best]) > 0)) best = i;
            if (best < 0) break;
            out[best]++;
            rest[best] = BigInteger.valueOf(-1);
        }
        return out;
    }

    /** The per-player share applies: in multiplayer, below a share of 1, and not on the last day of the bounty. */
    public static boolean capped(boolean multiplayer, double share, long day, long expiry) {
        return multiplayer && share < 1.0 && expiry - day > 1;
    }

    /** Value a player may still deliver: {@code ceil(all × share) − mine}, or {@link Long#MAX_VALUE} without a cap. */
    public static long room(long all, double share, long mine, boolean capped) {
        if (!capped) return Long.MAX_VALUE;
        return Math.max(0, (long) Math.ceil(all * share) - mine);
    }

    /** What is left of a weekly budget {@code cap} ({@code < 0}: no limit) when {@code used} was counted in {@code usedWeek}. */
    public static long left(long cap, long used, long usedWeek, long week) {
        if (cap < 0) return Long.MAX_VALUE;
        return usedWeek == week ? Math.max(0, cap - used) : cap;
    }

    /** Weighted pick among the available entries with {@code r} in [0, 1); -1 when none is available. */
    public static int pick(int[] weights, boolean[] available, double r) {
        long sum = 0;
        for (int i = 0; i < weights.length; i++) if (available[i] && weights[i] > 0) sum += weights[i];
        if (sum <= 0) return -1;
        long x = (long) Math.floor(Math.max(0, Math.min(0.999999999, r)) * sum);
        for (int i = 0; i < weights.length; i++) {
            if (!available[i] || weights[i] <= 0) continue;
            if (x < weights[i]) return i;
            x -= weights[i];
        }
        return -1;
    }

    /** NORMAL requests to keep open on {@code day}: {@code min..max}, fixed for the day. */
    public static int normalTarget(long day, int min, int max, long seed) {
        if (max <= min) return Math.max(0, min);
        return min + new SplittableRandom(mix(seed, day)).nextInt(max - min + 1);
    }

    /** ADVANCED requests to post in market week {@code week}: {@code min..max}. */
    public static int advancedTarget(long week, int min, int max, long seed) {
        if (max <= min) return Math.max(0, min);
        return min + new SplittableRandom(mix(seed ^ 0x5DEECE66DL, week)).nextInt(max - min + 1);
    }

    /** An ADVANCED request is due on {@code day}: the week's posts are spread over its days (the first on its first day). */
    public static boolean advancedDue(long day, int target, int postedThisWeek) {
        if (postedThisWeek >= target) return false;
        long dow = Math.floorMod(day, WEEK);
        return postedThisWeek <= dow * target / WEEK;
    }

    /** A random number in [0, 1) fixed by {@code seed} and {@code salt}. */
    public static double roll(long seed, long salt) { return new SplittableRandom(mix(seed, salt)).nextDouble(); }

    static long mix(long seed, long v) {
        long z = seed + 0x9E3779B97F4A7C15L * (v + 1);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
