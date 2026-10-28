// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * 0.1.10.35 (items 5-8): player shops (pure; {@link ExchangeData} saves them, {@link Shops} runs them in the game, the
 * offline harness checks the rules).
 *
 * <p>A commission is a one-time request; a shop holds long-lived offers of one player that any number of customers take in
 * whole units, again and again:
 * <ul>
 *   <li><b>EXCHANGE</b>: {@code costCount} of {@code cost} for {@code goodsCount} of {@code goods} (barter, no fee). The owner
 *       stocks the goods; the customer's items go to the shop's proceeds box.</li>
 *   <li><b>SELL</b>: {@code price} coins for {@code goodsCount} of {@code goods}; the owner's coin account gets the price
 *       less the exchange fee.</li>
 *   <li><b>BUY</b>: {@code costCount} of {@code cost} for {@code price} coins; the owner stocks the coins, the customer gets
 *       the price less the exchange fee, the items go to the proceeds box.</li>
 * </ul>
 * One shop per player, a number of offers and a storage of stacks (each offer's goods stock and each proceeds item in stacks
 * of the item's own size); an offer that would overfill the storage trades less, down to nothing. The owner's absence is
 * counted in run days: calendar days on which the server ran since the owner was last online (a server stopped for a week
 * counts no days; a server running every day counts each day). Idle after a number of run days (listed last, still trades),
 * dissolved after more (everything back to the owner's mailbox).
 */
public final class ShopRules {
    private ShopRules() {}

    public enum Kind { EXCHANGE, SELL, BUY }

    /** Items or coins of one unit: at most this many items, this many coins. */
    public static final int MAX_COUNT = 9_999;
    public static final long MAX_PRICE = 999_999_999L;
    /** Units in stock of one offer at most (units x price stays far below the range of a long). */
    public static final int MAX_UNITS = 1_000_000;

    /** The price and the units of a new offer in one long (the action packet): {@code price + units x 2^31}. */
    public static long pack(long price, long units) {
        return (Math.max(0, Math.min(Integer.MAX_VALUE, units)) << 31) | Math.max(0, Math.min(Integer.MAX_VALUE, price));
    }

    public static long packedPrice(long packed) { return packed & Integer.MAX_VALUE; }

    public static long packedUnits(long packed) { return packed >>> 31; }
    /** Shops of the server at most (a bound for a damaged save). */
    public static final int MAX_SHOPS = 1_024;

    public static final class Offer {
        public long id;
        public Kind kind = Kind.EXCHANGE;
        /** EXCHANGE and SELL: what the customer gets, per unit. */
        public String goods = "";
        public int goodsCount;
        /** EXCHANGE and BUY: what the customer gives, per unit. */
        public String cost = "";
        public int costCount;
        /** SELL and BUY: coins per unit. */
        public long price;
        /** Units in stock (goods units; BUY: units of coins). */
        public long stock;
        /** Paused by the owner; the upkeep of the week was not paid. */
        public boolean paused, unpaid;
        /** Units traded in total. */
        public long trades;

        /** Goods items in stock (0 for BUY: its stock is coins). */
        public long stockItems() { return kind == Kind.BUY ? 0 : stock * goodsCount; }

        /** Coins in stock (BUY only). */
        public long stockCoins() { return kind == Kind.BUY ? stock * price : 0; }

        /** The item that comes into the proceeds box ("" for SELL). */
        public String proceedsItem() { return kind == Kind.SELL ? "" : cost; }

        public boolean tradable() { return !paused && !unpaid && stock > 0; }
    }

    public static final class Shop {
        public UUID owner;
        public String ownerName = "";
        /** The run day on which the owner was last online. */
        public long seenRunDay;
        /** The market day of the last trade (Long.MIN_VALUE = none). */
        public long lastTradeDay = Long.MIN_VALUE;
        /** The market week whose upkeep is paid. */
        public long upkeepWeek = Long.MIN_VALUE;
        public final List<Offer> offers = new ArrayList<>();
        /** Items customers gave (EXCHANGE and BUY), until the owner collects them. */
        public final Map<String, Long> proceeds = new TreeMap<>();

        public Offer offer(long id) {
            for (Offer o : offers) if (o.id == id) return o;
            return null;
        }

        public boolean isEmpty() { return offers.isEmpty() && proceeds.isEmpty(); }
    }

    /**
     * Why an offer cannot be posted, or null: counts 1..{@link #MAX_COUNT}, price 1..{@link #MAX_PRICE}, the items named and
     * not the same (EXCHANGE).
     */
    public static String check(Kind kind, String goods, int goodsCount, String cost, int costCount, long price) {
        if (kind == null) return "bad_offer";
        boolean needGoods = kind != Kind.BUY, needCost = kind != Kind.SELL, needPrice = kind != Kind.EXCHANGE;
        if (needGoods && (goods == null || goods.isEmpty() || goodsCount < 1 || goodsCount > MAX_COUNT)) return "bad_offer";
        if (needCost && (cost == null || cost.isEmpty() || costCount < 1 || costCount > MAX_COUNT)) return "bad_offer";
        if (needPrice && (price < 1 || price > MAX_PRICE)) return "bad_offer";
        if (kind == Kind.EXCHANGE && goods.equals(cost)) return "bad_offer";
        return null;
    }

    /** Stacks of {@code items} of an item of {@code maxStack}. */
    public static long stacks(long items, int maxStack) {
        if (items <= 0) return 0;
        int m = Math.max(1, maxStack);
        return (items + m - 1) / m;
    }

    /** Stacks the shop's storage holds now: every offer's goods stock and every proceeds item. */
    public static long storage(Shop s, ToIntFunction<String> maxStack) {
        long n = 0;
        for (Offer o : s.offers) n += stacks(o.stockItems(), maxStack.applyAsInt(o.goods));
        for (Map.Entry<String, Long> e : s.proceeds.entrySet()) n += stacks(e.getValue(), maxStack.applyAsInt(e.getKey()));
        return n;
    }

    /** Stacks after {@code units} of offer {@code o} are traded (its stock less, the proceeds more). */
    public static long storageAfter(Shop s, Offer o, long units, ToIntFunction<String> maxStack) {
        long now = storage(s, maxStack);
        int gm = maxStack.applyAsInt(o.goods);
        long after = now - stacks(o.stockItems(), gm) + (o.kind == Kind.BUY ? 0 : stacks((o.stock - units) * o.goodsCount, gm));
        String p = o.proceedsItem();
        if (!p.isEmpty()) {
            int pm = maxStack.applyAsInt(p);
            long have = s.proceeds.getOrDefault(p, 0L);
            after = after - stacks(have, pm) + stacks(have + units * o.costCount, pm);
        }
        return after;
    }

    /** Stacks after {@code units} more units are stocked to offer {@code o} (BUY: none, its stock is coins). */
    public static long storageAfterStock(Shop s, Offer o, long units, ToIntFunction<String> maxStack) {
        long now = storage(s, maxStack);
        if (o.kind == Kind.BUY) return now;
        int gm = maxStack.applyAsInt(o.goods);
        return now - stacks(o.stockItems(), gm) + stacks((o.stock + units) * o.goodsCount, gm);
    }

    /**
     * Units of offer {@code o} a customer may take now: at most {@code wanted}, the stock and what the customer can give
     * ({@code afford} units), and as many as fit the storage of {@code capStacks} (a shop already over its storage, after a
     * change of the setting, may still trade what does not fill it more).
     */
    public static long maxUnits(Shop s, Offer o, long wanted, long afford, int capStacks, ToIntFunction<String> maxStack) {
        if (!o.tradable()) return 0;
        long k = Math.min(Math.min(wanted, o.stock), afford);
        if (k <= 0) return 0;
        if (o.proceedsItem().isEmpty()) return k;
        long now = storage(s, maxStack), limit = Math.max(capStacks, now);
        if (storageAfter(s, o, k, maxStack) <= limit) return k;
        // A unit changes two terms only: this offer's stock (fewer stacks) and its proceeds item (more). Their sum is not
        // monotone in the units (a swap of 64 pearls for 1 diamond frees a stack now and then), so a binary search could
        // stop early (59 of 20,000 random shops in the harness): from the most units the proceeds alone allow, downwards.
        int gm = Math.max(1, maxStack.applyAsInt(o.goods)), pm = Math.max(1, maxStack.applyAsInt(o.cost));
        long have = s.proceeds.getOrDefault(o.cost, 0L);
        long others = now - stacks(o.stockItems(), gm) - stacks(have, pm);
        long upper = Math.min(k - 1, ((limit - others) * pm - have) / Math.max(1, o.costCount));
        for (long u = upper; u >= 1; u--) {
            long stock = o.kind == Kind.BUY ? 0 : stacks((o.stock - u) * o.goodsCount, gm);
            if (others + stock + stacks(have + u * o.costCount, pm) <= limit) return u;
        }
        return 0;
    }

    /** Units of {@code o} that may be stocked: at most {@code wanted}, within the storage (BUY: any, coins). */
    public static long maxStock(Shop s, Offer o, long wanted, int capStacks, ToIntFunction<String> maxStack) {
        if (wanted <= 0) return 0;
        if (o.kind == Kind.BUY) return wanted;
        if (storageAfterStock(s, o, wanted, maxStack) <= capStacks) return wanted;
        long lo = 0, hi = wanted;
        while (lo < hi) {
            long mid = (lo + hi + 1) >>> 1;
            if (storageAfterStock(s, o, mid, maxStack) <= capStacks) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    /** The exchange fee of a coin amount (rounded up, at most the amount). */
    public static long fee(long coins, double rate) {
        if (coins <= 0 || !(rate > 0)) return 0;
        return Math.min(coins, (long) Math.ceil(coins * Math.min(1.0, rate) - 1.0E-9));
    }

    /** The upkeep of a week: every offer pays. */
    public static long upkeep(Shop s, long perOffer) {
        return Math.max(0, perOffer) * s.offers.size();
    }

    // ------------------------------------------------------------------ absence

    /** {run days, last date} after the server ran on calendar day {@code today} (epoch day); a clock set back counts nothing. */
    public static long[] runDay(long runDays, long lastDate, long today) {
        if (today > lastDate) return new long[] {runDays + 1, today};
        return new long[] {runDays, lastDate};
    }

    /** Run days since the owner was last online. */
    public static long absent(long runDays, long seenRunDay) { return Math.max(0, runDays - seenRunDay); }

    public static boolean idle(long absent, int idleDays) { return idleDays > 0 && absent >= idleDays; }

    public static boolean dissolve(long absent, int closeDays) { return closeDays > 0 && absent >= closeDays; }

    /** The list order: shops not idle first, then by the last trade (latest first), then by the owner's name. */
    public static int compare(Shop a, Shop b, long runDays, int idleDays) {
        boolean ia = idle(absent(runDays, a.seenRunDay), idleDays), ib = idle(absent(runDays, b.seenRunDay), idleDays);
        if (ia != ib) return ia ? 1 : -1;
        int c = Long.compare(b.lastTradeDay, a.lastTradeDay);
        return c != 0 ? c : a.ownerName.compareToIgnoreCase(b.ownerName);
    }

    /** The shop trades {@code item} (as goods or as cost). */
    public static boolean trades(Shop s, String item) {
        for (Offer o : s.offers) if (item.equals(o.goods) || item.equals(o.cost)) return true;
        return false;
    }
}
