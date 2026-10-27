// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.Map;
import java.util.UUID;

/**
 * 0.1.10.27 (X2): the rules of the Trade Terminal that need no game (pure Java; the offline harness runs them): who may do
 * what at a terminal, and how much a standing order trades in one run.
 *
 * <h2>Access</h2>
 * Levels VIEW &lt; TRADE &lt; CONFIG &lt; OWNER. The placer is the owner. A player on the access list has its level; members of
 * the owner's team (vanilla scoreboard team) have the team level; operators count as CONFIG; anyone else has NONE. The highest
 * applicable level wins.
 *
 * <h2>Standing orders</h2>
 * SELL keeps {@code keep} items in the sell buffer and sells the rest, at the daily auction (runs while the owner is offline) or
 * at once (only while the owner is online). BUY tops the stock up to {@code target} at most at {@code limit} coins an item,
 * paid from the owner's mailbox (only while the owner is online). Every trade goes through the same exchange functions as a
 * manual one, under the owner's UUID.
 *
 * <p>Instant sales and purchases wait for a batch of {@link #BATCH} items (a purchase: fewer when the target is smaller; a
 * sale: also when the sell buffer has no empty slot). Every trade rounds its coins (a sale down, a purchase up), so a pipe
 * that brings one item per run would lose up to a quarter of the value of a cheap commodity in one-item sales
 * (Harness1127 'batch'); the auction joins all submissions of a day into one order and needs no batch.
 */
public final class TerminalRules {
    private TerminalRules() {}

    public static final int NONE = 0, VIEW = 1, TRADE = 2, CONFIG = 3, OWNER = 4;
    public static final int MAX_ORDERS = 8, MAX_ACCESS = 16, SELL_SLOTS = 27, OUTPUT_SLOTS = 9;
    /** Ticks between two runs of the standing orders. */
    public static final int RUN_TICKS = 200;
    /** Items an instant sale or a purchase waits for (see the class comment). */
    public static final int BATCH = 64;

    /** The level of {@code player}: owner, list, team (when the player is in the owner's team), operator. */
    public static int level(UUID player, UUID owner, Map<UUID, Integer> list, boolean sameTeam, int teamLevel, boolean operator) {
        if (player == null) return NONE;
        if (player.equals(owner)) return OWNER;
        int level = NONE;
        Integer listed = list.get(player);
        if (listed != null) level = Math.max(level, Math.max(NONE, Math.min(CONFIG, listed)));
        if (sameTeam) level = Math.max(level, Math.max(NONE, Math.min(CONFIG, teamLevel)));
        if (operator) level = Math.max(level, CONFIG);
        return level;
    }

    /** The terminal actions and the level each needs. */
    public enum Action {
        OPEN(VIEW), DEPOSIT_SELL(TRADE), SETTLE(TRADE), LOAD_BUFFER(TRADE), TAKE_BUFFER(CONFIG), EDIT_ORDERS(CONFIG),
        OUTPUT_MODE(CONFIG), UNLOAD_OUTPUT(CONFIG), EDIT_ACCESS(OWNER);

        public final int needs;

        Action(int needs) { this.needs = needs; }
    }

    public static boolean may(int level, Action action) { return level >= action.needs; }

    public enum Side { SELL, BUY }

    /** 0.1.10.28 (X4): SCRAP = an instant sale whose rest goes to the scrap sink ({@link ScrapRules}) when the bid is below the floor. */
    public enum Mode { AUCTION, INSTANT, SCRAP }

