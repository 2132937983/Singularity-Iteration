// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 0.1.10.21 (futures M2-M5): the state of the SI exchange, one per server (overworld storage
 * {@code mio_icif_exchange}). It only stores; {@link ExchangeService} holds the rules.
 *
 * <ul>
 *   <li>Order books per commodity (resting limit orders; their escrow is the order itself: a buy holds price x
 *       remaining of the currency, a sell holds the remaining items).</li>
 *   <li>Treasury: the items the market owns (it sells only these, plus a small daily import).</li>
 *   <li>Mailboxes per player: items, coins and UU-matter (mB) that trades, commissions and settlements owe.</li>
 *   <li>Commissions, player listings and their demand evidence, trades, daily bars, the coin ledger, futures positions.</li>
 * </ul>
 * Every collection is bounded (see the limits in {@link ExchangeService}).
 */
public final class ExchangeData extends SavedData {
    public static final String NAME = "mio_icif_exchange";

    public enum Currency { COIN, UU }

    public enum RewardKind { COIN, ITEM, UU }

    public static final class Book {
        public final String commodity;
        public Currency currency;
        public final OrderBook book = new OrderBook();
        public final Map<Long, String> names = new HashMap<>();
        /** 0.1.10.35: the game time each resting order ends (orders of older versions have none: 7 market days). */
        public final Map<Long, Long> expires = new HashMap<>();
        Book(String commodity, Currency currency) { this.commodity = commodity; this.currency = currency; }
    }

    /** One trade. {@code maker}/{@code taker} are player UUIDs or {@code "system"}. */
    public record Trade(long day, long time, long price, int quantity, String maker, String taker) { }

    public static final class Bar {
        public long day, open, high, low, close, volume;
    }

    public static final class Commission {
        public long id;
        public UUID requester;
        public String requesterName = "";
        public String item;
        public int quantity, delivered;
        public RewardKind kind;
        public String rewardItem = "";
        public long rewardTotal, rewardPaid;
        public long day, expiry;
        /** 0.1.10.35: the game time the commission ends (0 = an older commission: it ends with the market day {@link #expiry}). */
        public long expiresAt;
        public boolean system;
        /** The requester met the play-time minimum when posting (counts as demand evidence). */
        public boolean counted;
        public final Set<UUID> deliverers = new HashSet<>();
    }

    public static final class Listing {
        public String item;
        public Currency currency;
        public long reference, listedDay, lastTradeDay;
        /** 0.1.10.26: listed by a datapack ({@code si_exchange/listings}); never delisted for lack of trades while it is there. */
        public boolean datapack;
    }

    public static final class LedgerDay {
        public long day, minted, burned;
        /** 0.1.10.26 (X3): of {@link #minted}, the coins bounties paid; items and UU-matter (mB) bounties created ("issue"). */
        public long bountyCoins, bountyItems, bountyUu;
        /** 0.1.10.26: coins that the Java API paid into ({@code [0]}) or took out of ({@code [1]}) mailboxes, per reason code. */
        public final Map<String, long[]> external = new java.util.TreeMap<>();
        /** 0.1.10.28 (X4): items the scrap sink took and the products (scrap) it gave. */
        public long scrapIn, scrapOut;
        /**
         * 0.1.10.35: coins of fees that went into the treasury reserve, and coins the reserve paid (bounty rewards). {@link #minted}
         * counts only new coins and {@link #burned} only destroyed ones; the change of circulation is
         * {@link ReserveRules#circulation}.
         */
        public long reserveIn, reserveOut;

        /** 0.1.10.35: the change of the coins in circulation on this day. */
        public long circulation() { return ReserveRules.circulation(minted, burned, reserveIn, reserveOut); }

        /** 0.1.10.35: coins that came into circulation: new ones and the reserve's payments (before 0.1.10.35: {@link #minted}). */
        public long created() { return minted + reserveOut; }

        /** 0.1.10.35: coins that left circulation: destroyed ones and the fees into the reserve (before 0.1.10.35: {@link #burned}). */
        public long destroyed() { return burned + reserveIn; }
    }

    /** 0.1.10.28 (X4): a delegate ID card in its issuer's register (a card not in the register is revoked). */
    public static final class CardRecord {
        public long serial;
        public UUID holder;
        public String holderName = "";
        public int level;
        public long expiry;
    }

    public static final class Position {
        public long id;
        public UUID owner;
        public String ownerName = "";
        public String commodity;
        public boolean isLong;
        public long price;
        public int quantity;
        public long openDay, expiry;
    }

    public static final class Mailbox {
        public final Map<String, Long> items = new LinkedHashMap<>();
        public long coins, uu;

        public boolean isEmpty() { return items.isEmpty() && coins == 0 && uu == 0; }
    }

