// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.42: what the Trade Box panel shows ({@link TradeBoxPackets}): the tier, the owner, the settings, the filter items,
 * the status of the last run ({@link TerminalRules.Status} ordinal, -1 = not run yet), the counters of the market day
 * ({@link TradeBoxRules#SOLD} ..), the bounties the owner claimed. Bounded on both sides.
 */
public record TradeBoxView(int tier, String owner, boolean ownerOnline, int mode, int percent, long coins, boolean inCoins, boolean autoDeliver,
                           boolean offlineAuction, List<String> filter, int status, List<Long> today, int claimed, long day) {
    public static final int MAX_FILTER = TradeBoxRules.FILTER_SLOTS, MAX_COUNTERS = TradeBoxRules.COUNTERS;

    /** A counter of the market day (0 when absent). */
    public long counter(int index) { return index >= 0 && index < today.size() ? today.get(index) : 0; }

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(tier); b.writeUtf(owner, 64); b.writeBoolean(ownerOnline); b.writeVarInt(mode); b.writeVarInt(percent);
        b.writeVarLong(coins); b.writeBoolean(inCoins); b.writeBoolean(autoDeliver); b.writeBoolean(offlineAuction);
        ExchangeView.list(b, filter, MAX_FILTER, f -> b.writeUtf(f, 128));
        b.writeVarInt(status + 1);
        ExchangeView.list(b, today, MAX_COUNTERS, b::writeVarLong);
        b.writeVarInt(claimed); b.writeVarLong(day);
    }

    public static TradeBoxView read(FriendlyByteBuf b) {
        int tier = b.readVarInt();
        String owner = b.readUtf(64);
        boolean online = b.readBoolean();
        int mode = b.readVarInt(), percent = b.readVarInt();
        long coins = b.readVarLong();
        boolean inCoins = b.readBoolean(), autoDeliver = b.readBoolean(), offlineAuction = b.readBoolean();
        List<String> filter = ExchangeView.read(b, MAX_FILTER, () -> b.readUtf(128));
        int status = b.readVarInt() - 1;
        List<Long> today = ExchangeView.read(b, MAX_COUNTERS, b::readVarLong);
        return new TradeBoxView(tier, owner, online, mode, percent, coins, inCoins, autoDeliver, offlineAuction, filter, status, today,
            b.readVarInt(), b.readVarLong());
    }
}
