// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.27 (X2): what the Trade Terminal panel of one viewer shows, built on the server ({@link TerminalPackets}) and sent on
 * request and once per second while the panel is open. Lists are bounded on both sides. The owner's mailbox coins are shown
 * only to viewers who may configure the terminal ({@code ownerCoins} -1 otherwise).
 */
public record TerminalView(String owner, boolean ownerOnline, int level, long energy, long maxEnergy, boolean energyOk, boolean outputMode,
                           int teamLevel, long ownerCoins, long day, List<OrderLine> orders, List<AccessLine> access, List<LogLine> log,
                           ExchangeView.SettleView settle, List<CardView.CardLine> cards, List<ExchangeView.CurveLine> curves) {
    public static final int MAX_ORDERS = TerminalRules.MAX_ORDERS, MAX_ACCESS = TerminalRules.MAX_ACCESS, MAX_LOG = 32;

    /** A standing order: {@code side} 0 sell / 1 buy, {@code mode} 0 auction / 1 instant, {@code status} -1 not run yet, else {@link TerminalRules.Status}. */
    public record OrderLine(int side, String item, long amount, int mode, long limit, boolean enabled, int status, boolean percent,
                            long estLimit, long estQuantity, long estCoins) {
        /** 0.1.10.29: without the estimate of an instant sale ({@code percent}: the limit is a percent). */
        public OrderLine(int side, String item, long amount, int mode, long limit, boolean enabled, int status) {
            this(side, item, amount, mode, limit, enabled, status, false, 0, 0, 0);
        }
    }

    public record AccessLine(String name, int level) { }

    /** {@code kind}: {@link TerminalOrders#LOG_SOLD}, {@code LOG_AUCTION}, {@code LOG_BOUGHT}. */
    public record LogLine(long day, int kind, String item, long quantity, long coins) { }

    public void write(FriendlyByteBuf b) {
        b.writeUtf(owner, 64); b.writeBoolean(ownerOnline); b.writeVarInt(level); b.writeVarLong(energy); b.writeVarLong(maxEnergy);
        b.writeBoolean(energyOk); b.writeBoolean(outputMode); b.writeVarInt(teamLevel); b.writeLong(ownerCoins); b.writeVarLong(day);
        ExchangeView.list(b, orders, MAX_ORDERS, o -> { b.writeVarInt(o.side()); b.writeUtf(o.item(), 128); b.writeVarLong(o.amount());
            b.writeVarInt(o.mode()); b.writeVarLong(o.limit()); b.writeBoolean(o.enabled()); b.writeVarInt(o.status() + 1);
            b.writeBoolean(o.percent()); b.writeVarLong(o.estLimit()); b.writeVarLong(o.estQuantity()); b.writeVarLong(o.estCoins()); });
        ExchangeView.list(b, access, MAX_ACCESS, a -> { b.writeUtf(a.name(), 64); b.writeVarInt(a.level()); });
        ExchangeView.list(b, log, MAX_LOG, l -> { b.writeVarLong(l.day()); b.writeVarInt(l.kind()); b.writeUtf(l.item(), 128);
            b.writeVarLong(l.quantity()); b.writeVarLong(l.coins()); });
        ExchangeView.writeSettle(b, settle);
        // 0.1.10.28 (X4): the owner's delegate ID cards (empty for other viewers)
        ExchangeView.list(b, cards, CardView.MAX_LINES, x -> { b.writeVarLong(x.serial()); b.writeUtf(x.holder(), 64); b.writeVarInt(x.level());
            b.writeVarLong(x.expiry()); });
        // 0.1.10.29: the curves of the settlement container (the minimum-price preview)
        ExchangeView.writeCurves(b, curves, ExchangeView.MAX_SETTLE_LINES);
    }

    public static TerminalView read(FriendlyByteBuf b) {
        String owner = b.readUtf(64);
        boolean online = b.readBoolean();
        int level = b.readVarInt();
        long energy = b.readVarLong(), maxEnergy = b.readVarLong();
        boolean energyOk = b.readBoolean(), outputMode = b.readBoolean();
        int teamLevel = b.readVarInt();
        long ownerCoins = b.readLong(), day = b.readVarLong();
        List<OrderLine> orders = ExchangeView.read(b, MAX_ORDERS, () -> new OrderLine(b.readVarInt(), b.readUtf(128), b.readVarLong(), b.readVarInt(),
            b.readVarLong(), b.readBoolean(), b.readVarInt() - 1, b.readBoolean(), b.readVarLong(), b.readVarLong(), b.readVarLong()));
        List<AccessLine> access = ExchangeView.read(b, MAX_ACCESS, () -> new AccessLine(b.readUtf(64), b.readVarInt()));
        List<LogLine> log = ExchangeView.read(b, MAX_LOG, () -> new LogLine(b.readVarLong(), b.readVarInt(), b.readUtf(128), b.readVarLong(), b.readVarLong()));
        ExchangeView.SettleView settle = ExchangeView.readSettle(b);
        List<CardView.CardLine> cards = ExchangeView.read(b, CardView.MAX_LINES, () -> new CardView.CardLine(b.readVarLong(), b.readUtf(64), b.readVarInt(),
            b.readVarLong()));
        List<ExchangeView.CurveLine> curves = ExchangeView.readCurves(b, ExchangeView.MAX_SETTLE_LINES);
        return new TerminalView(owner, online, level, energy, maxEnergy, energyOk, outputMode, teamLevel, ownerCoins, day, orders, access, log, settle,
            cards, curves);
    }
}