    /** 0.1.10.25 (X1): an order of the daily auction; its items are held until the market day changes. */
    public static final class AuctionOrder {
        public long id;
        public UUID owner;
        public String ownerName = "";
        public String item = "";
        public long quantity;
        /** The lowest average price the seller accepts, in percent of the bid without pressure on the clearing day (0 = any). */
        public int minPercent;
        public long day;
    }

    public long nextId = 1;
    public final Map<String, Book> books = new LinkedHashMap<>();
    public final Map<String, Long> treasury = new HashMap<>();
    public final Map<String, Integer> imported = new HashMap<>();
    public long importedDay = Long.MIN_VALUE;
    public final Map<UUID, Mailbox> mail = new HashMap<>();
    public final List<Commission> commissions = new ArrayList<>();
    public final Map<String, Listing> listings = new LinkedHashMap<>();
    public final Map<String, List<ListingRules.Evidence>> evidence = new HashMap<>();
    public final Map<String, ArrayDeque<Trade>> trades = new HashMap<>();
    public final Map<String, ArrayDeque<Bar>> bars = new HashMap<>();
    public final ArrayDeque<LedgerDay> ledger = new ArrayDeque<>();
    public final List<Position> positions = new ArrayList<>();
    public long lastDay = Long.MIN_VALUE;
    /** 0.1.10.25 (X1): items sold to the market that it has not passed on yet, per commodity; a share leaves each day. */
    public final Map<String, Double> pressure = new HashMap<>();
    public long pressureDay = Long.MIN_VALUE;
    /** 0.1.10.25: items each player sold to the market today, per commodity (the personal depth), for day {@link #soldDay}. */
    public final Map<UUID, Map<String, Long>> sold = new HashMap<>();
    public long soldDay = Long.MIN_VALUE;
    /** 0.1.10.25: coins each player may still receive from the market: {balance, day of the balance}. */
    public final Map<UUID, long[]> allowance = new HashMap<>();
    /** 0.1.10.25: the open orders of the daily auction. */
    public final List<AuctionOrder> auction = new ArrayList<>();
    /** 0.1.10.26 (X3): the system bounties, the vault and the weekly budgets. */
    public final BountyBoard board = new BountyBoard();
    /** 0.1.10.28 (X4): the stabiliser's factor of the sell depths and the allowance, the money supply estimate, the closed day counted. */
    public double stabFactor = 1.0;
    public long supply;
    public long supplyDay = Long.MIN_VALUE;
    /** 0.1.10.28: the last day each player sold to the market (active sellers of the stabiliser). */
    public final Map<UUID, Long> lastSale = new HashMap<>();
    /** 0.1.10.34: the market day each player last logged in or used the exchange (active players of the bounties). */
    public final Map<UUID, Long> seen = new HashMap<>();
    /** 0.1.10.28: the secret that signs ID cards (0 = not made yet), the next serial, the delegate cards per issuer. */
    public long cardSecret;
    public long cardSerial = 1;
    public final Map<UUID, List<CardRecord>> cards = new HashMap<>();
    /** 0.1.10.29: players whose coin account comes first (payments from it first, receipts onto it). */
    public final Set<UUID> bankMode = new HashSet<>();
    /** 0.1.10.38: first market day the Portable Futures Trader's data plan no longer covers, per player. */
    public final Map<UUID, Long> dataPlanUntil = new HashMap<>();
    /** 0.1.10.33: player results per week and the cost basis of the treasury stock (recorded, not shown yet). */
    public final MarketRecords records = new MarketRecords();
    /** 0.1.10.35: the treasury's coin reserve (fees; it pays bounty coins first). */
    public long reserve;
    /** 0.1.10.35: players who turned the chat notice of new commissions off. */
    public final Set<UUID> quietCommissions = new HashSet<>();
    /** 0.1.10.35: calendar days on which the server ran, and the last of them (epoch day; the absence of shop owners). */
    public long runDays;
    public long lastRunDate = Long.MIN_VALUE;
    /** 0.1.10.35: the player shops, by owner. */
    public final Map<UUID, ShopRules.Shop> shops = new LinkedHashMap<>();
    /** 0.1.10.35: owners whose shop closed while they were away (told at the next login). */
    public final Set<UUID> shopNotices = new HashSet<>();

