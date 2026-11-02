// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 0.1.10.42: the rules of the wealth ranking (follow-up proposal item 2, Q12). Pure (the offline harness runs it).
 *
 * <ul>
 *   <li><b>Categories</b>: {@link #TOTAL} total assets now; this market week's {@link #TRADE} net trade (coins received from the
 *       market and other players minus coins paid, {@link MarketRecords#TRADE_IN} - {@link MarketRecords#TRADE_OUT}),
 *       {@link #FUTURES} realised futures profit and {@link #BOUNTY} value delivered to bounties. The weekly categories rank by
 *       results, not volume: trades back and forth between two accounts only lose the fees.</li>
 *   <li><b>Total assets</b> by part: {@link #PART_COINS} coins on the account and carried (an offline player: carried at the
 *       logout), {@link #PART_UU} UU-matter (account and escrow) at the coin price of the UU board, {@link #PART_GOODS} items of
 *       the mailbox, the shop, the auction, sell orders and item rewards held for commissions at the market's bid,
 *       {@link #PART_ESCROW} coins held for buy orders, commission rewards and shop purchases, {@link #PART_FUTURES} what the open
 *       futures would pay at today's price, {@link #PART_BONDS} (0.1.10.43) the principal of treasury bonds and of the treasury fund
 *       (with subscriptions waiting for the next market day). Coins in chests and items outside the exchange do not count.</li>
 *   <li><b>Places</b> ({@link #rank}): highest value first, equal values share a place (1, 2, 2, 4); the top {@link #TOP} and the
 *       viewer's place. A player who hides their name is listed without it (the viewer always sees their own).</li>
 * </ul>
 */
public final class WealthRules {
    private WealthRules() {}

    public static final int TOTAL = 0, TRADE = 1, FUTURES = 2, BOUNTY = 3, CATEGORIES = 4;
    public static final int PART_COINS = 0, PART_UU = 1, PART_GOODS = 2, PART_ESCROW = 3, PART_FUTURES = 4, PART_BONDS = 5, PARTS = 6;
    public static final int TOP = 10;

    /** A player of the ranking: id (UUID text), name, value in the category, name hidden. */
    public record Player(String id, String name, long value, boolean hidden) { }

    /** A listed place: rank, name ("" when hidden from the viewer), value, the viewer's own. */
    public record Place(int rank, String name, long value, boolean me) { }

    /** The places of the top and the viewer's rank (0 = not ranked) and value, the players ranked. */
    public record Ranked(List<Place> top, int myRank, long myValue, int players) { }

    /** The sum of the parts (saturating at {@link Long#MAX_VALUE} / {@link Long#MIN_VALUE}). */
    public static long total(long[] parts) {
        long sum = 0;
        for (long p : parts) sum = add(sum, p);
        return sum;
    }

    /** {@code a + b}, saturating. */
    public static long add(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        return r;
    }

    /** The value of a weekly category from a player's results of the week ({@link MarketRecords} fields); TOTAL: {@code total}. */
    public static long value(int category, long total, long[] week) {
        return switch (category) {
            case TRADE -> week == null ? 0 : week[MarketRecords.TRADE_IN] - week[MarketRecords.TRADE_OUT];
            case FUTURES -> week == null ? 0 : week[MarketRecords.FUTURES];
            case BOUNTY -> week == null ? 0 : week[MarketRecords.BOUNTY];
            default -> total;
        };
    }

    /**
     * The ranking of {@code players} for the viewer {@code me}: players with a value of 0 are not ranked (the viewer is, when
     * listed); ties share the place and are listed by name.
     */
    public static Ranked rank(List<Player> players, String me, int top) {
        List<Player> ranked = new ArrayList<>();
        for (Player p : players) if (p.value() != 0 || p.id().equals(me)) ranked.add(p);
        ranked.sort(Comparator.comparingLong((Player p) -> p.value()).reversed()
            .thenComparing(p -> p.name().toLowerCase(java.util.Locale.ROOT)).thenComparing(Player::id));
        List<Place> out = new ArrayList<>();
        int myRank = 0, rank = 0;
        long myValue = 0, last = 0;
        for (int i = 0; i < ranked.size(); i++) {
            Player p = ranked.get(i);
            if (i == 0 || p.value() != last) rank = i + 1;
            last = p.value();
            boolean mine = p.id().equals(me);
            if (mine) { myRank = rank; myValue = p.value(); }
            if (out.size() < Math.max(0, top)) out.add(new Place(rank, p.hidden() && !mine ? "" : p.name(), p.value(), mine));
        }
        return new Ranked(out, myRank, myValue, ranked.size());
    }
}
