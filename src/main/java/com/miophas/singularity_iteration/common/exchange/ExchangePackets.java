// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_future_elc;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.10.21 (futures M4): the exchange panel packets.
 * <ul>
 *   <li>C2S {@link Request}: the view for the selected market (on open, on selection, once per second while open).</li>
 *   <li>C2S {@link Action}: one exchange action; it costs the energy fee of a trade.</li>
 *   <li>S2C {@link View}: the {@link ExchangeView} of the player.</li>
 * </ul>
 * The server acts only for a player whose open menu is a live futures machine in reach ({@code serves}), under the
 * {@link PacketRateLimit#EXCHANGE} bucket. Client values are never trusted: every rule is checked again in
 * {@link ExchangeService}.
 */
public final class ExchangePackets {
    private ExchangePackets() {}

    public static final int PLACE = 1, CANCEL = 2, COLLECT = 3, POST = 4, DELIVER = 5, WITHDRAW = 6, UU_IN = 7, UU_OUT = 8, FUTURE = 9;
    /** 0.1.10.25 (X1): the sell box (BOX_SELL: flag = into the auction, c = minimum percent) and the auction (AUCTION_CANCEL: a = order). */
    public static final int BOX_SELL = 10, BOX_FILL = 11, BOX_RETURN = 12, AUCTION_CANCEL = 13;
    /** 0.1.10.26 (X3): delivers what the player carries to a system bounty (a = bounty). */
    public static final int BOUNTY_DELIVER = 14;
    /**
     * 0.1.10.27 (X2): the settlement slot. SETTLE_SELL (flag = into the auction, c = minimum percent), SETTLE_BOUNTY (a = bounty),
     * SETTLE_COMMISSION (a = commission, d = quantity), SETTLE_UU, SETTLE_BUY (id = market, a = limit, d = quantity).
     */
    public static final int SETTLE_SELL = 15, SETTLE_BOUNTY = 16, SETTLE_COMMISSION = 17, SETTLE_UU = 18, SETTLE_BUY = 19;
    /**
     * 0.1.10.29: the coin account. COIN_IN (a = coins of the inventory, 0 = all), COIN_OUT (a = coins, 0 = all), BANK_MODE (flag = on),
     * SETTLE_COINS (the coins of the settlement container). BOX_SELL and SETTLE_SELL: a = minimum coins per item (0 = c percent).
     */
    public static final int COIN_IN = 20, COIN_OUT = 21, BANK_MODE = 22, SETTLE_COINS = 23;
    /** 0.1.10.34: BOUNTY_CLAIM / BOUNTY_ABANDON (a = bounty), BOUNTY_REFRESH (the unclaimed NORMAL bounties). */
    public static final int BOUNTY_CLAIM = 24, BOUNTY_ABANDON = 25, BOUNTY_REFRESH = 26;
    /**
     * 0.1.10.35 (items 5-8): SHOP_POST (c = kind 0 exchange / 1 sell / 2 buy, id = goods, d = goods per unit, text = cost item,
     * b = cost items per unit, a = price and units to stock: {@link ShopRules#pack}), SHOP_RESTOCK
     * (a = offer, d = units), SHOP_WITHDRAW (a = offer), SHOP_PAUSE (a = offer, flag = paused), SHOP_TRADE (id = owner UUID,
     * a = offer, d = units), SHOP_COLLECT (the proceeds box), COMMISSION_NOTIFY (flag = on), SETTLE_SHOP (as SHOP_TRADE with the
     * items of the settlement container).
     */
    public static final int SHOP_POST = 27, SHOP_RESTOCK = 28, SHOP_WITHDRAW = 29, SHOP_PAUSE = 30, SHOP_TRADE = 31, SHOP_COLLECT = 32,
        COMMISSION_NOTIFY = 33, SETTLE_SHOP = 34;

    /**
     * The view request: the selected market; 0.1.10.35: the panel's shop query ("" none, "*" the list, "*item" the list of
     * shops that trade the item, an owner's UUID: that shop and the list).
     */
    public record Request(String selected, String shop) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "exchange_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.selected, 128); b.writeUtf(p.shop, 160); }, b -> new Request(b.readUtf(128), b.readUtf(160)));
        public Request(String selected) { this(selected, ""); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 0.1.10.35: the last shop query of each player (the views after an action use it too). */
    static final java.util.Map<java.util.UUID, String> SHOP_QUERY = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * One action. Fields by kind: PLACE (id = market, flag = buy, c = 1 to rest, a = limit, d = quantity); CANCEL (a = order);
     * COLLECT; POST (id = item, d = quantity, c = reward kind 0 coins / 1 item / 2 UU, a = amount, text = reward item, b = days);
     * DELIVER (a = commission, d = quantity); WITHDRAW (a = commission); UU_IN; UU_OUT;
     * FUTURE (id = market, flag = long, d = quantity, c = days); BOX_SELL (flag = auction, c = minimum percent); BOX_FILL;
     * BOX_RETURN; AUCTION_CANCEL (a = order); BOUNTY_DELIVER (a = bounty).
     */
    public record Action(int kind, String id, long a, long b, int c, int d, boolean flag, String text, String selected) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "exchange_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeVarInt(p.kind); buf.writeUtf(p.id, 128); buf.writeVarLong(p.a); buf.writeVarLong(p.b); buf.writeVarInt(p.c); buf.writeVarInt(p.d);
            buf.writeBoolean(p.flag); buf.writeUtf(p.text, 128); buf.writeUtf(p.selected, 128);
        }, buf -> new Action(buf.readVarInt(), buf.readUtf(128), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(),
            buf.readBoolean(), buf.readUtf(128), buf.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record View(ExchangeView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "exchange_view"));
        public static final StreamCodec<FriendlyByteBuf, View> CODEC = StreamCodec.of((b, p) -> p.view.write(b), b -> new View(ExchangeView.read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, ExchangePackets::onRequest);
        registrar.playToServer(Action.TYPE, Action.CODEC, ExchangePackets::onAction);
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> context.enqueueWork(() -> ExchangeClientState.set(packet.view())));
    }

    private static mio_icif_future_elc machine(ServerPlayer player) {
        if (!(player.containerMenu instanceof FutureElcMenu menu)) return null;
        mio_icif_future_elc machine = menu.getBlockEntity();
        return machine != null && machine.serves(player) ? machine : null;
    }

    private static void onRequest(Request packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            mio_icif_future_elc machine = machine(player);
            // 0.1.10.29: the views have their own bucket, so refreshes never use up the actions'
            if (machine == null || !PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE_VIEW)) return;
            Bounties.seen(player);   // 0.1.10.34: an active player
            SHOP_QUERY.put(player.getUUID(), packet.shop == null ? "" : packet.shop);
            send(player, packet.selected, machine);
        });
    }

    private static void onAction(Action p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            mio_icif_future_elc machine = machine(player);
            if (machine == null) return;
            if (!PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE)) {
                // 0.1.10.29: a dropped action says so (before, the click did nothing)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mio_icif.exchange.too_fast"), true);
                return;
            }
            // 0.1.10.25: moving items between the inventory and the sell box is not a trade (no energy fee)
            if (p.kind == BOX_FILL || p.kind == BOX_RETURN) {
                FutureElcMenu menu = (FutureElcMenu) player.containerMenu;
                if (p.kind == BOX_FILL) menu.fillSellBox(player); else menu.returnSellBox(player);
                send(player, p.selected, machine);
                return;
            }
            machine.exchangeAction(player, () -> switch (p.kind) {
                case PLACE -> ExchangeService.place(player, p.id, p.flag, p.c == 1, p.a, p.d);
                case CANCEL -> ExchangeService.cancel(player, p.a);
                case COLLECT -> ExchangeService.collect(player);
                case POST -> ExchangeService.post(player, p.id, p.d, switch (p.c) {
                        case 1 -> ExchangeData.RewardKind.ITEM;
                        case 2 -> ExchangeData.RewardKind.UU;
                        default -> ExchangeData.RewardKind.COIN;
                    }, p.text, p.a, CommissionHours.clamp(p.b, EconomySettings.commissionMaxHours()));   // 0.1.10.35: b = hours
                case DELIVER -> ExchangeService.deliver(player, p.a, p.d);
                case WITHDRAW -> ExchangeService.withdraw(player, p.a);
                case UU_IN -> ExchangeService.depositUu(player);
                case UU_OUT -> ExchangeService.withdrawUu(player);
                case FUTURE -> ExchangeService.openFuture(player, p.id, p.flag, p.d, p.c);
                case BOX_SELL -> ExchangeService.sellBox(player, ((FutureElcMenu) player.containerMenu).sellBox(), p.flag, Math.max(0, Math.min(100, p.c)), p.b == 1,
                    Math.max(0, p.a));
                case COIN_IN -> ExchangeService.depositCoins(player, Math.max(0, p.a));
                case COIN_OUT -> ExchangeService.withdrawCoins(player, Math.max(0, p.a));
                case BANK_MODE -> ExchangeService.setBankMode(player, p.flag);
                case AUCTION_CANCEL -> ExchangeService.cancelAuction(player, p.a);
                case BOUNTY_DELIVER -> Bounties.deliver(player, p.a);
                case BOUNTY_CLAIM -> Bounties.claim(player, p.a);
                case BOUNTY_ABANDON -> Bounties.abandon(player, p.a);
                case BOUNTY_REFRESH -> Bounties.refresh(player);
                // 0.1.10.35
                case SHOP_POST -> Shops.post(player, ShopRules.Kind.values()[Math.max(0, Math.min(2, p.c))], p.id, p.d, p.text, (int) Math.min(ShopRules.MAX_COUNT, Math.max(0, p.b)),
                    ShopRules.packedPrice(p.a), ShopRules.packedUnits(p.a));
                case SHOP_RESTOCK -> Shops.restock(player, p.a, Math.max(0, p.d));
                case SHOP_WITHDRAW -> Shops.withdraw(player, p.a);
                case SHOP_PAUSE -> Shops.pause(player, p.a, p.flag);
                case SHOP_TRADE -> Shops.trade(player, p.id, p.a, Math.max(0, p.d));
                case SHOP_COLLECT -> Shops.collect(player);
                case COMMISSION_NOTIFY -> ExchangeService.commissionNotice(player, p.flag);
                case SETTLE_SELL, SETTLE_BOUNTY, SETTLE_COMMISSION, SETTLE_UU, SETTLE_BUY, SETTLE_COINS, SETTLE_SHOP -> settle(player, p);
                default -> new ExchangeService.Result(false, "message.mio_icif.exchange.unknown_action");
            });
            send(player, p.selected, machine);
            // 0.1.10.26: the trade and bounty events of the action, after it is complete
            ExchangeEvents.flush(player.server);
        });
    }

    /** 0.1.10.27 (X2): an action on the container of the menu's settlement slot. */
    static ExchangeService.Result settle(ServerPlayer player, Action p) {
        FutureElcMenu menu = (FutureElcMenu) player.containerMenu;
        SettlementStorage st = SettlementStorages.open(menu.settlementSlot(), 0);
        if (st == null) return new ExchangeService.Result(false, "message.mio_icif.exchange.settle_empty");
        ExchangeService.Result r = switch (p.kind) {
            case SETTLE_SELL -> Settlement.sell(player, st, p.flag, Math.max(0, Math.min(100, p.c)), p.b == 1, Math.max(0, p.a));
            case SETTLE_COINS -> Settlement.depositCoins(player, st);
            case SETTLE_BOUNTY -> Settlement.deliverBounty(player, st, p.a);
            case SETTLE_COMMISSION -> Settlement.deliverCommission(player, st, p.a, Math.max(1, p.d));
            case SETTLE_UU -> Settlement.depositUu(player, st);
            case SETTLE_SHOP -> {   // 0.1.10.35
                ExchangeService.Result t = Shops.trade(player, p.id, p.a, Math.max(1, p.d), st.source());
                st.commit();
                yield t;
            }
            default -> Settlement.buyInto(player, st, p.id, p.a, p.d);
        };
        menu.broadcastChanges();
        return r;
    }

    static void send(ServerPlayer player, String selected, mio_icif_future_elc machine) {
        PacketDistributor.sendToPlayer(player, new View(ExchangeViews.build(player, selected, machine.hasEnoughEnergy(),
            SHOP_QUERY.getOrDefault(player.getUUID(), ""))));
    }
}