    public static ExchangeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ExchangeData::new, ExchangeData::load), NAME);
    }

    public long newId() {
        setDirty();
        return nextId++;
    }

    public Book book(String commodity, Currency currency) {
        Book b = books.computeIfAbsent(commodity, c -> new Book(c, currency));
        b.currency = currency;
        return b;
    }

    public Mailbox mailbox(UUID player) {
        return mail.computeIfAbsent(player, p -> new Mailbox());
    }

    /** 0.1.10.25: the items {@code player} sold to the market on {@code day}, per commodity (a new day starts empty). */
    public Map<String, Long> sold(UUID player, long day) {
        if (soldDay != day) {
            sold.clear();
            soldDay = day;
            setDirty();
        }
        return sold.computeIfAbsent(player, p -> new HashMap<>());
    }

    public LedgerDay ledger(long day) {
        LedgerDay last = ledger.peekLast();
        if (last != null && last.day == day) return last;
        LedgerDay next = new LedgerDay();
        next.day = day;
        ledger.addLast(next);
        while (ledger.size() > ExchangeService.LEDGER_DAYS) ledger.removeFirst();
        return next;
    }

    // ------------------------------------------------------------------ persistence

    public static ExchangeData load(CompoundTag tag, HolderLookup.Provider provider) {
        ExchangeData d = new ExchangeData();
        d.nextId = Math.max(1, tag.getLong("NextId"));
        d.lastDay = tag.contains("LastDay") ? tag.getLong("LastDay") : Long.MIN_VALUE;
        d.importedDay = tag.contains("ImportedDay") ? tag.getLong("ImportedDay") : Long.MIN_VALUE;
        for (Tag t : tag.getList("Books", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            Book book = d.book(b.getString("Commodity"), currency(b.getString("Currency")));
            for (Tag o : b.getList("Orders", Tag.TAG_COMPOUND)) {
                CompoundTag order = (CompoundTag) o;
                try {
                    book.book.rest(new OrderBook.Order(order.getLong("Id"), order.getString("Owner"), order.getString("Team"),
                        order.getBoolean("Buy"), order.getLong("Price"), order.getInt("Remaining"), order.getLong("Day")));
                    book.names.put(order.getLong("Id"), order.getString("Name"));
                    if (order.getLong("Expires") > 0) book.expires.put(order.getLong("Id"), order.getLong("Expires"));   // 0.1.10.35
                } catch (IllegalArgumentException ignored) {
                    // a damaged order is dropped (its escrow is lost with it; it cannot be valid)
                }
            }
        }
        readLongMap(tag.getCompound("Treasury"), d.treasury);
        CompoundTag imp = tag.getCompound("Imported");
        for (String k : imp.getAllKeys()) d.imported.put(k, Math.max(0, imp.getInt(k)));
        CompoundTag mail = tag.getCompound("Mail");
        for (String k : mail.getAllKeys()) {
            UUID id = uuid(k);
            if (id == null) continue;
            CompoundTag m = mail.getCompound(k);
            Mailbox box = d.mailbox(id);
            box.coins = Math.max(0, m.getLong("Coins"));
            box.uu = Math.max(0, m.getLong("Uu"));
            readLongMap(m.getCompound("Items"), box.items);
        }
        for (Tag t : tag.getList("Commissions", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Commission x = new Commission();
            x.id = c.getLong("Id"); x.requester = uuid(c.getString("Requester")); x.requesterName = c.getString("Name");
            x.item = c.getString("Item"); x.quantity = c.getInt("Quantity"); x.delivered = c.getInt("Delivered");
            x.kind = reward(c.getString("Kind")); x.rewardItem = c.getString("RewardItem"); x.rewardTotal = c.getLong("RewardTotal");
            x.rewardPaid = c.getLong("RewardPaid"); x.day = c.getLong("Day"); x.expiry = c.getLong("Expiry"); x.system = c.getBoolean("System");
            x.counted = c.getBoolean("Counted");
            x.expiresAt = Math.max(0, c.getLong("ExpiresAt"));   // 0.1.10.35
            for (Tag u : c.getList("Deliverers", Tag.TAG_STRING)) { UUID id = uuid(u.getAsString()); if (id != null) x.deliverers.add(id); }
            if ((x.requester != null || x.system) && x.quantity > 0 && x.delivered >= 0 && x.delivered <= x.quantity) d.commissions.add(x);
        }
        for (Tag t : tag.getList("Listings", Tag.TAG_COMPOUND)) {
            CompoundTag l = (CompoundTag) t;
            Listing x = new Listing();
            x.item = l.getString("Item"); x.currency = currency(l.getString("Currency")); x.reference = Math.max(1, l.getLong("Reference"));
            x.listedDay = l.getLong("ListedDay"); x.lastTradeDay = l.getLong("LastTradeDay");
            x.datapack = l.getBoolean("Datapack");
            d.listings.put(x.item, x);
        }
        CompoundTag ev = tag.getCompound("Evidence");
        for (String item : ev.getAllKeys()) {
            List<ListingRules.Evidence> list = new ArrayList<>();
            for (Tag t : ev.getList(item, Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) t;
                Set<String> by = new HashSet<>();
                for (Tag u : e.getList("By", Tag.TAG_STRING)) by.add(u.getAsString());
                list.add(new ListingRules.Evidence(e.getLong("Day"), e.getString("Requester"), Set.copyOf(by), e.getLong("Unit"), e.getLong("Value")));
            }
            d.evidence.put(item, list);
        }
        CompoundTag tr = tag.getCompound("Trades");
        for (String item : tr.getAllKeys()) {
            ArrayDeque<Trade> q = new ArrayDeque<>();
            for (Tag t : tr.getList(item, Tag.TAG_COMPOUND)) {
                CompoundTag x = (CompoundTag) t;
                q.addLast(new Trade(x.getLong("Day"), x.getLong("Time"), x.getLong("Price"), x.getInt("Quantity"), x.getString("Maker"), x.getString("Taker")));
            }
            d.trades.put(item, q);
        }
        CompoundTag br = tag.getCompound("Bars");
        for (String item : br.getAllKeys()) {
            ArrayDeque<Bar> q = new ArrayDeque<>();
            for (Tag t : br.getList(item, Tag.TAG_COMPOUND)) {
                CompoundTag x = (CompoundTag) t;
                Bar bar = new Bar();
                bar.day = x.getLong("Day"); bar.open = x.getLong("O"); bar.high = x.getLong("H"); bar.low = x.getLong("L"); bar.close = x.getLong("C");
                bar.volume = x.getLong("V");
                q.addLast(bar);
            }
            d.bars.put(item, q);
        }
        for (Tag t : tag.getList("Ledger", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            LedgerDay l = new LedgerDay();
            l.day = x.getLong("Day"); l.minted = x.getLong("Minted"); l.burned = x.getLong("Burned");
            l.bountyCoins = x.getLong("BountyCoins"); l.bountyItems = x.getLong("BountyItems"); l.bountyUu = x.getLong("BountyUu");
            l.scrapIn = x.getLong("ScrapIn"); l.scrapOut = x.getLong("ScrapOut");
            l.reserveIn = Math.max(0, x.getLong("ReserveIn")); l.reserveOut = Math.max(0, x.getLong("ReserveOut"));   // 0.1.10.35
            CompoundTag ext = x.getCompound("External");
            for (String k : ext.getAllKeys()) { long[] v = ext.getLongArray(k); if (v.length == 2) l.external.put(k, v); }
            d.ledger.addLast(l);
        }
        for (Tag t : tag.getList("Positions", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            Position p = new Position();
            p.id = x.getLong("Id"); p.owner = uuid(x.getString("Owner")); p.ownerName = x.getString("Name"); p.commodity = x.getString("Commodity");
            p.isLong = x.getBoolean("Long"); p.price = x.getLong("Price"); p.quantity = x.getInt("Quantity"); p.openDay = x.getLong("Open");
            p.expiry = x.getLong("Expiry");
            if (p.owner != null && p.price > 0 && p.quantity > 0) d.positions.add(p);
        }
        // 0.1.10.25 (X1)
        CompoundTag pr = tag.getCompound("Pressure");
        for (String k : pr.getAllKeys()) { double v = pr.getDouble(k); if (v > 0 && Double.isFinite(v)) d.pressure.put(k, v); }
        d.pressureDay = tag.contains("PressureDay") ? tag.getLong("PressureDay") : Long.MIN_VALUE;
        d.soldDay = tag.contains("SoldDay") ? tag.getLong("SoldDay") : Long.MIN_VALUE;
        CompoundTag so = tag.getCompound("Sold");
        for (String k : so.getAllKeys()) {
            UUID id = uuid(k);
            if (id == null) continue;
            Map<String, Long> m = new HashMap<>();
            readLongMap(so.getCompound(k), m);
            if (!m.isEmpty()) d.sold.put(id, m);
        }
        CompoundTag al = tag.getCompound("Allowance");
        for (String k : al.getAllKeys()) {
            UUID id = uuid(k);
            long[] v = al.getLongArray(k);
            if (id != null && v.length == 2 && v[0] >= 0) d.allowance.put(id, v);
        }
        for (Tag t : tag.getList("Auction", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            AuctionOrder a = new AuctionOrder();
            a.id = x.getLong("Id"); a.owner = uuid(x.getString("Owner")); a.ownerName = x.getString("Name"); a.item = x.getString("Item");
            a.quantity = x.getLong("Quantity"); a.minPercent = Math.max(0, Math.min(100, x.getInt("MinPercent"))); a.day = x.getLong("Day");
            if (a.owner != null && a.quantity > 0 && !a.item.isEmpty()) d.auction.add(a);
        }
        // 0.1.10.26 (X3)
        BountyNbt.load(tag.getCompound("Bounties"), d.board);
        // 0.1.10.28 (X4)
        CompoundTag st = tag.getCompound("Stabilizer");
        double factor = st.contains("Factor") ? st.getDouble("Factor") : 1.0;
        d.stabFactor = Double.isFinite(factor) ? Math.max(0.05, Math.min(1.0, factor)) : 1.0;
        d.supply = Math.max(0, st.getLong("Supply"));
        d.supplyDay = st.contains("SupplyDay") ? st.getLong("SupplyDay") : Long.MIN_VALUE;
        CompoundTag ls = tag.getCompound("LastSale");
        for (String k : ls.getAllKeys()) { UUID id = uuid(k); if (id != null) d.lastSale.put(id, ls.getLong(k)); }
        // 0.1.10.34
        CompoundTag seen = tag.getCompound("Seen");
        for (String k : seen.getAllKeys()) { UUID id = uuid(k); if (id != null) d.seen.put(id, seen.getLong(k)); }
        // 0.1.10.29
        for (Tag t : tag.getList("BankMode", Tag.TAG_STRING)) { UUID id = uuid(t.getAsString()); if (id != null) d.bankMode.add(id); }
        // 0.1.10.38
        CompoundTag plans = tag.getCompound("DataPlan");
        for (String k : plans.getAllKeys()) { UUID id = uuid(k); if (id != null) d.dataPlanUntil.put(id, plans.getLong(k)); }
        CompoundTag cd = tag.getCompound("Cards");
        d.cardSecret = cd.getLong("Secret");
        d.cardSerial = Math.max(1, cd.getLong("Serial"));
        CompoundTag issuers = cd.getCompound("Issuers");
        for (String k : issuers.getAllKeys()) {
            UUID issuer = uuid(k);
            if (issuer == null) continue;
            List<CardRecord> list = new ArrayList<>();
            for (Tag t : issuers.getList(k, Tag.TAG_COMPOUND)) {
                CompoundTag x = (CompoundTag) t;
                CardRecord c = new CardRecord();
                c.serial = x.getLong("Serial"); c.holder = uuid(x.getString("Holder")); c.holderName = x.getString("Name");
                c.level = x.getInt("Level"); c.expiry = x.getLong("Expiry");
                if (c.holder != null) list.add(c);
            }
            if (!list.isEmpty()) d.cards.put(issuer, list);
        }
        // 0.1.10.33
        CompoundTag rec = tag.getCompound("Records");
        d.records.week = rec.contains("Week") ? rec.getLong("Week") : Long.MIN_VALUE;
        readStats(rec.getCompound("ThisWeek"), d.records.thisWeek);
        readStats(rec.getCompound("LastWeek"), d.records.lastWeek);
        readStats(rec.getCompound("Total"), d.records.total);
        CompoundTag cost = rec.getCompound("Cost");
        for (String k : cost.getAllKeys()) {
            CompoundTag x = cost.getCompound(k);
            double coins = x.getDouble("Coins"), stock = x.getDouble("Stock");
            if (Double.isFinite(coins) && Double.isFinite(stock) && stock > 0) d.records.cost.put(k, new double[] {Math.max(0, coins), stock});
        }
        // 0.1.10.35
        d.reserve = Math.max(0, tag.getLong("Reserve"));
        for (Tag t : tag.getList("QuietCommissions", Tag.TAG_STRING)) { UUID id = uuid(t.getAsString()); if (id != null) d.quietCommissions.add(id); }
        CompoundTag run = tag.getCompound("RunDays");
        d.runDays = Math.max(0, run.getLong("Days"));
        d.lastRunDate = run.contains("Last") ? run.getLong("Last") : Long.MIN_VALUE;
        for (Tag t : tag.getList("Shops", Tag.TAG_COMPOUND)) {
            ShopRules.Shop s = readShop((CompoundTag) t);
            if (s != null && d.shops.size() < ShopRules.MAX_SHOPS) d.shops.put(s.owner, s);
        }
        for (Tag t : tag.getList("ShopNotices", Tag.TAG_STRING)) { UUID id = uuid(t.getAsString()); if (id != null) d.shopNotices.add(id); }
        return d;
    }

    /** 0.1.10.35: a shop from NBT; damaged offers are dropped (an offer without its items cannot trade). */
    static ShopRules.Shop readShop(CompoundTag x) {
        ShopRules.Shop s = new ShopRules.Shop();
        s.owner = uuid(x.getString("Owner"));
        if (s.owner == null) return null;
        s.ownerName = x.getString("Name");
        s.seenRunDay = x.getLong("Seen");
        s.lastTradeDay = x.contains("LastTrade") ? x.getLong("LastTrade") : Long.MIN_VALUE;
        s.upkeepWeek = x.contains("UpkeepWeek") ? x.getLong("UpkeepWeek") : Long.MIN_VALUE;
        for (Tag t : x.getList("Offers", Tag.TAG_COMPOUND)) {
            CompoundTag o = (CompoundTag) t;
            ShopRules.Offer f = new ShopRules.Offer();
            f.id = o.getLong("Id");
            String kind = o.getString("Kind");
            f.kind = "SELL".equals(kind) ? ShopRules.Kind.SELL : "BUY".equals(kind) ? ShopRules.Kind.BUY : ShopRules.Kind.EXCHANGE;
            f.goods = o.getString("Goods"); f.goodsCount = o.getInt("GoodsCount");
            f.cost = o.getString("Cost"); f.costCount = o.getInt("CostCount");
            f.price = o.getLong("Price"); f.stock = Math.max(0, Math.min(ShopRules.MAX_UNITS, o.getLong("Stock")));
            f.paused = o.getBoolean("Paused"); f.unpaid = o.getBoolean("Unpaid"); f.trades = Math.max(0, o.getLong("Trades"));
            if (ShopRules.check(f.kind, f.goods, f.goodsCount, f.cost, f.costCount, f.price) == null && s.offers.size() < 54) s.offers.add(f);
        }
        readLongMap(x.getCompound("Proceeds"), s.proceeds);
        return s;
    }

    static CompoundTag writeShop(ShopRules.Shop s) {
        CompoundTag x = new CompoundTag();
        x.putString("Owner", s.owner.toString());
        x.putString("Name", s.ownerName);
        x.putLong("Seen", s.seenRunDay);
        x.putLong("LastTrade", s.lastTradeDay);
        x.putLong("UpkeepWeek", s.upkeepWeek);
        ListTag offers = new ListTag();
        for (ShopRules.Offer f : s.offers) {
            CompoundTag o = new CompoundTag();
            o.putLong("Id", f.id); o.putString("Kind", f.kind.name());
            o.putString("Goods", f.goods); o.putInt("GoodsCount", f.goodsCount);
            o.putString("Cost", f.cost); o.putInt("CostCount", f.costCount);
            o.putLong("Price", f.price); o.putLong("Stock", f.stock);
            if (f.paused) o.putBoolean("Paused", true);
            if (f.unpaid) o.putBoolean("Unpaid", true);
            o.putLong("Trades", f.trades);
            offers.add(o);
        }
        x.put("Offers", offers);
        x.put("Proceeds", writeLongMap(s.proceeds));
        return x;
    }

    private static void readStats(CompoundTag tag, Map<UUID, long[]> out) {
        for (String k : tag.getAllKeys()) {
            UUID id = uuid(k);
            long[] v = tag.getLongArray(k);
            if (id == null || out.size() >= MarketRecords.MAX_PLAYERS) continue;
            out.put(id, java.util.Arrays.copyOf(v, MarketRecords.FIELDS));
        }
    }

    private static CompoundTag writeStats(Map<UUID, long[]> map) {
        CompoundTag t = new CompoundTag();
        map.forEach((id, v) -> t.putLongArray(id.toString(), v));
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("NextId", nextId);
        tag.putLong("LastDay", lastDay);
        tag.putLong("ImportedDay", importedDay);
        ListTag books = new ListTag();
        for (Book b : this.books.values()) {
            CompoundTag x = new CompoundTag();
            x.putString("Commodity", b.commodity);
            x.putString("Currency", b.currency.name());
            ListTag orders = new ListTag();
            for (OrderBook.Order o : b.book.orders()) {
                CompoundTag order = new CompoundTag();
                order.putLong("Id", o.id()); order.putString("Owner", o.owner()); order.putString("Team", o.team());
                order.putBoolean("Buy", o.buy()); order.putLong("Price", o.price()); order.putInt("Remaining", o.remaining());
                order.putLong("Day", o.day()); order.putString("Name", b.names.getOrDefault(o.id(), ""));
                Long expires = b.expires.get(o.id());
                if (expires != null) order.putLong("Expires", expires);   // 0.1.10.35
                orders.add(order);
            }
            x.put("Orders", orders);
            books.add(x);
        }
        tag.put("Books", books);
        tag.put("Treasury", writeLongMap(treasury));
        CompoundTag imp = new CompoundTag();
        imported.forEach(imp::putInt);
        tag.put("Imported", imp);
        CompoundTag mail = new CompoundTag();
        this.mail.forEach((id, box) -> {
            if (box.isEmpty()) return;
            CompoundTag m = new CompoundTag();
            m.putLong("Coins", box.coins); m.putLong("Uu", box.uu); m.put("Items", writeLongMap(box.items));
            mail.put(id.toString(), m);
        });
        tag.put("Mail", mail);
        ListTag com = new ListTag();
        for (Commission c : commissions) {
            CompoundTag x = new CompoundTag();
            x.putLong("Id", c.id); x.putString("Requester", c.requester == null ? "" : c.requester.toString()); x.putString("Name", c.requesterName);
            x.putString("Item", c.item); x.putInt("Quantity", c.quantity); x.putInt("Delivered", c.delivered); x.putString("Kind", c.kind.name());
            x.putString("RewardItem", c.rewardItem); x.putLong("RewardTotal", c.rewardTotal); x.putLong("RewardPaid", c.rewardPaid);
            x.putLong("Day", c.day); x.putLong("Expiry", c.expiry); x.putBoolean("System", c.system); x.putBoolean("Counted", c.counted);
            if (c.expiresAt > 0) x.putLong("ExpiresAt", c.expiresAt);   // 0.1.10.35
            ListTag by = new ListTag();
            for (UUID u : c.deliverers) by.add(StringTag.valueOf(u.toString()));
            x.put("Deliverers", by);
            com.add(x);
        }
        tag.put("Commissions", com);
        ListTag lis = new ListTag();
        for (Listing l : listings.values()) {
            CompoundTag x = new CompoundTag();
            x.putString("Item", l.item); x.putString("Currency", l.currency.name()); x.putLong("Reference", l.reference);
            x.putLong("ListedDay", l.listedDay); x.putLong("LastTradeDay", l.lastTradeDay);
            if (l.datapack) x.putBoolean("Datapack", true);
            lis.add(x);
        }
        tag.put("Listings", lis);
        CompoundTag ev = new CompoundTag();
        evidence.forEach((item, list) -> {
            ListTag l = new ListTag();
            for (ListingRules.Evidence e : list) {
                CompoundTag x = new CompoundTag();
                x.putLong("Day", e.day()); x.putString("Requester", e.requester()); x.putLong("Unit", e.unitPrice()); x.putLong("Value", e.rewardValue());
                ListTag by = new ListTag();
                for (String u : e.deliverers()) by.add(StringTag.valueOf(u));
                x.put("By", by);
                l.add(x);
            }
            ev.put(item, l);
        });
        tag.put("Evidence", ev);
        CompoundTag tr = new CompoundTag();
        trades.forEach((item, q) -> {
            ListTag l = new ListTag();
            for (Trade t : q) {
                CompoundTag x = new CompoundTag();
                x.putLong("Day", t.day()); x.putLong("Time", t.time()); x.putLong("Price", t.price()); x.putInt("Quantity", t.quantity());
                x.putString("Maker", t.maker()); x.putString("Taker", t.taker());
                l.add(x);
            }
            tr.put(item, l);
        });
        tag.put("Trades", tr);
        CompoundTag br = new CompoundTag();
        bars.forEach((item, q) -> {
            ListTag l = new ListTag();
            for (Bar b : q) {
                CompoundTag x = new CompoundTag();
                x.putLong("Day", b.day); x.putLong("O", b.open); x.putLong("H", b.high); x.putLong("L", b.low); x.putLong("C", b.close); x.putLong("V", b.volume);
                l.add(x);
            }
            br.put(item, l);
        });
        tag.put("Bars", br);
        ListTag led = new ListTag();
        for (LedgerDay l : ledger) {
            CompoundTag x = new CompoundTag();
            x.putLong("Day", l.day); x.putLong("Minted", l.minted); x.putLong("Burned", l.burned);
            if (l.bountyCoins != 0) x.putLong("BountyCoins", l.bountyCoins);
            if (l.bountyItems != 0) x.putLong("BountyItems", l.bountyItems);
            if (l.bountyUu != 0) x.putLong("BountyUu", l.bountyUu);
            if (l.scrapIn != 0) x.putLong("ScrapIn", l.scrapIn);
            if (l.scrapOut != 0) x.putLong("ScrapOut", l.scrapOut);
            if (l.reserveIn != 0) x.putLong("ReserveIn", l.reserveIn);   // 0.1.10.35
            if (l.reserveOut != 0) x.putLong("ReserveOut", l.reserveOut);
            if (!l.external.isEmpty()) {
                CompoundTag ext = new CompoundTag();
                l.external.forEach(ext::putLongArray);
                x.put("External", ext);
            }
            led.add(x);
        }
        tag.put("Ledger", led);
        ListTag pos = new ListTag();
        for (Position p : positions) {
            CompoundTag x = new CompoundTag();
            x.putLong("Id", p.id); x.putString("Owner", p.owner.toString()); x.putString("Name", p.ownerName); x.putString("Commodity", p.commodity);
            x.putBoolean("Long", p.isLong); x.putLong("Price", p.price); x.putInt("Quantity", p.quantity); x.putLong("Open", p.openDay);
            x.putLong("Expiry", p.expiry);
            pos.add(x);
        }
        tag.put("Positions", pos);
        // 0.1.10.25 (X1)
        CompoundTag pr = new CompoundTag();
        pressure.forEach((k, v) -> { if (v > 0) pr.putDouble(k, v); });
        tag.put("Pressure", pr);
        tag.putLong("PressureDay", pressureDay);
        tag.putLong("SoldDay", soldDay);
        CompoundTag so = new CompoundTag();
        sold.forEach((id, m) -> { if (!m.isEmpty()) so.put(id.toString(), writeLongMap(m)); });
        tag.put("Sold", so);
        CompoundTag al = new CompoundTag();
        allowance.forEach((id, v) -> al.putLongArray(id.toString(), v));
        tag.put("Allowance", al);
        ListTag auc = new ListTag();
        for (AuctionOrder a : auction) {
            CompoundTag x = new CompoundTag();
            x.putLong("Id", a.id); x.putString("Owner", a.owner.toString()); x.putString("Name", a.ownerName); x.putString("Item", a.item);
            x.putLong("Quantity", a.quantity); x.putInt("MinPercent", a.minPercent); x.putLong("Day", a.day);
            auc.add(x);
        }
        tag.put("Auction", auc);
        // 0.1.10.26 (X3)
        tag.put("Bounties", BountyNbt.save(board));
        // 0.1.10.28 (X4)
        CompoundTag st = new CompoundTag();
        st.putDouble("Factor", stabFactor);
        st.putLong("Supply", supply);
        st.putLong("SupplyDay", supplyDay);
        tag.put("Stabilizer", st);
        CompoundTag ls = new CompoundTag();
        lastSale.forEach((id, dd) -> ls.putLong(id.toString(), dd));
        tag.put("LastSale", ls);
        // 0.1.10.34
        CompoundTag seenTag = new CompoundTag();
        seen.forEach((id, dd) -> seenTag.putLong(id.toString(), dd));
        tag.put("Seen", seenTag);
        // 0.1.10.29
        ListTag bank = new ListTag();
        for (UUID id : bankMode) bank.add(StringTag.valueOf(id.toString()));
        tag.put("BankMode", bank);
        // 0.1.10.38
        CompoundTag plans = new CompoundTag();
        dataPlanUntil.forEach((id, day) -> plans.putLong(id.toString(), day));
        tag.put("DataPlan", plans);
        CompoundTag cd = new CompoundTag();
        cd.putLong("Secret", cardSecret);
        cd.putLong("Serial", cardSerial);
        CompoundTag issuers = new CompoundTag();
        cards.forEach((issuer, list) -> {
            ListTag lt = new ListTag();
            for (CardRecord c : list) {
                CompoundTag x = new CompoundTag();
                x.putLong("Serial", c.serial); x.putString("Holder", c.holder.toString()); x.putString("Name", c.holderName);
                x.putInt("Level", c.level); x.putLong("Expiry", c.expiry);
                lt.add(x);
            }
            if (!lt.isEmpty()) issuers.put(issuer.toString(), lt);
        });
        cd.put("Issuers", issuers);
        tag.put("Cards", cd);
        // 0.1.10.33
        CompoundTag rec = new CompoundTag();
        rec.putLong("Week", records.week);
        rec.put("ThisWeek", writeStats(records.thisWeek));
        rec.put("LastWeek", writeStats(records.lastWeek));
        rec.put("Total", writeStats(records.total));
        CompoundTag cost = new CompoundTag();
        records.cost.forEach((k, v) -> {
            CompoundTag x = new CompoundTag();
            x.putDouble("Coins", v[0]);
            x.putDouble("Stock", v[1]);
            cost.put(k, x);
        });
        rec.put("Cost", cost);
        tag.put("Records", rec);
        // 0.1.10.35
        tag.putLong("Reserve", reserve);
        ListTag quiet = new ListTag();
        for (UUID id : quietCommissions) quiet.add(StringTag.valueOf(id.toString()));
        tag.put("QuietCommissions", quiet);
        CompoundTag run = new CompoundTag();
        run.putLong("Days", runDays);
        run.putLong("Last", lastRunDate);
        tag.put("RunDays", run);
        ListTag shopList = new ListTag();
        for (ShopRules.Shop s : shops.values()) shopList.add(writeShop(s));
        tag.put("Shops", shopList);
        ListTag notices = new ListTag();
        for (UUID id : shopNotices) notices.add(StringTag.valueOf(id.toString()));
        tag.put("ShopNotices", notices);
        return tag;
    }

    private static Currency currency(String s) { return "UU".equals(s) ? Currency.UU : Currency.COIN; }

    private static RewardKind reward(String s) {
        return switch (s) { case "ITEM" -> RewardKind.ITEM; case "UU" -> RewardKind.UU; default -> RewardKind.COIN; };
    }

    private static UUID uuid(String s) {
        try { return s == null || s.isEmpty() ? null : UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private static void readLongMap(CompoundTag tag, Map<String, Long> out) {
        for (String k : tag.getAllKeys()) { long v = tag.getLong(k); if (v > 0) out.put(k, v); }
    }

    private static CompoundTag writeLongMap(Map<String, Long> map) {
        CompoundTag t = new CompoundTag();
        map.forEach((k, v) -> { if (v > 0) t.putLong(k, v); });
        return t;
    }
}
