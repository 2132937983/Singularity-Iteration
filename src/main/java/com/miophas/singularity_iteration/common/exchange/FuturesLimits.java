// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.1.10.42 (futures caps, proposal Q4): the limits of a player's open futures. Pure (the offline harness runs it).
 *
 * <ul>
 *   <li><b>Rows</b>: contracts of the same commodity, side and expiry day are one row ({@link #rows}); a player holds at most
 *       {@link #MAX_ROWS} rows (before: 8 contracts, each order its own). Inside a row every contract keeps its own price as a
 *       lot, so the settlement is the same sum as before; a new order at the price of an existing lot adds to that lot (the
 *       payoff is linear in the quantity, so nothing changes). A row has at most {@link #MAX_LOTS} lots and
 *       {@link #MAX_ROW_QUANTITY} items.</li>
 *   <li><b>Quantity of one order</b> ({@link #maxQuantity}): {@code max(1,024, 1,000,000 / contract price)}, at most
 *       {@link #MAX_QUANTITY} (before: 1,024 for every commodity): about a million coins of collateral for cheap commodities,
 *       1,024 items for those above 976 coins.</li>
 *   <li><b>Open collateral</b>: the collateral of all open lots of a player stays at most the server's
 *       {@code futuresMaxOpenCollateral} (multiplayer; 0 = no cap; single player: no cap).</li>
 * </ul>
 */
public final class FuturesLimits {
    private FuturesLimits() {}

    public static final int MAX_ROWS = 16, MAX_LOTS = 32, MIN_ORDER = 1024, MAX_QUANTITY = 65_536;
    public static final int MAX_ROW_QUANTITY = MAX_LOTS * MAX_QUANTITY;
    /** Coins of collateral one order may reach before the quantity cap goes below {@link #MAX_QUANTITY}. */
    public static final long ORDER_VALUE = 1_000_000L;
    /** The multiplayer default of {@code futuresMaxOpenCollateral}. */
    public static final long DEFAULT_OPEN_COLLATERAL = 5_000_000L;

    /** Why an order cannot open. */
    public enum Refusal { NONE, QUANTITY, ROWS, LOTS, ROW_QUANTITY, COLLATERAL }

    /** One open contract (a lot): commodity, side, expiry day, price per item, quantity. */
    public record Lot(String commodity, boolean isLong, long expiry, long price, int quantity) {
        public long collateral() { return Forwards.collateral(price, quantity); }
    }

    /** A row: the lots of one commodity, side and expiry; {@code first} the index of its first lot in the input. */
    public record Row(String commodity, boolean isLong, long expiry, long quantity, long collateral, int lots, int first) {
        /** The average price per item (collateral / quantity, rounded to the nearest coin). */
        public long averagePrice() { return quantity <= 0 ? 0 : (collateral + quantity / 2) / quantity; }
    }

    /** The most items of one order at {@code price} coins per item. */
    public static int maxQuantity(long price) {
        if (price <= 0) return MAX_QUANTITY;
        return (int) Math.max(MIN_ORDER, Math.min(MAX_QUANTITY, ORDER_VALUE / price));
    }

    /** The cap of the open collateral: {@code configured} in multiplayer (0 = none), none in single player; -1 = no cap. */
    public static long collateralCap(long configured, boolean multiplayer) {
        return multiplayer && configured > 0 ? configured : -1;
    }

    static String key(String commodity, boolean isLong, long expiry) { return commodity + '|' + (isLong ? 'L' : 'S') + '|' + expiry; }

    /** The rows of {@code lots}, in the order of their first lot. */
    public static List<Row> rows(List<Lot> lots) {
        Map<String, long[]> sums = new LinkedHashMap<>();   // quantity, collateral, lots, first
        Map<String, Lot> heads = new LinkedHashMap<>();
        for (int i = 0; i < lots.size(); i++) {
            Lot l = lots.get(i);
            String k = key(l.commodity(), l.isLong(), l.expiry());
            long[] s = sums.get(k);
            if (s == null) { s = new long[] {0, 0, 0, i}; sums.put(k, s); heads.put(k, l); }
            s[0] += l.quantity();
            s[1] = Math.addExact(s[1], l.collateral());
            s[2]++;
        }
        List<Row> out = new ArrayList<>(sums.size());
        for (Map.Entry<String, long[]> e : sums.entrySet()) {
            Lot h = heads.get(e.getKey());
            long[] s = e.getValue();
            out.add(new Row(h.commodity(), h.isLong(), h.expiry(), s[0], s[1], (int) s[2], (int) s[3]));
        }
        return out;
    }

    /** The collateral of all {@code lots}. */
    public static long openCollateral(List<Lot> lots) {
        long sum = 0;
        for (Lot l : lots) sum = Math.addExact(sum, l.collateral());
        return sum;
    }

    /**
     * Whether a player holding {@code mine} may open {@code quantity} items of {@code commodity} ({@code isLong}, expiring on
     * {@code expiry}) at {@code price}; {@code cap} the open collateral cap (-1 = none). The first rule broken, in the order
     * quantity, rows, lots, row quantity, collateral.
     */
    public static Refusal check(List<Lot> mine, String commodity, boolean isLong, long expiry, long price, int quantity, long cap) {
        if (quantity < 1 || quantity > maxQuantity(price)) return Refusal.QUANTITY;
        Row row = null;
        List<Row> rows = rows(mine);
        for (Row r : rows) if (r.commodity().equals(commodity) && r.isLong() == isLong && r.expiry() == expiry) row = r;
        if (row == null && rows.size() >= MAX_ROWS) return Refusal.ROWS;
        if (row != null && row.lots() >= MAX_LOTS && !hasLotAt(mine, commodity, isLong, expiry, price)) return Refusal.LOTS;
        if (row != null && row.quantity() + quantity > MAX_ROW_QUANTITY) return Refusal.ROW_QUANTITY;
        if (cap >= 0) {
            long need = Forwards.collateral(price, quantity);
            long open = openCollateral(mine);
            if (open > cap - need) return Refusal.COLLATERAL;
        }
        return Refusal.NONE;
    }

    /** A lot of that row at exactly {@code price} exists (a new order adds to it). */
    public static boolean hasLotAt(List<Lot> mine, String commodity, boolean isLong, long expiry, long price) {
        for (Lot l : mine)
            if (l.commodity().equals(commodity) && l.isLong() == isLong && l.expiry() == expiry && l.price() == price) return true;
        return false;
    }

    /** Coins of collateral still free under {@code cap} (-1 = no cap: {@link Long#MAX_VALUE}). */
    public static long room(long open, long cap) {
        return cap < 0 ? Long.MAX_VALUE : Math.max(0, cap - open);
    }

    /** The most items an order at {@code price} may have now: the order cap and the free collateral (0 = none). */
    public static long orderRoom(long price, long open, long cap) {
        long max = maxQuantity(price);
        if (cap < 0 || price <= 0) return max;
        return Math.min(max, room(open, cap) / price);
    }
}
