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
        public boolean system;
        /** The requester met the play-time minimum when posting (counts as demand evidence). */
        public boolean counted;
        public final Set<UUID> deliverers = new HashSet<>();
    }

    public static final class Listing {
        public String item;
        public Currency currency;
        public long reference, listedDay, lastTradeDay;
    }

    public static final class LedgerDay {
        public long day, minted, burned;
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
            for (Tag u : c.getList("Deliverers", Tag.TAG_STRING)) { UUID id = uuid(u.getAsString()); if (id != null) x.deliverers.add(id); }
            if ((x.requester != null || x.system) && x.quantity > 0 && x.delivered >= 0 && x.delivered <= x.quantity) d.commissions.add(x);
        }
        for (Tag t : tag.getList("Listings", Tag.TAG_COMPOUND)) {
            CompoundTag l = (CompoundTag) t;
            Listing x = new Listing();
            x.item = l.getString("Item"); x.currency = currency(l.getString("Currency")); x.reference = Math.max(1, l.getLong("Reference"));
            x.listedDay = l.getLong("ListedDay"); x.lastTradeDay = l.getLong("LastTradeDay");
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
        return d;
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
