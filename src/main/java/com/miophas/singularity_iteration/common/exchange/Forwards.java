// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.21 (futures M5): price and payoff of the futures contracts that the machine sells. Pure.
 *
 * <ul>
 *   <li><b>Fair forward price</b> ({@link #fairForward}): the market price after {@code days} that the mean reversion
 *       alone predicts (each day the distance to the base price shrinks by the regression factor: 0.15 above the base,
 *       0.225 below). It is only a reference: the contracts are priced by {@link FuturesPricer}, which simulates the
 *       whole price model (rounding, events); the forward alone left one side a positive expectation.</li>
 *   <li><b>Spread</b> ({@link #contractPrice}): a long contract opens at the break-even price plus half the spread, a
 *       short at minus half.</li>
 *   <li><b>Payoff</b> (cash settlement at the market price of the expiry day): long {@code (S - F) x n}, short
 *       {@code (F - S) x n}, both capped at {@code F x n} either way. The collateral is {@code F x n}: a contract can
 *       never lose more than its collateral, and the market never pays more than one collateral of profit, so a
 *       contract cannot default and the coins it can create are bounded.</li>
 * </ul>
 */
public final class Forwards {
    private Forwards() {}

    public static final double REGRESSION_ABOVE = 0.15, REGRESSION_BELOW = 0.225;

    /** Expected price after {@code days} days from {@code price} (base {@code base}), without the random part. */
    public static double fairForward(double price, double base, int days) {
        double p = price;
        for (int d = 0; d < days; d++) {
            double deviation = (p - base) / base;
            double regression = deviation < 0 ? REGRESSION_BELOW : REGRESSION_ABOVE;
            regression += Math.max(0, Math.abs(deviation) - 0.5) * 0.2;
            regression = Math.min(regression, 0.6);
            p = p * (1 - deviation * regression);
        }
        return p;
    }

    /** The contract price per item: the forward price plus (long) or minus (short) half the spread, at least 1. */
    public static long contractPrice(double forward, boolean isLong, double spread) {
        double s = Math.max(0, Math.min(0.5, spread));
        return Math.max(1, isLong ? (long) Math.ceil(forward * (1 + s / 2)) : (long) Math.floor(forward * (1 - s / 2)));
    }

    /** Collateral of a contract: price x quantity. */
    public static long collateral(long price, int quantity) {
        return Math.multiplyExact(price, (long) quantity);
    }

    /** What the holder gets back at expiry: collateral + payoff, between 0 and twice the collateral. */
    public static long settlement(boolean isLong, long price, int quantity, long settlePrice) {
        long collateral = collateral(price, quantity);
        long payoff = (isLong ? settlePrice - price : price - settlePrice) * quantity;
        payoff = Math.max(-collateral, Math.min(collateral, payoff));
        return collateral + payoff;
    }
}
