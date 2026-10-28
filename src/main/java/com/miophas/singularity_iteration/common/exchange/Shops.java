// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.exchange.ShopRules.Kind;
import com.miophas.singularity_iteration.common.exchange.ShopRules.Offer;
import com.miophas.singularity_iteration.common.exchange.ShopRules.Shop;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * 0.1.10.35 (items 5-8): the player shops in the game ({@link ShopRules} holds the rules). The goods, coins and proceeds of a
 * shop live in the exchange data (no block). Every action costs the energy fee of a trade (the futures machine's action). The
 * fees (offer fee, weekly upkeep, the exchange fee of coin trades) go to the treasury reserve. Only plain items (no data
 * components) are stocked and traded, as with the mailbox.
 */
public final class Shops {
    private Shops() {}

    static final ToIntFunction<String> MAX_STACK = ExchangeTables::maxStack;

    static Item coin() { return mio_icif_normal.COIN.get(); }

    /** An item a shop may stock or take: a registered item, not the coin. */
    static Item goods(String id) {
        Item item = id == null || id.isEmpty() ? null : ExchangeService.item(id);
        return item == null || item == coin() ? null : item;
    }

    // ------------------------------------------------------------------ the clock and the owners

    /**
     * Every 200 ticks: counts a new calendar day the server runs on, marks the owners online, closes the shops of owners away
     * for {@code shopCloseDays} run days, charges the upkeep of a new market week. Returns whether anything changed.
     */
    static boolean tick(MinecraftServer server, ExchangeData d, long day) {
        boolean dirty = false;
        long[] r = ShopRules.runDay(d.runDays, d.lastRunDate, java.time.LocalDate.now().toEpochDay());
        if (r[0] != d.runDays || r[1] != d.lastRunDate) {
            d.runDays = r[0];
            d.lastRunDate = r[1];
            dirty = true;
        }
        if (d.shops.isEmpty()) return dirty;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Shop s = d.shops.get(p.getUUID());
            if (s != null && s.seenRunDay != d.runDays) {
                s.seenRunDay = d.runDays;
                s.ownerName = p.getGameProfile().getName();
                dirty = true;
            }
        }
        int close = EconomySettings.shopCloseDays();
        for (Shop s : new ArrayList<>(d.shops.values())) {
            if (!ShopRules.dissolve(ShopRules.absent(d.runDays, s.seenRunDay), close)) continue;
            dissolve(d, s);
            d.shopNotices.add(s.owner);
            dirty = true;
        }
        long week = BountyMath.week(day);
        for (Shop s : d.shops.values()) {
            if (s.upkeepWeek == week) continue;
            upkeep(server, d, s, day, week);
            dirty = true;
        }
        return dirty;
    }

    /** The upkeep of a new market week from the owner's coin account; offers that cannot be paid pause (unpaid). */
    static void upkeep(MinecraftServer server, ExchangeData d, Shop s, long day, long week) {
        s.upkeepWeek = week;
        long each = EconomySettings.shopUpkeepPerWeek();
        if (each <= 0) { for (Offer o : s.offers) o.unpaid = false; return; }
        ExchangeData.Mailbox box = d.mailbox(s.owner);
        long paid = 0;
        for (Offer o : s.offers) {
            if (box.coins >= each) {
                box.coins -= each;
                paid += each;
                o.unpaid = false;
            } else {
                o.unpaid = true;
            }
        }
        ExchangeService.toReserve(server, d, day, paid);
    }

    /** Everything of a shop to its owner's mailbox (goods, coins of BUY offers, proceeds); the shop is gone. */
    static void dissolve(ExchangeData d, Shop s) {
        ExchangeData.Mailbox box = d.mailbox(s.owner);
        for (Offer o : s.offers) returnStock(d, s.owner, o);
        for (Map.Entry<String, Long> e : s.proceeds.entrySet()) ExchangeService.addItems(box, e.getKey(), e.getValue());
        s.offers.clear();
        s.proceeds.clear();
        d.shops.remove(s.owner);
    }

    static void returnStock(ExchangeData d, UUID owner, Offer o) {
        if (o.kind == Kind.BUY) ExchangeService.credit(d, owner, Currency.COIN, o.stockCoins());
        else ExchangeService.addItems(d.mailbox(owner), o.goods, o.stockItems());
        o.stock = 0;
    }

    /** At login: the owner is here (absence 0); a shop closed while away is told. */
    public static void onLogin(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        Shop s = d.shops.get(player.getUUID());
        if (s != null && s.seenRunDay != d.runDays) {
            s.seenRunDay = d.runDays;
            d.setDirty();
        }
        if (d.shopNotices.remove(player.getUUID())) {
            d.setDirty();
            player.sendSystemMessage(Component.translatable("message.mio_icif.exchange.shop_closed_notice"));
        }
    }

    // ------------------------------------------------------------------ the owner's actions

    /**
     * Posts an offer of {@code player}'s shop (the shop opens with its first offer): the offer fee and the first stock
     * ({@code units}: goods from the inventory, or for BUY the coins) are taken at once.
     */
    public static Result post(ServerPlayer player, Kind kind, String goodsId, int goodsCount, String costId, int costCount, long price, long units) {
        if (!EconomySettings.shopsEnabled()) return Result.fail("shops_off");
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        if (kind == Kind.BUY) { goodsId = ""; goodsCount = 0; }
        if (kind == Kind.SELL) { costId = ""; costCount = 0; }
        if (kind == Kind.EXCHANGE) price = 0;
        if (ShopRules.check(kind, goodsId, goodsCount, costId, costCount, price) != null) return Result.fail("bad_offer");
        Item goods = kind == Kind.BUY ? null : goods(goodsId);
        Item cost = kind == Kind.SELL ? null : goods(costId);
        if (kind != Kind.BUY && goods == null || kind != Kind.SELL && cost == null) return Result.fail("bad_item");
        if (units < 1 || units > ShopRules.MAX_UNITS) return Result.fail("bad_quantity");
        Shop s = d.shops.get(player.getUUID());
        if (s == null && d.shops.size() >= ShopRules.MAX_SHOPS) return Result.fail("board_full");
        if (s != null && s.offers.size() >= EconomySettings.shopMaxOffers()) return Result.fail("shop_offers_full", EconomySettings.shopMaxOffers());
        Shop shop = s != null ? s : newShop(player, d);
        Offer o = new Offer();
        o.kind = kind;
        o.goods = kind == Kind.BUY ? "" : goodsId;
        o.goodsCount = goodsCount;
        o.cost = kind == Kind.SELL ? "" : costId;
        o.costCount = costCount;
        o.price = price;
        if (ShopRules.maxStock(shop, o, units, EconomySettings.shopStacks(), MAX_STACK) < units) return Result.fail("shop_storage_full");
        long fee = EconomySettings.shopOfferFee();
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (kind == Kind.BUY) {
            long coins = Math.multiplyExact(units, price);
            if (!ExchangeService.takeCoins(plan, d, player.getUUID(), Math.addExact(coins, fee))) return Result.fail("not_enough_coins");
        } else {
            if (!plan.removeMain(goods, Math.multiplyExact(units, (long) goodsCount), true)) return Result.fail("not_enough_items");
            if (!ExchangeService.takeCoins(plan, d, player.getUUID(), fee)) return Result.fail("not_enough_coins");
        }
        plan.commit();
        o.id = d.newId();
        o.stock = units;
        shop.offers.add(o);
        d.shops.putIfAbsent(player.getUUID(), shop);
        ExchangeService.toReserve(server, d, ExchangeService.today(server), fee);
        d.setDirty();
        return Result.done("shop_posted", units);
    }

    static Shop newShop(ServerPlayer player, ExchangeData d) {
        Shop s = new Shop();
        s.owner = player.getUUID();
        s.ownerName = player.getGameProfile().getName();
        s.seenRunDay = d.runDays;
        s.upkeepWeek = BountyMath.week(ExchangeService.today(player.server));   // the offer fee pays the first week
        return s;
    }

    /** More stock for an own offer ({@code units}: goods from the inventory; BUY: coins). */
    public static Result restock(ServerPlayer player, long offerId, long units) {
        ExchangeData d = ExchangeData.get(player.server);
        Shop s = d.shops.get(player.getUUID());
        Offer o = s == null ? null : s.offer(offerId);
        if (o == null) return Result.fail("no_offer");
        if (units < 1 || units > ShopRules.MAX_UNITS - o.stock) return Result.fail("bad_quantity");
        if (ShopRules.maxStock(s, o, units, EconomySettings.shopStacks(), MAX_STACK) < units) return Result.fail("shop_storage_full");
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (o.kind == Kind.BUY) {
            if (!ExchangeService.takeCoins(plan, d, player.getUUID(), Math.multiplyExact(units, o.price))) return Result.fail("not_enough_coins");
        } else {
            Item goods = goods(o.goods);
            if (goods == null || !plan.removeMain(goods, Math.multiplyExact(units, (long) o.goodsCount), true)) return Result.fail("not_enough_items");
        }
        plan.commit();
        o.stock += units;
        d.setDirty();
        return Result.done("shop_restocked", units);
    }

    /** Takes an own offer down: its stock goes back to the mailbox. */
    public static Result withdraw(ServerPlayer player, long offerId) {
        ExchangeData d = ExchangeData.get(player.server);
        Shop s = d.shops.get(player.getUUID());
        Offer o = s == null ? null : s.offer(offerId);
        if (o == null) return Result.fail("no_offer");
        returnStock(d, player.getUUID(), o);
        s.offers.remove(o);
        if (s.isEmpty()) d.shops.remove(player.getUUID());
        d.setDirty();
        return Result.done("shop_withdrawn");
    }

    /** Pauses or resumes an own offer; resuming an offer whose upkeep was not paid pays this week's upkeep now. */
    public static Result pause(ServerPlayer player, long offerId, boolean paused) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        Shop s = d.shops.get(player.getUUID());
        Offer o = s == null ? null : s.offer(offerId);
        if (o == null) return Result.fail("no_offer");
        if (!paused && o.unpaid) {
            long each = EconomySettings.shopUpkeepPerWeek();
            InventoryPlan plan = new InventoryPlan(player.getInventory());
            if (!ExchangeService.takeCoins(plan, d, player.getUUID(), each)) return Result.fail("not_enough_coins");
            plan.commit();
            ExchangeService.toReserve(server, d, ExchangeService.today(server), each);
            o.unpaid = false;
        }
        o.paused = paused;
        d.setDirty();
        return Result.done(paused ? "shop_paused" : "shop_resumed");
    }

    /** The proceeds box of the own shop into the inventory (the rest to the mailbox). */
    public static Result collect(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        Shop s = d.shops.get(player.getUUID());
        if (s == null || s.proceeds.isEmpty()) return Result.fail("shop_no_proceeds");
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        long n = 0;
        for (Map.Entry<String, Long> e : s.proceeds.entrySet()) {
            Item item = ExchangeService.item(e.getKey());
            if (item == null) ExchangeService.addItems(d.mailbox(player.getUUID()), e.getKey(), e.getValue());
            else ExchangeService.give(plan, d, player.getUUID(), item, e.getKey(), e.getValue());
            n += e.getValue();
        }
        plan.commit();
        s.proceeds.clear();
        if (s.isEmpty()) d.shops.remove(player.getUUID());
        d.setDirty();
        return Result.done("shop_collected", n);
    }

    // ------------------------------------------------------------------ the customer's trade

    /** Units of {@code o} the customer can give for now ({@code from}: the items' source; coins: inventory and account). */
    static long afford(ExchangeData d, UUID customer, InventoryPlan plan, ItemSource from, Offer o) {
        if (o.kind == Kind.SELL) {
            long coins = plan.count(coin(), false) + d.mailbox(customer).coins;
            return o.price <= 0 ? 0 : coins / o.price;
        }
        Item cost = goods(o.cost);
        return cost == null || o.costCount <= 0 ? 0 : from.count(cost) / o.costCount;
    }

    /** Units {@code viewer} may take of {@code o} now (for the view). */
    static long maxNow(ServerPlayer viewer, ExchangeData d, Shop s, Offer o) {
        if (viewer.getUUID().equals(s.owner)) return 0;
        InventoryPlan plan = new InventoryPlan(viewer.getInventory());
        return ShopRules.maxUnits(s, o, Long.MAX_VALUE, afford(d, viewer.getUUID(), plan, ItemSource.of(plan), o), EconomySettings.shopStacks(), MAX_STACK);
    }

    /** Takes {@code units} (at most; whole units) of offer {@code offerId} of {@code owner}'s shop. */
    public static Result trade(ServerPlayer player, String owner, long offerId, long units) {
        return trade(player, owner, offerId, units, null);
    }

    /** {@link #trade} with the cost items from {@code from} (a settlement container; null = the inventory). */
    public static Result trade(ServerPlayer player, String owner, long offerId, long units, ItemSource from) {
        if (!EconomySettings.shopsEnabled()) return Result.fail("shops_off");
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        UUID ownerId;
        try { ownerId = UUID.fromString(owner); } catch (IllegalArgumentException e) { return Result.fail("no_offer"); }
        Shop s = d.shops.get(ownerId);
        Offer o = s == null ? null : s.offer(offerId);
        if (o == null) return Result.fail("no_offer");
        if (ownerId.equals(player.getUUID())) return Result.fail("shop_own");
        if (!o.tradable()) return Result.fail(o.stock <= 0 ? "shop_sold_out" : "shop_offer_paused");
        if (units < 1) return Result.fail("bad_quantity");
        UUID me = player.getUUID();
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        ItemSource src = from != null ? from : ItemSource.of(plan);
        long afford = afford(d, me, plan, src, o);
        long k = ShopRules.maxUnits(s, o, units, afford, EconomySettings.shopStacks(), MAX_STACK);
        if (k < 1) {
            if (afford < 1) return Result.fail(o.kind == Kind.SELL ? "not_enough_coins" : "not_enough_items");
            return Result.fail("shop_storage_full");
        }
        long day = ExchangeService.today(server);
        double rate = EconomySettings.exchangeFee();
        switch (o.kind) {
            case EXCHANGE -> {
                Item cost = goods(o.cost), goods = goods(o.goods);
                if (cost == null || goods == null || !src.take(cost, k * o.costCount)) return Result.fail("not_enough_items");
                ExchangeService.give(plan, d, me, goods, o.goods, k * o.goodsCount);
                s.proceeds.merge(o.cost, k * o.costCount, Long::sum);
            }
            case SELL -> {
                Item goods = goods(o.goods);
                long total = Math.multiplyExact(k, o.price), fee = ShopRules.fee(total, rate);
                if (goods == null || !ExchangeService.takeCoins(plan, d, me, total)) return Result.fail("not_enough_coins");
                ExchangeService.give(plan, d, me, goods, o.goods, k * o.goodsCount);
                ExchangeService.credit(d, ownerId, Currency.COIN, total - fee);
                ExchangeService.toReserve(server, d, day, fee);
                d.records.add(me, day, MarketRecords.TRADE_OUT, total);
                d.records.add(ownerId, day, MarketRecords.TRADE_IN, total - fee);
            }
            case BUY -> {
                Item cost = goods(o.cost);
                long total = Math.multiplyExact(k, o.price), fee = ShopRules.fee(total, rate);
                if (cost == null || !src.take(cost, k * o.costCount)) return Result.fail("not_enough_items");
                ExchangeService.pay(plan, d, me, Currency.COIN, total - fee);
                s.proceeds.merge(o.cost, k * o.costCount, Long::sum);
                ExchangeService.toReserve(server, d, day, fee);
                d.records.add(ownerId, day, MarketRecords.TRADE_OUT, total);
                d.records.add(me, day, MarketRecords.TRADE_IN, total - fee);
            }
        }
        if (!plan.unchanged()) throw new IllegalStateException("inventory changed during a shop trade");
        plan.commit();
        o.stock -= k;
        o.trades += k;
        s.lastTradeDay = day;
        d.setDirty();
        Bounties.seen(player);
        return Result.done("shop_traded", k);
    }

    // ------------------------------------------------------------------ the view

    /**
     * The shop part of the panel's view for {@code query}: "" nothing; "*" the list; "*item" the list of shops that trade the
     * item; an owner's UUID: the list and that shop's offers. The player's own shop summary is always there.
     */
    static ExchangeView.ShopView view(ServerPlayer player, ExchangeData d, String query) {
        boolean enabled = EconomySettings.shopsEnabled();
        String q = query == null ? "" : query;
        List<ExchangeView.ShopLine> list = new ArrayList<>();
        int idleDays = EconomySettings.shopIdleDays();
        if (!q.isEmpty()) {
            String filter = q.startsWith("*") ? q.substring(1) : "";
            List<Shop> shops = new ArrayList<>(d.shops.values());
            shops.sort((a, b) -> ShopRules.compare(a, b, d.runDays, idleDays));
            for (Shop s : shops) {
                if (list.size() >= ExchangeView.MAX_SHOPS) break;
                if (!filter.isEmpty() && !ShopRules.trades(s, filter)) continue;
                int open = 0;
                for (Offer o : s.offers) if (o.tradable()) open++;
                list.add(new ExchangeView.ShopLine(s.owner.toString(), s.ownerName, s.offers.size(), open,
                    ShopRules.idle(ShopRules.absent(d.runDays, s.seenRunDay), idleDays), s.lastTradeDay == Long.MIN_VALUE ? -1 : s.lastTradeDay,
                    s.owner.equals(player.getUUID())));
            }
        }
        String openOwner = "", openName = "";
        boolean openIdle = false;
        List<ExchangeView.OfferLine> offers = new ArrayList<>();
        if (!q.isEmpty() && !q.startsWith("*")) {
            Shop s = null;
            try { s = d.shops.get(UUID.fromString(q)); } catch (IllegalArgumentException ignored) { }
            if (s != null) {
                openOwner = s.owner.toString();
                openName = s.ownerName;
                openIdle = ShopRules.idle(ShopRules.absent(d.runDays, s.seenRunDay), idleDays);
                for (Offer o : s.offers) {
                    if (offers.size() >= ExchangeView.MAX_OFFERS) break;
                    offers.add(line(o, maxNow(player, d, s, o)));
                }
            }
        }
        Shop mine = d.shops.get(player.getUUID());
        List<ExchangeView.OfferLine> myOffers = new ArrayList<>();
        List<ExchangeView.MailLine> proceeds = new ArrayList<>();
        long stacks = 0;
        if (mine != null) {
            for (Offer o : mine.offers) if (myOffers.size() < ExchangeView.MAX_OFFERS) myOffers.add(line(o, 0));
            for (Map.Entry<String, Long> e : mine.proceeds.entrySet())
                if (proceeds.size() < ExchangeView.MAX_PROCEEDS) proceeds.add(new ExchangeView.MailLine(e.getKey(), e.getValue()));
            stacks = ShopRules.storage(mine, MAX_STACK);
        }
        return new ExchangeView.ShopView(enabled, list, openOwner, openName, openIdle, offers, myOffers, proceeds, stacks,
            EconomySettings.shopStacks(), EconomySettings.shopMaxOffers(), EconomySettings.shopOfferFee(), EconomySettings.shopUpkeepPerWeek(),
            !d.quietCommissions.contains(player.getUUID()), EconomySettings.commissionDefaultHours(), EconomySettings.commissionMaxHours());
    }

    static ExchangeView.OfferLine line(Offer o, long maxNow) {
        return new ExchangeView.OfferLine(o.id, o.kind.ordinal(), o.goods, o.goodsCount, o.cost, o.costCount, o.price, o.stock,
            o.paused, o.unpaid, maxNow, o.trades);
    }
}
