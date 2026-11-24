// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 0.1.10.21 (futures M2): the limit order book of one commodity, a continuous double auction with price-time
 * priority. Pure arithmetic; prices are integers (coins or mB of UU per item).
 *
 * <ul>
 *   <li>An incoming order first trades against the resting orders of the other side, best price first and, at one
 *       price, oldest first, at the price of the resting order, while that price is within the limit.</li>
 *   <li><b>Self-trade prevention</b>: a resting order of the same owner or of the same team is skipped (it stays in
 *       the book). Trades with oneself or one's team would only paint the price.</li>
 *   <li>The rest of a limit order can rest in the book; a market order never rests.</li>
 * </ul>
 * The book holds no items or coins: the exchange escrows them when an order is placed ({@code ExchangeService}).
 */
public final class OrderBook {
    /** A resting order. {@code team} is empty without a team. */
    public record Order(long id, String owner, String team, boolean buy, long price, int remaining, long day) {
        public Order {
            if (price < 1 || remaining < 1) throw new IllegalArgumentException("price and quantity must be positive");
        }
    }

    /** A trade between a resting order (maker) and an incoming one (taker). */
    public record Fill(long makerId, String makerOwner, String takerOwner, boolean takerBuys, long price, int quantity) { }

    /** One price level of the depth view. */
    public record Level(long price, long quantity, int orders) { }

    private static final class Entry {
        final Order order;
        int remaining;
        Entry(Order order) { this.order = order; this.remaining = order.remaining(); }
    }

    private final TreeMap<Long, ArrayDeque<Entry>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, ArrayDeque<Entry>> asks = new TreeMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Entry> byId = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private int size;

    /** Resting orders. */
    public int size() { return size; }

    /**
     * Trades an incoming order against the book and returns the fills (the book is updated).
     *
     * @param limit the worst acceptable price (for a market buy {@link Long#MAX_VALUE}, for a market sell 1)
     */
    public List<Fill> match(String owner, String team, boolean buy, long limit, int quantity) {
        List<Fill> fills = new ArrayList<>();
        TreeMap<Long, ArrayDeque<Entry>> side = buy ? asks : bids;
        int left = quantity;
        Iterator<Map.Entry<Long, ArrayDeque<Entry>>> levels = side.entrySet().iterator();
        while (left > 0 && levels.hasNext()) {
            var level = levels.next();
            long price = level.getKey();
            if (buy ? price > limit : price < limit) break;
            Iterator<Entry> queue = level.getValue().iterator();
            while (left > 0 && queue.hasNext()) {
                Entry entry = queue.next();
                if (sameParty(entry.order, owner, team)) continue;
                int take = Math.min(left, entry.remaining);
                fills.add(new Fill(entry.order.id(), entry.order.owner(), owner, buy, price, take));
                entry.remaining -= take;
                left -= take;
                if (entry.remaining == 0) { queue.remove(); byId.remove(entry.order.id()); size--; }
            }
            if (level.getValue().isEmpty()) levels.remove();
        }
        return fills;
    }

    private static boolean sameParty(Order order, String owner, String team) {
        return order.owner().equals(owner) || !team.isEmpty() && team.equals(order.team());
    }

    /** Adds a resting order (its {@code remaining} is the quantity that rests). */
    public void rest(Order order) {
        Entry entry = new Entry(order);
        (order.buy() ? bids : asks).computeIfAbsent(order.price(), p -> new ArrayDeque<>()).addLast(entry);
        byId.put(order.id(), entry);
        size++;
    }

    /** Removes a resting order; returns its unfilled quantity, or 0 when it is not in the book. */
    public int cancel(long id) {
        Entry target = byId.remove(id);
        if (target == null) return 0;
        TreeMap<Long, ArrayDeque<Entry>> side = target.order.buy() ? bids : asks;
        ArrayDeque<Entry> queue = side.get(target.order.price());
        if (queue != null && queue.remove(target)) {
            size--;
            if (queue.isEmpty()) side.remove(target.order.price());
            return target.remaining;
        }
        return 0;
    }

    /** 0.1.10.33: changes the day of resting orders in place, time priority kept (the repair of a market day that went back). */
    public void redate(java.util.function.LongUnaryOperator day) {
        for (var side : List.of(bids, asks)) {
            for (var queue : side.values()) {
                ArrayDeque<Entry> moved = new ArrayDeque<>(queue.size());
                for (Entry e : queue) {
                    long d = day.applyAsLong(e.order.day());
                    if (d == e.order.day()) { moved.addLast(e); continue; }
                    Order o = e.order;
                    Entry x = new Entry(new Order(o.id(), o.owner(), o.team(), o.buy(), o.price(), o.remaining(), d));
                    x.remaining = e.remaining;
                    moved.addLast(x);
                    byId.put(o.id(), x);
                }
                queue.clear();
                queue.addAll(moved);
            }
        }
    }

    /** The resting order with this id (with its current remaining quantity), or null. */
    public Order find(long id) {
        Entry e = byId.get(id);
        if (e == null) return null;
        return new Order(e.order.id(), e.order.owner(), e.order.team(), e.order.buy(), e.order.price(), e.remaining, e.order.day());
    }

    /** All resting orders with their current remaining quantity: bids best first, then asks best first. */
    public List<Order> orders() {
        List<Order> out = new ArrayList<>(size);
        for (var side : List.of(bids, asks))
            for (var queue : side.values())
                for (Entry e : queue) out.add(new Order(e.order.id(), e.order.owner(), e.order.team(), e.order.buy(), e.order.price(), e.remaining, e.order.day()));
        return out;
    }

    /** The best price of a side, or 0 when it is empty. */
    public long best(boolean bidSide) {
        var side = bidSide ? bids : asks;
        return side.isEmpty() ? 0 : side.firstKey();
    }

    /** The first {@code levels} price levels of a side, best first. */
    public List<Level> depth(boolean bidSide, int levels) {
        List<Level> out = new ArrayList<>();
        for (var level : (bidSide ? bids : asks).entrySet()) {
            if (out.size() >= levels) break;
            long quantity = 0;
            for (Entry e : level.getValue()) quantity += e.remaining;
            out.add(new Level(level.getKey(), quantity, level.getValue().size()));
        }
        return out;
    }
}
