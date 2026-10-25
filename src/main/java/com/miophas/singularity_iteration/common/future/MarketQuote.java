// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.future;

/**
 * 0.1.10.20 (M1): the two prices of the futures market. Pure arithmetic, no Minecraft classes.
 *
 * <ul>
 *   <li><b>Buy</b> (the player buys from the market): the price plus half the spread, rounded up on the
 *       total of the trade.</li>
 *   <li><b>Sell</b> (the market buys from the player): the price minus half the spread. Each commodity has
 *       a daily buy budget for all players together; while it is used up, the price of each further item
 *       falls linearly, by {@code drop} at the end of the budget. A sale above the budget is refused. The
 *       total is rounded down.</li>
 * </ul>
 * Before 0.1.10.20 both prices were the market price and the market bought without limit: buying on a low
 * day and selling after the mean reversion made coins without risk, and a farm could sell without end.
 */
public final class MarketQuote {
    private MarketQuote() {}

    /** No buy budget (the market buys without limit). */
    public static final int UNLIMITED = 0;

    /** Coins that buying {@code quantity} items at {@code price} costs; 0 for a quantity or price below 1. */
    public static long buyCost(int price, int quantity, double spread) {
        if (price < 1 || quantity < 1) return 0;
        double total = (double) price * quantity * (1.0 + clampSpread(spread) / 2.0);
        return (long) Math.ceil(total - 1.0E-9);
    }

    /**
     * Coins that selling {@code quantity} items at {@code price} pays, or -1 when the sale does not fit in the
     * rest of the daily budget.
     *
     * @param bought items of this commodity that the market already bought today
     * @param budget the daily buy budget in items, {@link #UNLIMITED} for no limit
     * @param drop   the fall of the price at the end of the budget (0 to 0.9)
     */
    public static long sellPayout(int price, int quantity, double spread, int bought, int budget, double drop) {
        if (price < 1 || quantity < 1) return 0;
        double base = (double) price * (1.0 - clampSpread(spread) / 2.0);
        if (budget == UNLIMITED) return (long) Math.floor(base * quantity + 1.0E-9);
        if (bought < 0 || (long) bought + quantity > budget) return -1;
        double d = Math.max(0.0, Math.min(0.9, drop));
        // item i of the sale (0-based) is at budget position bought + i + 0.5: sum = q * bought + q^2 / 2
        double used = ((double) quantity * bought + (double) quantity * quantity / 2.0) / budget;
        return (long) Math.floor(base * (quantity - d * used) + 1.0E-9);
    }

    /** Items the market still buys today: the rest of the budget, {@link Integer#MAX_VALUE} without a budget. */
    public static int budgetLeft(int bought, int budget) {
        return budget == UNLIMITED ? Integer.MAX_VALUE : Math.max(0, budget - Math.max(0, bought));
    }

    private static double clampSpread(double spread) {
        return Double.isFinite(spread) ? Math.max(0.0, Math.min(0.5, spread)) : 0.0;
    }
}
