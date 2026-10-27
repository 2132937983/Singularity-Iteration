// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.28 (X4): what the account screen of a personal ID card shows ({@link CardPackets}): the mailbox, the daily allowance
 * left (-1 = none), the stabiliser's factor, the resting orders, the auction orders and the delegate cards the holder issued.
 * Lists are bounded on both sides.
 */
public record CardView(String holder, long day, long mailCoins, long mailUu, long mailItems, long allowance, double factor,
                       List<MailLine> mail, List<OrderLine> orders, List<AuctionLine> auctions, List<CardLine> cards) {
    public static final int MAX_LINES = 40;

    public record MailLine(String item, long count) { }

    public record OrderLine(long id, String commodity, boolean buy, long price, int remaining, long day) { }

    public record AuctionLine(long id, String item, long quantity, int minPercent) { }

    public record CardLine(long serial, String holder, int level, long expiry) { }

    public void write(FriendlyByteBuf b) {
        b.writeUtf(holder, 64); b.writeVarLong(day); b.writeVarLong(mailCoins); b.writeVarLong(mailUu); b.writeVarLong(mailItems);
        b.writeLong(allowance); b.writeDouble(factor);
        ExchangeView.list(b, mail, MAX_LINES, x -> { b.writeUtf(x.item(), 128); b.writeVarLong(x.count()); });
        ExchangeView.list(b, orders, MAX_LINES, x -> { b.writeVarLong(x.id()); b.writeUtf(x.commodity(), 128); b.writeBoolean(x.buy());
            b.writeVarLong(x.price()); b.writeVarInt(x.remaining()); b.writeVarLong(x.day()); });
        ExchangeView.list(b, auctions, MAX_LINES, x -> { b.writeVarLong(x.id()); b.writeUtf(x.item(), 128); b.writeVarLong(x.quantity());
            b.writeVarInt(x.minPercent()); });
        ExchangeView.list(b, cards, MAX_LINES, x -> { b.writeVarLong(x.serial()); b.writeUtf(x.holder(), 64); b.writeVarInt(x.level());
            b.writeVarLong(x.expiry()); });
    }

    public static CardView read(FriendlyByteBuf b) {
        String holder = b.readUtf(64);
        long day = b.readVarLong(), coins = b.readVarLong(), uu = b.readVarLong(), items = b.readVarLong(), allowance = b.readLong();
        double factor = b.readDouble();
        List<MailLine> mail = ExchangeView.read(b, MAX_LINES, () -> new MailLine(b.readUtf(128), b.readVarLong()));
        List<OrderLine> orders = ExchangeView.read(b, MAX_LINES, () -> new OrderLine(b.readVarLong(), b.readUtf(128), b.readBoolean(), b.readVarLong(),
            b.readVarInt(), b.readVarLong()));
        List<AuctionLine> auctions = ExchangeView.read(b, MAX_LINES, () -> new AuctionLine(b.readVarLong(), b.readUtf(128), b.readVarLong(), b.readVarInt()));
        List<CardLine> cards = ExchangeView.read(b, MAX_LINES, () -> new CardLine(b.readVarLong(), b.readUtf(64), b.readVarInt(), b.readVarLong()));
        return new CardView(holder, day, coins, uu, items, allowance, factor, mail, orders, auctions, cards);
    }
}
