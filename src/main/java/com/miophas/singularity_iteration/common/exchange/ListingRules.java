// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 0.1.10.21 (futures M3): when demand evidence lists an item on the exchange. Pure.
 *
 * <p>The evidence is the finished commissions for the item (each with its requester, its deliverers, the day and,
 * for a coin reward, the unit price). An item is listed when, within {@link #WINDOW_DAYS} days:
 * <ul>
 *   <li>multiplayer: at least {@code requesters} different requesters asked for it, at least {@code fulfilled}
 *       commissions were delivered, by at least two different deliverers in total, and the rewards reached
 *       {@code minValue} coins;</li>
 *   <li>single player: {@code fulfilled} delivered commissions on different days (one player cannot be several
 *       requesters).</li>
 * </ul>
 * Deliveries by the requester are refused when they happen, and a requester below the play-time minimum does not
 * count ({@code ExchangeService}). The first price is the robust median of the coin unit prices, clipped to the
 * fundamental band ({@link Valuation}).
 */
public final class ListingRules {
    private ListingRules() {}

    public static final int WINDOW_DAYS = 7;

    /** One finished commission. {@code unitPrice} is -1 for a barter reward. */
    public record Evidence(long day, String requester, Set<String> deliverers, long unitPrice, long rewardValue) { }

    /** The decision: list or not, and the first price (0 when there is no coin price). */
    public record Decision(boolean list, long firstPrice, String reason) { }

    public static Decision evaluate(List<Evidence> evidence, long today, boolean singlePlayer, int requesters, int fulfilled,
                                    long minValue, double fundamental) {
        List<Evidence> recent = new ArrayList<>();
        for (Evidence e : evidence) if (today - e.day() < WINDOW_DAYS && today >= e.day() && !e.deliverers().isEmpty()) recent.add(e);
        List<Long> prices = new ArrayList<>();
        for (Evidence e : recent) if (e.unitPrice() > 0) prices.add(e.unitPrice());
        long first = prices.isEmpty() ? Math.round(fundamental) : Valuation.robustPrice(prices);
        first = Valuation.clip(first, fundamental);
        if (singlePlayer) {
            Set<Long> days = new HashSet<>();
            for (Evidence e : recent) days.add(e.day());
            return days.size() >= fulfilled ? new Decision(first > 0, first, "days " + days.size())
                : new Decision(false, 0, "days " + days.size() + "/" + fulfilled);
        }
        Set<String> who = new HashSet<>(), by = new HashSet<>();
        long value = 0;
        for (Evidence e : recent) { who.add(e.requester()); by.addAll(e.deliverers()); value += e.rewardValue(); }
        boolean ok = who.size() >= requesters && recent.size() >= fulfilled && by.size() >= 2 && value >= minValue && first > 0;
        return new Decision(ok, ok ? first : 0, "requesters " + who.size() + "/" + requesters + ", fulfilled " + recent.size() + "/" + fulfilled
            + ", deliverers " + by.size() + "/2, value " + value + "/" + minValue);
    }
}
