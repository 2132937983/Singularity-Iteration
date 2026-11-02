// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.TerminalRules.Mode;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Order;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Side;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Status;
import java.util.Collection;
import java.util.List;

/**
 * 0.1.10.42 (exchange proposal idea 4, option A of Q8): the rules of the Trade Boxes. Pure (the offline harness runs it).
 *
 * <ul>
 *   <li><b>Basic Trade Box</b> (chest, emerald, iron, redstone; no circuit): {@link #BASIC_SLOTS} slots that hoppers and
 *       pipes fill with commodities. Every run (10 seconds) its commodities join the owner's daily auction order with the
 *       box's minimum in percent of the bid; what the auction does not sell goes to the owner's mailbox as before. Nothing
 *       comes out of it by pipe.</li>
 *   <li><b>Advanced Trade Box</b> (a basic box, a UU-matter cell and two advanced circuits; or the upgrade kit used on a
 *       placed basic box, which keeps its contents): {@link #ADVANCED_SLOTS} slots, {@link #RETURN_SLOTS} return slots that
 *       pipes empty, a filter of {@link #FILTER_SLOTS} items. It sells into the auction or at once ({@link #INSTANT}: the
 *       owner's sell depth, a minimum in percent of the bid or in coins; {@link #SCRAP}: the rest to the scrap sink), and
 *       delivers to the bounties the owner claimed before it sells.</li>
 *   <li>Every sale is the owner's ({@link TerminalOrders}): the sell depth, allowance, unlocks and records of the owner's
 *       UUID, the coins on the owner's coin account. Instant sales follow the Trade Terminal: only while the owner is online
 *       (with {@code offlineAuction} the box uses the auction meanwhile) and in batches of 64 items unless the box is full.
 *       A box needs no energy.</li>
 * </ul>
 */
public final class TradeBoxRules {
    private TradeBoxRules() {}

    public static final int BASIC = 0, ADVANCED = 1;
    public static final int BASIC_SLOTS = 27, ADVANCED_SLOTS = 54, RETURN_SLOTS = 9, FILTER_SLOTS = 9;
    /** Sell modes: the daily auction, at once, at once with the rest to the scrap sink. */
    public static final int AUCTION = 0, INSTANT = 1, SCRAP = 2, MODES = 3;
    /** The minimum of a new box: half the bid. */
    public static final int DEFAULT_PERCENT = 50;
    public static final long MAX_COINS = 999_999_999L;
    /** The counters of the market day: sold at once, their coins, into the auction, scrapped, delivered to bounties, their coins. */
    public static final int SOLD = 0, COINS = 1, AUCTIONED = 2, SCRAPPED = 3, DELIVERED = 4, DELIVERED_COINS = 5, COUNTERS = 6;

    /** The settings of a box (clamped to the tier by {@link #of}). */
    public record Settings(int mode, int percent, long coins, boolean inCoins, boolean autoDeliver, boolean offlineAuction) {
        public static final Settings DEFAULT = new Settings(AUCTION, DEFAULT_PERCENT, 1, false, true, true);
    }

    /** {@code s} within the ranges of the tier: a basic box sells only into the auction, with a minimum in percent. */
    public static Settings of(int tier, Settings s) {
        boolean advanced = tier == ADVANCED;
        int mode = advanced ? Math.max(0, Math.min(MODES - 1, s.mode())) : AUCTION;
        return new Settings(mode, percent(s.percent()), coins(s.coins()), advanced && s.inCoins(), s.autoDeliver(), s.offlineAuction());
    }

    public static int slots(int tier) { return tier == ADVANCED ? ADVANCED_SLOTS : BASIC_SLOTS; }

    public static int percent(long p) { return (int) Math.max(0, Math.min(MinPrice.MAX_PERCENT, p)); }

    public static long coins(long c) { return Math.max(1, Math.min(MAX_COINS, c)); }

    /** The mode of this run: an instant mode falls back to the auction while the owner is offline (when allowed). */
    public static int runMode(int tier, Settings s, boolean ownerOnline) {
        Settings x = of(tier, s);
        if (x.mode() != AUCTION && !ownerOnline && x.offlineAuction()) return AUCTION;
        return x.mode();
    }

    /** The filter (commodities of the filter items) accepts {@code commodity}: an empty filter accepts every commodity. */
    public static boolean accepts(Collection<String> filterCommodities, String commodity) {
        if (commodity == null) return false;
        return filterCommodities.isEmpty() || filterCommodities.contains(commodity);
    }

    /** The sell order of one commodity of the box in {@code mode} ({@link #runMode}). */
    public static Order order(String commodity, int mode, Settings s) {
        if (mode == AUCTION) return new Order(Side.SELL, commodity, 0, Mode.AUCTION, Math.min(100, percent(s.percent())), true, false);
        Mode m = mode == SCRAP ? Mode.SCRAP : Mode.INSTANT;
        if (s.inCoins()) return new Order(Side.SELL, commodity, 0, m, coins(s.coins()), true, false);
        return new Order(Side.SELL, commodity, 0, m, percent(s.percent()), true, true);
    }

    /** One status for the panel: OK when any order traded, else the first reason that is not NOTHING or DISABLED, else NOTHING. */
    public static Status summary(List<Status> statuses) {
        Status first = null;
        for (Status s : statuses) {
            if (s == Status.OK) return Status.OK;
            if (first == null && s != Status.NOTHING && s != Status.DISABLED) first = s;
        }
        return first == null ? Status.NOTHING : first;
    }

    /** The counters for market day {@code today}: cleared when {@code counterDay} is another day. Returns the day now counted. */
    public static long roll(long[] counters, long counterDay, long today) {
        if (counterDay != today) java.util.Arrays.fill(counters, 0);
        return today;
    }

    /** The counter a log line of {@link TerminalOrders} adds to, or -1. */
    public static int counter(int logKind) {
        return switch (logKind) {
            case TerminalOrders.LOG_SOLD -> SOLD;
            case TerminalOrders.LOG_AUCTION -> AUCTIONED;
            case TerminalOrders.LOG_SCRAP -> SCRAPPED;
            default -> -1;
        };
    }
}
