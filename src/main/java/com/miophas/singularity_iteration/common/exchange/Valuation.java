// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 0.1.10.21 (futures M3): the fundamental value of items and the robust statistics of the price discovery. Pure.
 *
 * <ul>
 *   <li><b>Calibration</b>: coins per bucket of UU-matter is the median of {@code base price / UU value} over the
 *       datapack commodities that have both. The curated commodity list of the pack sets the exchange rate between
 *       coins and the UU recipe valuation, so no item needs a hand-written price.</li>
 *   <li><b>Band</b>: a player listing trades only between {@link #BAND_LOW} and {@link #BAND_HIGH} times its
 *       fundamental value (UU value x coins per bucket). A group of players cannot list a cobblestone at a diamond's
 *       price.</li>
 *   <li><b>Robust price</b>: the median of the values within 3 MADs of the median (outliers out).</li>
 * </ul>
 */
public final class Valuation {
    private Valuation() {}

    public static final double BAND_LOW = 0.5, BAND_HIGH = 3.0;

    /** Coins per bucket of UU from (base price, UU buckets) pairs; 0 without a usable pair. */
    public static double coinsPerBucket(List<double[]> pricedPairs) {
        List<Double> ratios = new ArrayList<>();
        for (double[] pair : pricedPairs) if (pair[0] > 0 && pair[1] > 0 && Double.isFinite(pair[0] / pair[1])) ratios.add(pair[0] / pair[1]);
        return ratios.isEmpty() ? 0 : median(ratios);
    }

    /** Fundamental value in coins, 0 when unknown. */
    public static double fundamental(double uuBuckets, double coinsPerBucket) {
        return uuBuckets > 0 && coinsPerBucket > 0 ? uuBuckets * coinsPerBucket : 0;
    }

    /** {@code price} inside the band of {@code fundamental} (unchanged when the fundamental value is unknown). */
    public static long clip(long price, double fundamental) {
        if (fundamental <= 0) return Math.max(1, price);
        long low = Math.max(1, Math.round(fundamental * BAND_LOW)), high = Math.max(low, Math.round(fundamental * BAND_HIGH));
        return Math.max(low, Math.min(high, price));
    }

    /** The median of the values within 3 MADs of the median; 0 for no values. */
    public static long robustPrice(List<Long> values) {
        if (values.isEmpty()) return 0;
        List<Double> v = new ArrayList<>();
        for (long x : values) v.add((double) x);
        double m = median(v);
        List<Double> dev = new ArrayList<>();
        for (double x : v) dev.add(Math.abs(x - m));
        double mad = median(dev);
        List<Double> kept = new ArrayList<>();
        for (double x : v) if (mad == 0 ? x == m : Math.abs(x - m) <= 3 * mad) kept.add(x);
        return Math.round(median(kept.isEmpty() ? v : kept));
    }

    static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