    /**
     * A standing order. SELL: {@code amount} = items to keep, {@code limit} = lowest price per item (INSTANT, 0 = any) or the
     * lowest average in percent of the bid without pressure (AUCTION, 0..100). BUY: {@code amount} = target stock,
     * {@code limit} = highest price per item. 0.1.10.29: {@code percent} (SELL INSTANT / SCRAP only): {@code limit} is the lowest
     * price per item in percent of the bid without pressure at the time of the sale (0..100, {@link MinPrice#limit}).
     */
    public record Order(Side side, String item, long amount, Mode mode, long limit, boolean enabled, boolean percent) {
        public Order {
            if (side == null || item == null || item.isEmpty() || mode == null) throw new IllegalArgumentException("order");
            if (amount < 0 || limit < 0) throw new IllegalArgumentException("order amounts");
            if (side == Side.SELL && mode == Mode.AUCTION && limit > 100) throw new IllegalArgumentException("auction minimum 0..100 %");
            percent = percent && side == Side.SELL && mode != Mode.AUCTION;
            if (percent && limit > MinPrice.MAX_PERCENT) throw new IllegalArgumentException("minimum 0..100 %");
        }

        public Order(Side side, String item, long amount, Mode mode, long limit, boolean enabled) {
            this(side, item, amount, mode, limit, enabled, false);
        }

        /** The same order, enabled or not. */
        public Order withEnabled(boolean on) { return new Order(side, item, amount, mode, limit, on, percent); }
    }

    /**
     * The outcome of an order in a run (shown in the terminal): OK traded; DISABLED; NOTHING to trade; OFFLINE waits for the
     * owner; LOCKED the owner has not unlocked the commodity; NO_ENERGY; NO_COINS in the owner's mailbox; NO_MARKET not a
     * datapack commodity (any more); PRICE the market does not trade at the limit now; BATCH an instant
     * sale or a purchase waits for a batch.
     */
    public enum Status { OK, DISABLED, NOTHING, OFFLINE, LOCKED, NO_ENERGY, NO_COINS, NO_MARKET, PRICE, BATCH }

    /**
     * Items a SELL order offers now: the buffer count above the kept amount; 0 when it must wait. An instant sale waits for
     * {@link #BATCH} items unless the sell buffer is full ({@code bufferFull}: no empty slot).
     */
    public static long sellOffer(Order o, long inBuffer, boolean ownerOnline, boolean lockedOffline, boolean bufferFull) {
        return sellStatus(o, inBuffer, ownerOnline, lockedOffline, bufferFull) == Status.OK ? Math.max(0, inBuffer - o.amount()) : 0;
    }

    /** The status of a SELL order before it trades. */
    public static Status sellStatus(Order o, long inBuffer, boolean ownerOnline, boolean lockedOffline, boolean bufferFull) {
        if (!o.enabled() || o.side() != Side.SELL) return Status.DISABLED;
        if (o.mode() != Mode.AUCTION && !ownerOnline || lockedOffline) return Status.OFFLINE;
        long surplus = inBuffer - o.amount();
        if (surplus <= 0) return Status.NOTHING;
        if (o.mode() != Mode.AUCTION && surplus < BATCH && !bufferFull) return Status.BATCH;
        return Status.OK;
    }

    /**
     * Items a BUY order asks for now: up to the target, as many as the owner's mailbox coins pay at the limit; 0 while the
     * owner is offline (Q3: only the daily auction runs offline) and while fewer than a batch are missing
     * ({@link #BATCH}, or the whole target when it is smaller).
     */
    public static long buyNeed(Order o, long stock, long coins, boolean ownerOnline) {
        if (buyStatus(o, stock, coins, ownerOnline) != Status.OK) return 0;
        return Math.min(o.amount() - stock, coins / o.limit());
    }

    /** The status of a BUY order before it trades. */
    public static Status buyStatus(Order o, long stock, long coins, boolean ownerOnline) {
        if (!o.enabled() || o.side() != Side.BUY) return Status.DISABLED;
        if (!ownerOnline) return Status.OFFLINE;
        long want = o.amount() - stock;
        if (want <= 0) return Status.NOTHING;
        if (want < Math.min(BATCH, o.amount())) return Status.BATCH;
        return o.limit() > 0 && coins >= o.limit() ? Status.OK : Status.NO_COINS;
    }

    /** The run of a terminal falls on this tick (staggered by the position hash so terminals do not run together). */
    public static boolean runsOn(long gameTime, long positionHash) {
        return Math.floorMod(gameTime + positionHash, RUN_TICKS) == 0;
    }
}
