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
                           List<AuctionLine> auctions) {
    public static final int MAX_MARKETS = 160, MAX_SPARK = 20, MAX_BARS = 30, MAX_LEVELS = 8, MAX_TRADES = 12;
    public static final int MAX_ORDERS = 32, MAX_MAIL = 40, MAX_COMMISSIONS = 64, MAX_POSITIONS = 8, MAX_LEDGER = 14;
    public static final int MAX_STOCKS = 12, MAX_CANDIDATES = 8, MAX_BOX = 18, MAX_AUCTIONS = 96;

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
        return new ExchangeView(day, coins, uu, mailCoins, mailTypes, energy, markets, detail, orders, mail, commissions, positions, ledger, stocks, candidates,
            allowance, box, auctions);
    }

    private interface Writer<T> { void write(T value); }

    private interface Reader<T> { T read(); }

    private static <T> void list(FriendlyByteBuf b, List<T> values, int max, Writer<T> writer) {
        int n = Math.min(max, values.size());
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) writer.write(values.get(i));
    }

    private static <T> List<T> read(FriendlyByteBuf b, int max, Reader<T> reader) {
        int n = b.readVarInt();
        if (n < 0 || n > max) throw new io.netty.handler.codec.DecoderException("exchange view list of " + n + " (max " + max + ")");
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(reader.read());
        return out;
    }
}
