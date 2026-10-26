// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.Arrays;
import java.util.Random;

/**
 * 0.1.10.21 (futures M5): contract prices from a Monte Carlo run of the market's own price model. Pure.
 *
 * <p>A contract settles at expiry against the market price S: a long at price P gets back {@code min(S, 2P)} per item
 * (the payoff is capped at the collateral), a short {@code 2P - min(S, 2P)}. The market is the counterparty, so a
 * contract with a positive expected payoff would create coins from nothing. The pricer therefore simulates the price
 * over the contract's days with the same rules as the market (the event active now, new events, the random step with
 * its rounding) and finds the break-even price of each side on those paths: the lowest P where a long expects nothing,
 * the highest P where a short expects nothing. Half the spread is then added for the long and taken off for the short.
 * A plain forecast is not enough: the rounding down of every step and the shape of the events (trends that go on,
 * bubbles) gave one side a positive expectation in the 0.1.10.21 harness.
 *
 * <p>The paths come from a fixed seed (per market and day), so the quote does not change while the day lasts.
 */
public final class FuturesPricer {
    private FuturesPricer() {}

    /** One simulated price path; {@code step} counts the days from 0. */
    public interface Path { int next(int price, int step); }

    /** Starts a path (a path can keep state, such as a simulated event) with the shared random source. */
    public interface Model { Path start(Random random); }

    /** Contract prices per item and the mean simulated settlement price. */
    public record Quote(long longPrice, long shortPrice, double mean) { }

    public static Quote quote(int start, int days, Model model, double spread, int paths, long seed) {
        Random random = new Random(seed);
        int n = Math.max(1, paths);
        long[] end = new long[n];
        for (int i = 0; i < n; i++) {
            Path path = model.start(random);
            int p = start;
            for (int d = 0; d < days; d++) p = Math.max(0, path.next(p, d));
            end[i] = p;
        }
        Arrays.sort(end);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + end[i];
        double mean = prefix[n] / (double) n;
        double s = Math.max(0, Math.min(0.5, spread));
        long fairLong = fairLong(end, prefix), fairShort = fairShort(end, prefix);
        long longPrice = Math.max(1, (long) Math.ceil(fairLong * (1 + s / 2)));
        long shortPrice = Math.max(1, (long) Math.floor(fairShort * (1 - s / 2)));
        return new Quote(longPrice, shortPrice, mean);
    }

    /** Mean of {@code min(S, cap)} over the sorted settlement prices. */
    static double cappedMean(long[] end, long[] prefix, long cap) {
        int lo = 0, hi = end.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (end[mid] < cap) lo = mid + 1; else hi = mid;
        }
        return (prefix[lo] + (double) (end.length - lo) * cap) / end.length;
    }

    /** Mean payoff per item of a long at {@code price}: {@code min(S, 2P) - P}. */
    public static double longPayoff(long[] end, long[] prefix, long price) { return cappedMean(end, prefix, 2 * price) - price; }

    /** Mean payoff per item of a short at {@code price}: {@code P - min(S, 2P)}. */
    public static double shortPayoff(long[] end, long[] prefix, long price) { return price - cappedMean(end, prefix, 2 * price); }

    /**
     * The lowest price from half the median up at which a long expects nothing. Over that range the long payoff only
     * falls (its slope is {@code 2 Pr(S > 2P) - 1}), and at the mean it is never positive.
     */
    static long fairLong(long[] end, long[] prefix) {
        long hi = Math.max(1, (long) Math.ceil(prefix[end.length] / (double) end.length));
        long lo = Math.max(1, end[end.length / 2] / 2);
        if (lo > hi) lo = hi;
        while (lo < hi) {
            long mid = (lo + hi) >>> 1;
            if (longPayoff(end, prefix, mid) <= 0) hi = mid; else lo = mid + 1;
        }
        return hi;
    }

    /**
     * The highest price up to the mean at which a short expects nothing. The short payoff is not positive at 1, falls
     * while {@code P} is below half the median and rises after, so the prices where it is not positive form [1, P*].
     */
    static long fairShort(long[] end, long[] prefix) {
        long lo = 1, hi = Math.max(1, (long) Math.floor(prefix[end.length] / (double) end.length));
        if (shortPayoff(end, prefix, hi) <= 0) return hi;
        while (lo < hi) {
            long mid = (lo + hi + 1) >>> 1;
            if (shortPayoff(end, prefix, mid) <= 0) lo = mid; else hi = mid - 1;
        }
        return lo;
    }
}
