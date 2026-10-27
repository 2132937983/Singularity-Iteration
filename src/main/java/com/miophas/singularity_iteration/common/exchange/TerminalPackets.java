// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeTerminalBlockEntity;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Action;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.menu.producer.TradeTerminalMenu;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.10.27 (X2): the Trade Terminal panel packets: C2S {@link Request} (the view), C2S {@link TAction} (one action), S2C
 * {@link View}. The server acts only for a player whose open menu is a live terminal in reach, under the
 * {@link PacketRateLimit#EXCHANGE} bucket, and checks the player's access level for every action ({@link TerminalRules#may}).
 */
public final class TerminalPackets {
    private TerminalPackets() {}

    /**
     * Kinds: ORDER_SET (c = index or -1 for a new one, text = commodity, a = amount, b = limit, flags: 1 buy, 2 instant,
     * 4 enabled, 8 scrap, 16 the limit is a percent (0.1.10.29)); ORDER_REMOVE (c); ORDER_TOGGLE (c); OUTPUT_MODE (flags 1 = on); ACCESS_SET (text = player name, a = level,
     * 0 removes); TEAM_LEVEL (a); SETTLE_SELL (flags 1 = auction, a = minimum percent); SETTLE_UU; SETTLE_LOAD (settlement
     * container into the sell buffer); SETTLE_UNLOAD (output buffer into the settlement container); RUN_NOW; SETTLE_BOUNTY
     * (a = bounty); SETTLE_COMMISSION (a = commission, c = quantity); SETTLE_BUY (text = commodity, a = limit, c = quantity).
     */
    public static final int ORDER_SET = 1, ORDER_REMOVE = 2, ORDER_TOGGLE = 3, OUTPUT_MODE = 4, ACCESS_SET = 5, TEAM_LEVEL = 6,
        SETTLE_SELL = 7, SETTLE_UU = 8, SETTLE_LOAD = 9, SETTLE_UNLOAD = 10, RUN_NOW = 11, SETTLE_BOUNTY = 12, SETTLE_COMMISSION = 13,
        SETTLE_BUY = 14;
    /**
     * 0.1.10.28 (X4): ID cards of the owner. ISSUE_CARD (text = player name, a = level, b = days: a blank card of the owner's inventory
     * becomes a delegate card for that player), REVOKE_CARD (a = serial), REVOKE_ALL_CARDS. SETTLE_SELL flags 2 = scrap sink.
     */
    public static final int ISSUE_CARD = 15, REVOKE_CARD = 16, REVOKE_ALL_CARDS = 17;
    /** 0.1.10.29: SETTLE_COINS (the coins of the settlement container onto the viewer's coin account). SETTLE_SELL b = minimum coins per item. */
    public static final int SETTLE_COINS = 18;

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "terminal_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> { }, b -> new Request());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TAction(int kind, int c, String text, long a, long b, int flags) implements CustomPacketPayload {
        public static final Type<TAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "terminal_action"));
        public static final StreamCodec<FriendlyByteBuf, TAction> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.kind); buf.writeVarInt(p.c); buf.writeUtf(p.text, 128); buf.writeVarLong(p.a); buf.writeVarLong(p.b); buf.writeVarInt(p.flags);
        }, buf -> new TAction(buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarLong(), buf.readVarLong(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record View(TerminalView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "terminal_view"));
        public static final StreamCodec<FriendlyByteBuf, View> CODEC = StreamCodec.of((b, p) -> p.view.write(b), b -> new View(TerminalView.read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, TerminalPackets::onRequest);
        registrar.playToServer(TAction.TYPE, TAction.CODEC, TerminalPackets::onAction);
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> context.enqueueWork(() -> TerminalClientState.set(packet.view())));
    }

    private static TradeTerminalMenu menu(ServerPlayer player) {
        if (!(player.containerMenu instanceof TradeTerminalMenu menu) || menu.terminal() == null || !menu.stillValid(player)) return null;
        return menu;
    }

    private static void onRequest(Request packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TradeTerminalMenu menu = menu(player);
            if (menu == null || !PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE_VIEW)) return;
            send(player, menu);
        });
    }

    private static void onAction(TAction p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TradeTerminalMenu menu = menu(player);
            if (menu == null) return;
            if (!PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE)) {
                player.displayClientMessage(Component.translatable("message.mio_icif.exchange.too_fast"), true);
                return;
            }
            TradeTerminalBlockEntity t = menu.terminal();
            int level = t.levelOf(player);
            Result r = switch (p.kind) {
                case SETTLE_SELL, SETTLE_UU, SETTLE_BOUNTY, SETTLE_COMMISSION, SETTLE_BUY, SETTLE_COINS -> {
                    if (!TerminalRules.may(level, Action.SETTLE)) yield Result.fail("terminal.denied");
                    // a trade of the viewer at this terminal: the energy fee of a trade, the result on the action bar
                    t.exchangeAction(player, () -> settle(player, menu, p));
                    menu.broadcastChanges();
                    yield null;
                }
                default -> act(player, menu, t, level, p);
            };
            if (r != null) player.displayClientMessage(Component.translatable(r.key(), r.args()), true);
            send(player, menu);
        });
    }

    static Result settle(ServerPlayer player, TradeTerminalMenu menu, TAction p) {
        SettlementStorage st = SettlementStorages.open(menu.settlementSlot(), 0);
        if (st == null) return Result.fail("settle_empty");
        return switch (p.kind) {
            case SETTLE_SELL -> Settlement.sell(player, st, (p.flags & 1) != 0, (int) Math.max(0, Math.min(100, p.a)), (p.flags & 2) != 0,
                Math.max(0, p.b));
            case SETTLE_COINS -> Settlement.depositCoins(player, st);
            case SETTLE_BOUNTY -> Settlement.deliverBounty(player, st, p.a);
            case SETTLE_COMMISSION -> Settlement.deliverCommission(player, st, p.a, Math.max(1, p.c));
            case SETTLE_BUY -> Settlement.buyInto(player, st, p.text, p.a, Math.max(1, Math.min(ExchangeService.MAX_QUANTITY, p.c)));
            default -> Settlement.depositUu(player, st);
        };
    }

    static Result act(ServerPlayer player, TradeTerminalMenu menu, TradeTerminalBlockEntity t, int level, TAction p) {
        MinecraftServer server = player.server;
        switch (p.kind) {
            case ORDER_SET -> {
                if (!TerminalRules.may(level, Action.EDIT_ORDERS)) return Result.fail("terminal.denied");
                if (!FutureCommodityManager.hasCommodity(p.text)) return Result.fail("terminal.bad_order");
                TerminalRules.Order o;
                try {
                    o = new TerminalRules.Order((p.flags & 1) != 0 ? TerminalRules.Side.BUY : TerminalRules.Side.SELL, p.text,
                        Math.min(p.a, ExchangeService.MAX_QUANTITY * 10L),
                        (p.flags & 8) != 0 ? TerminalRules.Mode.SCRAP : (p.flags & 2) != 0 ? TerminalRules.Mode.INSTANT : TerminalRules.Mode.AUCTION,
                        p.b, (p.flags & 4) != 0, (p.flags & 16) != 0);
                } catch (IllegalArgumentException e) {
                    return Result.fail("terminal.bad_order");
                }
                if (o.side() == TerminalRules.Side.BUY && o.limit() <= 0) return Result.fail("terminal.bad_order");
                if (!t.setOrder(p.c, o)) return Result.fail("terminal.orders_full", TerminalRules.MAX_ORDERS);
                return Result.done("terminal.order_saved");
            }
            case ORDER_REMOVE, ORDER_TOGGLE -> {
                if (!TerminalRules.may(level, Action.EDIT_ORDERS)) return Result.fail("terminal.denied");
                if (p.c < 0 || p.c >= t.orders().size()) return Result.fail("terminal.bad_order");
                if (p.kind == ORDER_REMOVE) { t.removeOrder(p.c); return Result.done("terminal.order_removed"); }
                TerminalRules.Order o = t.orders().get(p.c);
                t.setOrder(p.c, o.withEnabled(!o.enabled()));
                return Result.done("terminal.order_saved");
            }
            case OUTPUT_MODE -> {
                if (!TerminalRules.may(level, Action.OUTPUT_MODE)) return Result.fail("terminal.denied");
                t.setOutputMode((p.flags & 1) != 0);
                return null;
            }
            case ACCESS_SET -> {
                if (!TerminalRules.may(level, Action.EDIT_ACCESS)) return Result.fail("terminal.denied");
                GameProfile who = profile(server, p.text);
                if (who == null) return Result.fail("terminal.no_player", p.text);
                if (!t.setAccess(who.getId(), who.getName(), (int) p.a)) return Result.fail("terminal.access_full", TerminalRules.MAX_ACCESS);
                return Result.done("terminal.access_saved", who.getName());
            }
            case TEAM_LEVEL -> {
                if (!TerminalRules.may(level, Action.EDIT_ACCESS)) return Result.fail("terminal.denied");
                t.setTeamLevel((int) p.a);
                return null;
            }
            case SETTLE_LOAD, SETTLE_UNLOAD -> {
                boolean load = p.kind == SETTLE_LOAD;
                if (!TerminalRules.may(level, load ? Action.LOAD_BUFFER : Action.UNLOAD_OUTPUT)) return Result.fail("terminal.denied");
                SettlementStorage st = SettlementStorages.open(menu.settlementSlot(), 0);
                if (st == null) return Result.fail("settle_empty");
                long moved = load ? Settlement.load(st, t.sellBuffer(), s -> ExchangeService.sellableAs(s) != null) : Settlement.unload(st, t.outputBuffer());
                menu.broadcastChanges();
                return moved > 0 ? Result.done("terminal.moved", moved) : Result.fail("terminal.nothing_moved");
            }
            case ISSUE_CARD -> {
                if (!TerminalRules.may(level, Action.EDIT_ACCESS) || !player.getUUID().equals(t.owner().uuid())) return Result.fail("terminal.denied");
                GameProfile who = profile(server, p.text);
                if (who == null) return Result.fail("terminal.no_player", p.text);
                return IdCards.issue(player, who, (int) p.a, (int) Math.max(1, Math.min(IdCardRules.MAX_DAYS, p.b)));
            }
            case REVOKE_CARD, REVOKE_ALL_CARDS -> {
                if (!TerminalRules.may(level, Action.EDIT_ACCESS) || !player.getUUID().equals(t.owner().uuid())) return Result.fail("terminal.denied");
                int n = IdCards.revoke(server, player.getUUID(), p.kind == REVOKE_ALL_CARDS ? -1 : p.a);
                return n > 0 ? Result.done("card.revoked", n) : Result.fail("card.none_revoked");
            }
            case RUN_NOW -> {
                if (!TerminalRules.may(level, Action.EDIT_ORDERS)) return Result.fail("terminal.denied");
                if (t.getLevel() instanceof ServerLevel world) t.runOrders(world);
                return Result.done("terminal.ran");
            }
            default -> {
                return Result.fail("unknown_action");
            }
        }
    }

    /** A player by name: online, else the server's profile cache. */
    static GameProfile profile(MinecraftServer server, String name) {
        if (name == null || name.isBlank()) return null;
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getGameProfile();
        var cache = server.getProfileCache();
        return cache == null ? null : cache.get(name).orElse(null);
    }

    static void send(ServerPlayer player, TradeTerminalMenu menu) {
        PacketDistributor.sendToPlayer(player, new View(build(player, menu)));
    }

    /** The view of one viewer. */
    static TerminalView build(ServerPlayer player, TradeTerminalMenu menu) {
        TradeTerminalBlockEntity t = menu.terminal();
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        int level = t.levelOf(player);
        List<TerminalView.OrderLine> orders = new ArrayList<>();
        for (int i = 0; i < t.orders().size(); i++) {
            TerminalRules.Order o = t.orders().get(i);
            TerminalRules.Status s = t.status(i);
            // 0.1.10.29: an instant sale order shows its limit now and what the surplus of the sell buffer would sell for
            long[] est = estimate(server, d, t, o, day);
            orders.add(new TerminalView.OrderLine(o.side().ordinal(), o.item(), o.amount(), o.mode().ordinal(), o.limit(), o.enabled(),
                s == null ? -1 : s.ordinal(), o.percent(), est[0], est[1], est[2]));
        }
        List<TerminalView.AccessLine> access = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : t.access().entrySet()) access.add(new TerminalView.AccessLine(t.accessName(e.getKey()), e.getValue()));
        List<TerminalView.LogLine> log = new ArrayList<>();
        List<TradeTerminalBlockEntity.LogLine> lines = t.log();
        for (int i = lines.size() - 1; i >= 0 && log.size() < TerminalView.MAX_LOG; i--) {
            var l = lines.get(i);
            log.add(new TerminalView.LogLine(l.day(), l.kind(), l.item(), l.quantity(), l.coins()));
        }
        long coins = -1;
        if (TerminalRules.may(level, Action.EDIT_ORDERS) && t.owner().canAct()) {
            ExchangeData.Mailbox box = d.mail.get(t.owner().uuid());
            coins = box == null ? 0 : box.coins;
        }
        List<CardView.CardLine> cards = new ArrayList<>();
        if (level == TerminalRules.OWNER && player.getUUID().equals(t.owner().uuid()))
            for (ExchangeData.CardRecord c : IdCards.issued(server, player.getUUID()))
                if (cards.size() < CardView.MAX_LINES) cards.add(new CardView.CardLine(c.serial, c.holderName, c.level, c.expiry));
        ExchangeView.SettleView settle = ExchangeViews.settleView(player, d, menu.settlementSlot(), day);
        List<ExchangeView.CurveLine> curves = new ArrayList<>();
        for (ExchangeView.BoxLine l : settle.lines()) ExchangeViews.addCurve(curves, server, d, player.getUUID(), l.id(), day);
        return new TerminalView(t.ownerName(), t.ownerOnline(server), level, t.energy(), t.maxEnergy(), t.hasEnoughEnergy(), t.outputMode(),
            t.teamLevel(), coins, day, orders, access, log, settle, cards, curves);
    }

    /**
     * 0.1.10.29: {limit now, items, coins} of an instant sale of the sell buffer's surplus by a SELL INSTANT / SCRAP order at the
     * market now (the owner's curve, sales today and allowance; resting bids of players not counted); zeros for other orders.
     */
    static long[] estimate(MinecraftServer server, ExchangeData d, TradeTerminalBlockEntity t, TerminalRules.Order o, long day) {
        if (o.side() != TerminalRules.Side.SELL || o.mode() == TerminalRules.Mode.AUCTION || t.owner().uuid() == null) return new long[3];
        ExchangeService.Market m = ExchangeService.market(server, o.item());
        if (m == null || !m.system() || m.currency() != ExchangeData.Currency.COIN) return new long[3];
        long limit = TerminalOrders.limitOf(o, m);
        long surplus = Math.min(ExchangeService.MAX_QUANTITY, TerminalOrders.count(t.sellBuffer(), o.item()) - o.amount());
        if (surplus <= 0) return new long[] {limit, 0, 0};
        UUID owner = t.owner().uuid();
        MinPrice.Quote q = MinPrice.quote(ExchangeService.curve(m), ExchangeService.soldToday(d, owner, m.id(), day), ExchangeService.pressure(d, m.id()),
            limit, surplus, ExchangeService.allowanceLeft(server, d, owner, day));
        return new long[] {limit, q.quantity(), q.coins()};
    }
}
