// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * 0.1.10.35: the market's bid of a made item never pays more than its inputs cost at the market (pure; the offline harness
 * runs it).
 *
 * <p>A commodity may name what one item of it is made from (datapack {@code made_from}: item to count per item, e.g. refined
 * iron from one iron ingot). Its bid is then at most the sum of {@code count x ask} of the inputs now, the ask being the
 * treasury price {@code price x (1 + spread / 2)} (an import costs more). The daily prices of the commodities move
 * independently, so a fixed {@code bid_price} alone is under the inputs on most days and over them on some: buying the
 * inputs, making the item and selling it at once paid on those days. With the cap it never does.
 */
public final class BidLinks {
    private BidLinks() {}

    /** No cap (no inputs, or an input the market does not quote). */
    public static final double NONE = Double.POSITIVE_INFINITY;

    /**
     * The cap of the bid of one item: the inputs at their asks ({@code askUnit} of an input; 0 or less = not quoted, then
     * {@link #NONE}).
     */
    public static double cap(Map<String, Double> inputs, ToDoubleFunction<String> askUnit) {
        if (inputs == null || inputs.isEmpty()) return NONE;
        double sum = 0;
        for (Map.Entry<String, Double> e : inputs.entrySet()) {
            double count = e.getValue() == null ? 0 : e.getValue();
            if (!(count > 0)) continue;
            double ask = askUnit.applyAsDouble(e.getKey());
            if (!(ask > 0) || !Double.isFinite(ask)) return NONE;
            sum += count * ask;
        }
        return sum > 0 ? sum : NONE;
    }

    /** The bid of one item under the cap. */
    public static double bid(double bidUnit, double cap) {
        if (!(bidUnit > 0)) return 0;
        return cap < bidUnit ? Math.max(0, cap) : bidUnit;
    }

    /** The ask of one item at {@code price} before rounding: {@code price x (1 + spread / 2)}. */
    public static double askUnit(double price, double spread) {
        if (!(price > 0)) return 0;
        double s = Double.isFinite(spread) ? Math.max(0, Math.min(0.5, spread)) : 0;
        return price * (1 + s / 2);
    }
}
