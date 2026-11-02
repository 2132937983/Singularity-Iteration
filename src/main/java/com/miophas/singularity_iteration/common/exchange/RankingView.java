// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.42: the wealth ranking a panel asked for ({@link WealthRanking}): one category, its top entries and the viewer's place.
 * Sent with the exchange view and the Trade Terminal view only while the panel shows the ranking. Bounded on both sides.
 *
 * <p>{@code parts}: the viewer's total assets by kind ({@link WealthRules#PART_COINS} ..), only for the category
 * {@link WealthRules#TOTAL}. {@code hidden}: the viewer keeps their name out of the ranking (others see "anonymous").
 */
public record RankingView(boolean enabled, int category, List<Entry> top, int myRank, long myValue, int players, boolean hidden,
                          List<Long> parts, long day) {
    public static final int MAX_TOP = WealthRules.TOP, MAX_PARTS = WealthRules.PARTS;

    /** A place: rank (1 = first), name ("" = anonymous), value, the viewer's own entry. */
    public record Entry(int rank, String name, long value, boolean me) { }

    public static final RankingView NONE = new RankingView(false, -1, List.of(), 0, 0, 0, false, List.of(), 0);

    /** A ranking was asked for (the panel shows it). */
    public boolean shown() { return category >= 0; }

    static void write(FriendlyByteBuf b, RankingView r) {
        RankingView x = r == null ? NONE : r;
        b.writeBoolean(x.enabled());
        b.writeVarInt(x.category() + 1);
        ExchangeView.list(b, x.top(), MAX_TOP, e -> { b.writeVarInt(e.rank()); b.writeUtf(e.name(), 32); b.writeLong(e.value()); b.writeBoolean(e.me()); });
        b.writeVarInt(x.myRank()); b.writeLong(x.myValue()); b.writeVarInt(x.players()); b.writeBoolean(x.hidden());
        ExchangeView.list(b, x.parts(), MAX_PARTS, b::writeLong);
        b.writeVarLong(x.day());
    }

    static RankingView read(FriendlyByteBuf b) {
        boolean enabled = b.readBoolean();
        int category = b.readVarInt() - 1;
        if (category < -1 || category >= WealthRules.CATEGORIES) throw new io.netty.handler.codec.DecoderException("ranking category");
        List<Entry> top = ExchangeView.read(b, MAX_TOP, () -> new Entry(b.readVarInt(), b.readUtf(32), b.readLong(), b.readBoolean()));
        int myRank = b.readVarInt();
        long myValue = b.readLong();
        int players = b.readVarInt();
        boolean hidden = b.readBoolean();
        List<Long> parts = ExchangeView.read(b, MAX_PARTS, b::readLong);
        return new RankingView(enabled, category, top, myRank, myValue, players, hidden, parts, b.readVarLong());
    }
}
