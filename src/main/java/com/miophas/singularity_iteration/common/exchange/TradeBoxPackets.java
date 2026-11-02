// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeBoxBlockEntity;
import com.miophas.singularity_iteration.common.menu.producer.TradeBoxMenu;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.10.42: the packets of the Trade Box panel: the view request (once per second while the panel is open), the settings
 * and filter actions (owner and operators: only they open the box), the view. The server checks the menu again on every
 * packet.
 */
public final class TradeBoxPackets {
    private TradeBoxPackets() {}

    /**
     * Actions: MODE (value {@link TradeBoxRules#AUCTION} ..), PERCENT (value 0..100), COINS (value 1..999,999,999), IN_COINS,
     * AUTO_DELIVER, OFFLINE_AUCTION (value 1 on, 0 off), FILTER (index: the item the player holds on the cursor, or empty),
     * FILTER_CLEAR.
     */
    public static final int MODE = 0, PERCENT = 1, COINS = 2, IN_COINS = 3, AUTO_DELIVER = 4, OFFLINE_AUCTION = 5, FILTER = 6, FILTER_CLEAR = 7;

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "trade_box_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> { }, b -> new Request());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BAction(int kind, int index, long value) implements CustomPacketPayload {
        public static final Type<BAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "trade_box_action"));
        public static final StreamCodec<FriendlyByteBuf, BAction> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.kind); b.writeVarInt(p.index); b.writeVarLong(p.value); },
            b -> new BAction(b.readVarInt(), b.readVarInt(), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record View(TradeBoxView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "trade_box_view"));
        public static final StreamCodec<FriendlyByteBuf, View> CODEC = StreamCodec.of((b, p) -> p.view.write(b), b -> new View(TradeBoxView.read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, TradeBoxPackets::onRequest);
        registrar.playToServer(BAction.TYPE, BAction.CODEC, TradeBoxPackets::onAction);
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> context.enqueueWork(() -> TradeBoxClientState.set(packet.view())));
    }

    private static TradeBoxMenu menu(ServerPlayer player) {
        if (!(player.containerMenu instanceof TradeBoxMenu menu) || menu.box() == null || !menu.stillValid(player)) return null;
        return menu;
    }

    private static void onRequest(Request packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TradeBoxMenu menu = menu(player);
            if (menu == null || !PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE_VIEW)) return;
            send(player, menu.box());
        });
    }

    private static void onAction(BAction p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TradeBoxMenu menu = menu(player);
            if (menu == null) return;
            if (!PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE)) {
                player.displayClientMessage(Component.translatable("message.mio_icif.exchange.too_fast"), true);
                return;
            }
            act(menu, menu.box(), p);
            send(player, menu.box());
        });
    }

    /** Applies an action to the box ({@link TradeBoxRules#of} clamps the settings to the tier). */
    static void act(TradeBoxMenu menu, TradeBoxBlockEntity box, BAction p) {
        TradeBoxRules.Settings s = box.settings();
        boolean on = p.value() != 0;
        switch (p.kind()) {
            case MODE -> box.setSettings(new TradeBoxRules.Settings((int) Math.max(0, Math.min(TradeBoxRules.MODES - 1, p.value())), s.percent(),
                s.coins(), s.inCoins(), s.autoDeliver(), s.offlineAuction()));
            case PERCENT -> box.setSettings(new TradeBoxRules.Settings(s.mode(), TradeBoxRules.percent(p.value()), s.coins(), s.inCoins(),
                s.autoDeliver(), s.offlineAuction()));
            case COINS -> box.setSettings(new TradeBoxRules.Settings(s.mode(), s.percent(), TradeBoxRules.coins(p.value()), s.inCoins(),
                s.autoDeliver(), s.offlineAuction()));
            case IN_COINS -> box.setSettings(new TradeBoxRules.Settings(s.mode(), s.percent(), s.coins(), on, s.autoDeliver(), s.offlineAuction()));
            case AUTO_DELIVER -> box.setSettings(new TradeBoxRules.Settings(s.mode(), s.percent(), s.coins(), s.inCoins(), on, s.offlineAuction()));
            case OFFLINE_AUCTION -> box.setSettings(new TradeBoxRules.Settings(s.mode(), s.percent(), s.coins(), s.inCoins(), s.autoDeliver(), on));
            case FILTER -> {
                // the item on the cursor of the server's menu (not an id the client names)
                ItemStack held = menu.getCarried();
                box.setFilter(p.index(), held.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            }
            case FILTER_CLEAR -> box.clearFilter();
            default -> { }
        }
    }

    static void send(ServerPlayer player, TradeBoxBlockEntity box) {
        PacketDistributor.sendToPlayer(player, new View(build(player.server, box)));
    }

    /** The view of the box. */
    public static TradeBoxView build(MinecraftServer server, TradeBoxBlockEntity box) {
        long day = ExchangeService.today(server);
        TradeBoxRules.Settings s = box.settings();
        List<Long> today = new ArrayList<>();
        for (long v : box.counters(day)) today.add(v);
        TerminalRules.Status st = box.lastStatus();
        return new TradeBoxView(box.tier(), box.ownerName(), box.ownerOnline(server), s.mode(), s.percent(), s.coins(), s.inCoins(), s.autoDeliver(),
            s.offlineAuction(), box.filter(), st == null ? -1 : st.ordinal(), today,
            box.owner().canAct() ? Bounties.claimedBy(ExchangeData.get(server), box.owner().uuid()) : 0, day);
    }
}
