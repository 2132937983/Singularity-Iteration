// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.21 (futures M4): what the exchange panel of one player shows, built on the server
 * ({@link ExchangeViews#build}) and sent on request and once per second while the panel is open. Every list is bounded
 * on both sides (the decoder rejects larger lists).
 */
public record ExchangeView(long day, long coins, long uu, long mailCoins, int mailTypes, boolean energy,
                           List<Row> markets, Detail detail, List<MyOrder> orders, List<MailLine> mail,
                           List<CommissionLine> commissions, List<PositionLine> positions, List<LedgerLine> ledger,
                           List<StockLine> stocks, List<Candidate> candidates, long allowance, List<BoxLine> box,
                           List<AuctionLine> auctions, List<BountyLine> bounties, BountyBudget budget, SettleView settle,
                           StabilizerLine stabilizer) {
    public static final int MAX_MARKETS = 160, MAX_SPARK = 20, MAX_BARS = 30, MAX_LEVELS = 8, MAX_TRADES = 12;
    public static final int MAX_ORDERS = 32, MAX_MAIL = 40, MAX_COMMISSIONS = 64, MAX_POSITIONS = 8, MAX_LEDGER = 14;
    public static final int MAX_STOCKS = 12, MAX_CANDIDATES = 8, MAX_BOX = 18, MAX_AUCTIONS = 96;
    /** 0.1.10.26 (X3). */
    public static final int MAX_BOUNTIES = 48, MAX_BOUNTY_NEEDS = 8, MAX_BOUNTY_REWARDS = 8;
    /** 0.1.10.27 (X2). */
    public static final int MAX_SETTLE_LINES = 32, MAX_SETTLE_TARGETS = 16;

    /** A watchlist row. {@code currency}: 0 coins, 1 UU-matter (mB). */
    public record Row(String id, int currency, boolean system, long price, int changePermille, long volume, long[] spark, boolean locked) { }

    public record Bar(long day, long open, long high, long low, long close, long volume) { }

    public record Level(long price, long quantity, int orders) { }

    public record TradeLine(long price, int quantity, long time, boolean mine) { }

    /**
     * The selected market: system quotes per item (0 when the market does not quote: no supply, or a player listing; the
     * bid is the price of this player's next item), the treasury supply, the sell pressure (0.1.10.25), the fundamental
     * value, the craft depth, the price band, daily bars, depth, last trades, the four futures prices, the items this
     * player sold of it today and may still buy today (-1 = no limit).
     */
    public record Detail(String id, int currency, boolean system, long price, long ask, long bid, long supply, long pressure,
                         long fundamental, int depth, long low, long high, List<Bar> bars, List<Level> bids, List<Level> asks,
                         List<TradeLine> trades, long futLong3, long futShort3, long futLong7, long futShort7, long mine, long buyLeft) { }

    /**
     * 0.1.10.25 (X1): one commodity of the sell box: the items in the box, what an instant sale would sell and pay now, and
     * what the daily auction would sell and pay if the day changed now.
     */
    public record BoxLine(String id, long quantity, long nowQuantity, long nowCoins, long auctionQuantity, long auctionCoins) { }

    /** 0.1.10.25: one of this player's auction orders with the estimate if the day changed now. */
    public record AuctionLine(long id, String item, long quantity, int minPercent, long estQuantity, long estCoins) { }

    public record MyOrder(long id, String commodity, boolean buy, long price, int remaining, long day) { }

    public record MailLine(String item, long count) { }

    /** {@code kind}: 0 coins, 1 item, 2 UU-matter. */
    public record CommissionLine(long id, String requester, String item, int quantity, int delivered, int kind, String rewardItem,
                                 long rewardTotal, long expiry, boolean system, boolean mine) { }

    public record PositionLine(long id, String commodity, boolean isLong, long price, int quantity, long expiry, long valueNow) { }

    public record LedgerLine(long day, long minted, long burned) { }

    public record StockLine(String item, long stock) { }

    /** Progress of an item toward a listing (multiplayer: requesters / fulfilled / deliverers; single player: days). */
    public record Candidate(String item, int requesters, int fulfilled, int deliverers) { }

    /** 0.1.10.26 (X3): one item a bounty asks for. */
    public record NeedLine(String item, long count, long delivered) { }

    /** 0.1.10.26: one reward of a bounty; {@code kind} 0 coins / 1 item / 2 UU (mB), {@code source} 0 minted / 1 treasury / 2 vault / 3 issue. */
    public record RewardLine(int kind, int source, String item, long amount, long paid) { }

    /**
     * 0.1.10.26: an open system bounty. {@code tier} 0 normal / 1 advanced / 2 special; {@code split} 0 first / 1 contribution;
     * {@code sharePermille} the share of the value one player may deliver; {@code mine} / {@code all} the value this player
     * delivered / the whole value (coins); {@code room} the value this player may still deliver (-1 = no limit).
     */
    public record BountyLine(long id, int tier, long expiry, int split, int sharePermille, List<NeedLine> needs, List<RewardLine> rewards,
                             long mine, long all, long room, int contributors) { }

    /** 0.1.10.26: the weekly budgets of the bounties (issued items and UU-matter against the caps) and the vault. */
    public record BountyBudget(long advancedIssued, long advancedCap, long specialIssued, long specialCap, long uuIssued, long uuCap,
                               int vaultTypes, long vaultItems) {
        public static final BountyBudget NONE = new BountyBudget(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * 0.1.10.27 (X2): the container in the settlement slot. {@code kind} 0 none / 1 items / 2 AE2 cell / 3 fluid container;
     * the quote per commodity (as for the sell box), the items that sell as no commodity, the UU-matter (mB) and the bounties
     * and commissions that its plain items can serve now.
     */
    public record SettleView(int kind, List<BoxLine> lines, long other, long uu, List<SettleTarget> targets) {
        public static final SettleView NONE = new SettleView(0, List.of(), 0, 0, List.of());
    }

    /** 0.1.10.27: {@code type} 0 a system bounty / 1 a player commission; {@code count} the items the container can deliver now. */
    public record SettleTarget(int type, long id, String item, long count) { }

    /**
     * 0.1.10.28 (X4): the stabiliser: on, its factor of the depths and the allowance, the net coins of the last 7 closed days
     * against the target, the money supply estimate, the active sellers; the scrap sink of the last 7 days (items in, products out).
     */
    public record StabilizerLine(boolean enabled, double factor, long net7, long target, long supply, int active, long scrapIn, long scrapOut) {
        public static final StabilizerLine NONE = new StabilizerLine(false, 1, 0, 0, 0, 0, 0, 0);
    }

    /** 0.1.10.27: the settlement view on the wire (shared with the Trade Terminal view). */
    public static void writeSettle(FriendlyByteBuf b, SettleView s) {
        SettleView x = s == null ? SettleView.NONE : s;
        b.writeVarInt(x.kind());
        list(b, x.lines(), MAX_SETTLE_LINES, l -> { b.writeUtf(l.id(), 128); b.writeVarLong(l.quantity()); b.writeVarLong(l.nowQuantity());
            b.writeVarLong(l.nowCoins()); b.writeVarLong(l.auctionQuantity()); b.writeVarLong(l.auctionCoins()); });
        b.writeVarLong(x.other()); b.writeVarLong(x.uu());
        list(b, x.targets(), MAX_SETTLE_TARGETS, t -> { b.writeVarInt(t.type()); b.writeVarLong(t.id()); b.writeUtf(t.item(), 128); b.writeVarLong(t.count()); });
    }

    public static SettleView readSettle(FriendlyByteBuf b) {
        int kind = b.readVarInt();
        List<BoxLine> lines = read(b, MAX_SETTLE_LINES, () -> new BoxLine(b.readUtf(128), b.readVarLong(), b.readVarLong(), b.readVarLong(),
            b.readVarLong(), b.readVarLong()));
        long other = b.readVarLong(), uu = b.readVarLong();
        List<SettleTarget> targets = read(b, MAX_SETTLE_TARGETS, () -> new SettleTarget(b.readVarInt(), b.readVarLong(), b.readUtf(128), b.readVarLong()));
        return new SettleView(kind, lines, other, uu, targets);
    }

    // ------------------------------------------------------------------ codec

    public void write(FriendlyByteBuf b) {
        b.writeVarLong(day); b.writeVarLong(coins); b.writeVarLong(uu); b.writeVarLong(mailCoins); b.writeVarInt(mailTypes); b.writeBoolean(energy);
        list(b, markets, MAX_MARKETS, r -> {
            b.writeUtf(r.id(), 128); b.writeVarInt(r.currency()); b.writeBoolean(r.system()); b.writeVarLong(r.price()); b.writeInt(r.changePermille());
            b.writeVarLong(r.volume()); b.writeBoolean(r.locked());
            int n = Math.min(MAX_SPARK, r.spark().length);
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) b.writeVarLong(r.spark()[i]);
        });
        b.writeBoolean(detail != null);
        if (detail != null) {
            Detail d = detail;
            b.writeUtf(d.id(), 128); b.writeVarInt(d.currency()); b.writeBoolean(d.system()); b.writeVarLong(d.price()); b.writeVarLong(d.ask());
            b.writeVarLong(d.bid()); b.writeVarLong(d.supply()); b.writeVarLong(d.pressure()); b.writeVarLong(d.fundamental()); b.writeVarInt(d.depth());
            b.writeVarLong(d.low()); b.writeVarLong(d.high());
            list(b, d.bars(), MAX_BARS, x -> { b.writeVarLong(x.day()); b.writeVarLong(x.open()); b.writeVarLong(x.high()); b.writeVarLong(x.low());
                b.writeVarLong(x.close()); b.writeVarLong(x.volume()); });
            list(b, d.bids(), MAX_LEVELS, x -> { b.writeVarLong(x.price()); b.writeVarLong(x.quantity()); b.writeVarInt(x.orders()); });
            list(b, d.asks(), MAX_LEVELS, x -> { b.writeVarLong(x.price()); b.writeVarLong(x.quantity()); b.writeVarInt(x.orders()); });
            list(b, d.trades(), MAX_TRADES, x -> { b.writeVarLong(x.price()); b.writeVarInt(x.quantity()); b.writeVarLong(x.time()); b.writeBoolean(x.mine()); });
            b.writeVarLong(d.futLong3()); b.writeVarLong(d.futShort3()); b.writeVarLong(d.futLong7()); b.writeVarLong(d.futShort7());
            b.writeVarLong(d.mine()); b.writeLong(d.buyLeft());
        }
        list(b, orders, MAX_ORDERS, x -> { b.writeVarLong(x.id()); b.writeUtf(x.commodity(), 128); b.writeBoolean(x.buy()); b.writeVarLong(x.price());
            b.writeVarInt(x.remaining()); b.writeVarLong(x.day()); });
        list(b, mail, MAX_MAIL, x -> { b.writeUtf(x.item(), 128); b.writeVarLong(x.count()); });
        list(b, commissions, MAX_COMMISSIONS, x -> { b.writeVarLong(x.id()); b.writeUtf(x.requester(), 32); b.writeUtf(x.item(), 128);
            b.writeVarInt(x.quantity()); b.writeVarInt(x.delivered()); b.writeVarInt(x.kind()); b.writeUtf(x.rewardItem(), 128);
            b.writeVarLong(x.rewardTotal()); b.writeVarLong(x.expiry()); b.writeBoolean(x.system()); b.writeBoolean(x.mine()); });
        list(b, positions, MAX_POSITIONS, x -> { b.writeVarLong(x.id()); b.writeUtf(x.commodity(), 128); b.writeBoolean(x.isLong());
            b.writeVarLong(x.price()); b.writeVarInt(x.quantity()); b.writeVarLong(x.expiry()); b.writeVarLong(x.valueNow()); });
        list(b, ledger, MAX_LEDGER, x -> { b.writeVarLong(x.day()); b.writeVarLong(x.minted()); b.writeVarLong(x.burned()); });
        list(b, stocks, MAX_STOCKS, x -> { b.writeUtf(x.item(), 128); b.writeVarLong(x.stock()); });
        list(b, candidates, MAX_CANDIDATES, x -> { b.writeUtf(x.item(), 128); b.writeVarInt(x.requesters()); b.writeVarInt(x.fulfilled());
            b.writeVarInt(x.deliverers()); });
        b.writeLong(allowance);
        list(b, box, MAX_BOX, x -> { b.writeUtf(x.id(), 128); b.writeVarLong(x.quantity()); b.writeVarLong(x.nowQuantity()); b.writeVarLong(x.nowCoins());
            b.writeVarLong(x.auctionQuantity()); b.writeVarLong(x.auctionCoins()); });
        list(b, auctions, MAX_AUCTIONS, x -> { b.writeVarLong(x.id()); b.writeUtf(x.item(), 128); b.writeVarLong(x.quantity()); b.writeVarInt(x.minPercent());
            b.writeVarLong(x.estQuantity()); b.writeVarLong(x.estCoins()); });
        // 0.1.10.26 (X3)
        list(b, bounties, MAX_BOUNTIES, x -> {
            b.writeVarLong(x.id()); b.writeVarInt(x.tier()); b.writeVarLong(x.expiry()); b.writeVarInt(x.split()); b.writeVarInt(x.sharePermille());
            list(b, x.needs(), MAX_BOUNTY_NEEDS, n -> { b.writeUtf(n.item(), 128); b.writeVarLong(n.count()); b.writeVarLong(n.delivered()); });
            list(b, x.rewards(), MAX_BOUNTY_REWARDS, r -> { b.writeVarInt(r.kind()); b.writeVarInt(r.source()); b.writeUtf(r.item(), 128);
                b.writeVarLong(r.amount()); b.writeVarLong(r.paid()); });
            b.writeVarLong(x.mine()); b.writeVarLong(x.all()); b.writeLong(x.room()); b.writeVarInt(x.contributors());
        });
        BountyBudget g = budget == null ? BountyBudget.NONE : budget;
        b.writeVarLong(g.advancedIssued()); b.writeVarLong(g.advancedCap()); b.writeVarLong(g.specialIssued()); b.writeVarLong(g.specialCap());
        b.writeVarLong(g.uuIssued()); b.writeVarLong(g.uuCap()); b.writeVarInt(g.vaultTypes()); b.writeVarLong(g.vaultItems());
        // 0.1.10.27 (X2)
        writeSettle(b, settle);
        // 0.1.10.28 (X4)
        StabilizerLine s = stabilizer == null ? StabilizerLine.NONE : stabilizer;
        b.writeBoolean(s.enabled()); b.writeDouble(s.factor()); b.writeLong(s.net7()); b.writeVarLong(s.target()); b.writeVarLong(s.supply());
        b.writeVarInt(s.active()); b.writeVarLong(s.scrapIn()); b.writeVarLong(s.scrapOut());
    }

    public static ExchangeView read(FriendlyByteBuf b) {
        long day = b.readVarLong(), coins = b.readVarLong(), uu = b.readVarLong(), mailCoins = b.readVarLong();
        int mailTypes = b.readVarInt();
        boolean energy = b.readBoolean();
        List<Row> markets = read(b, MAX_MARKETS, () -> {
            String id = b.readUtf(128); int currency = b.readVarInt(); boolean system = b.readBoolean(); long price = b.readVarLong();
            int change = b.readInt(); long volume = b.readVarLong(); boolean locked = b.readBoolean();
            int n = b.readVarInt();
            if (n < 0 || n > MAX_SPARK) throw new io.netty.handler.codec.DecoderException("spark");
            long[] spark = new long[n];
            for (int i = 0; i < n; i++) spark[i] = b.readVarLong();
            return new Row(id, currency, system, price, change, volume, spark, locked);
        });
        Detail detail = null;
        if (b.readBoolean()) {
            String id = b.readUtf(128); int currency = b.readVarInt(); boolean system = b.readBoolean(); long price = b.readVarLong();
            long ask = b.readVarLong(), bid = b.readVarLong(), supply = b.readVarLong(), pressure = b.readVarLong(), fundamental = b.readVarLong();
            int depth = b.readVarInt(); long low = b.readVarLong(), high = b.readVarLong();
            List<Bar> bars = read(b, MAX_BARS, () -> new Bar(b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong()));
            List<Level> bids = read(b, MAX_LEVELS, () -> new Level(b.readVarLong(), b.readVarLong(), b.readVarInt()));
            List<Level> asks = read(b, MAX_LEVELS, () -> new Level(b.readVarLong(), b.readVarLong(), b.readVarInt()));
            List<TradeLine> trades = read(b, MAX_TRADES, () -> new TradeLine(b.readVarLong(), b.readVarInt(), b.readVarLong(), b.readBoolean()));
            long l3 = b.readVarLong(), s3 = b.readVarLong(), l7 = b.readVarLong(), s7 = b.readVarLong();
            long mine = b.readVarLong(), buyLeft = b.readLong();
            detail = new Detail(id, currency, system, price, ask, bid, supply, pressure, fundamental, depth, low, high, bars, bids, asks, trades,
                l3, s3, l7, s7, mine, buyLeft);
        }
        List<MyOrder> orders = read(b, MAX_ORDERS, () -> new MyOrder(b.readVarLong(), b.readUtf(128), b.readBoolean(), b.readVarLong(), b.readVarInt(), b.readVarLong()));
        List<MailLine> mail = read(b, MAX_MAIL, () -> new MailLine(b.readUtf(128), b.readVarLong()));
        List<CommissionLine> commissions = read(b, MAX_COMMISSIONS, () -> new CommissionLine(b.readVarLong(), b.readUtf(32), b.readUtf(128),
            b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(128), b.readVarLong(), b.readVarLong(), b.readBoolean(), b.readBoolean()));
        List<PositionLine> positions = read(b, MAX_POSITIONS, () -> new PositionLine(b.readVarLong(), b.readUtf(128), b.readBoolean(), b.readVarLong(),
            b.readVarInt(), b.readVarLong(), b.readVarLong()));
        List<LedgerLine> ledger = read(b, MAX_LEDGER, () -> new LedgerLine(b.readVarLong(), b.readVarLong(), b.readVarLong()));
        List<StockLine> stocks = read(b, MAX_STOCKS, () -> new StockLine(b.readUtf(128), b.readVarLong()));
        List<Candidate> candidates = read(b, MAX_CANDIDATES, () -> new Candidate(b.readUtf(128), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        long allowance = b.readLong();
        List<BoxLine> box = read(b, MAX_BOX, () -> new BoxLine(b.readUtf(128), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong()));
        List<AuctionLine> auctions = read(b, MAX_AUCTIONS, () -> new AuctionLine(b.readVarLong(), b.readUtf(128), b.readVarLong(), b.readVarInt(),
            b.readVarLong(), b.readVarLong()));
        List<BountyLine> bounties = read(b, MAX_BOUNTIES, () -> {
            long id = b.readVarLong(); int tier = b.readVarInt(); long expiry = b.readVarLong(); int split = b.readVarInt(); int share = b.readVarInt();
            List<NeedLine> needs = read(b, MAX_BOUNTY_NEEDS, () -> new NeedLine(b.readUtf(128), b.readVarLong(), b.readVarLong()));
            List<RewardLine> rewards = read(b, MAX_BOUNTY_REWARDS, () -> new RewardLine(b.readVarInt(), b.readVarInt(), b.readUtf(128), b.readVarLong(), b.readVarLong()));
            return new BountyLine(id, tier, expiry, split, share, needs, rewards, b.readVarLong(), b.readVarLong(), b.readLong(), b.readVarInt());
        });
        BountyBudget budget = new BountyBudget(b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(),
            b.readVarInt(), b.readVarLong());
        SettleView settle = readSettle(b);
        StabilizerLine stabilizer = new StabilizerLine(b.readBoolean(), b.readDouble(), b.readLong(), b.readVarLong(), b.readVarLong(), b.readVarInt(),
            b.readVarLong(), b.readVarLong());
        return new ExchangeView(day, coins, uu, mailCoins, mailTypes, energy, markets, detail, orders, mail, commissions, positions, ledger, stocks, candidates,
            allowance, box, auctions, bounties, budget, settle, stabilizer);
    }

    interface Writer<T> { void write(T value); }

    interface Reader<T> { T read(); }

    static <T> void list(FriendlyByteBuf b, List<T> values, int max, Writer<T> writer) {
        int n = Math.min(max, values.size());
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) writer.write(values.get(i));
    }

    static <T> List<T> read(FriendlyByteBuf b, int max, Reader<T> reader) {
        int n = b.readVarInt();
        if (n < 0 || n > max) throw new io.netty.handler.codec.DecoderException("exchange view list of " + n + " (max " + max + ")");
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(reader.read());
        return out;
    }
}
