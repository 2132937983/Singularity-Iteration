// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.10.28 (X4): the account screen of a personal ID card: C2S {@link Request} and {@link CAction}, S2C {@link View}. The
 * server answers only a player who holds a valid personal card of their own in a hand ({@link IdCards#personalCard}), under
 * the {@link PacketRateLimit#EXCHANGE} bucket. From afar a player sees the account and cancels orders; refunds go to the
 * mailbox (nothing moves into the inventory remotely), and revokes the delegate cards they issued.
 */
public final class CardPackets {
    private CardPackets() {}

    /** Kinds: CANCEL_ORDER (a = order), CANCEL_AUCTION (a = auction order), REVOKE (a = serial), REVOKE_ALL. */
    public static final int CANCEL_ORDER = 1, CANCEL_AUCTION = 2, REVOKE = 3, REVOKE_ALL = 4;

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "card_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> { }, b -> new Request());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CAction(int kind, long a) implements CustomPacketPayload {
        public static final Type<CAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "card_action"));
        public static final StreamCodec<FriendlyByteBuf, CAction> CODEC = StreamCodec.of((b, p) -> { b.writeVarInt(p.kind); b.writeVarLong(p.a); },
            b -> new CAction(b.readVarInt(), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record View(CardView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "card_view"));
        public static final StreamCodec<FriendlyByteBuf, View> CODEC = StreamCodec.of((b, p) -> p.view.write(b), b -> new View(CardView.read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, CardPackets::onRequest);
        registrar.playToServer(CAction.TYPE, CAction.CODEC, CardPackets::onAction);
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> context.enqueueWork(() -> CardClientState.set(packet.view())));
    }

    private static void onRequest(Request packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE)) return;
            if (IdCards.personalCard(player) == null) return;
            send(player);
        });
    }

    private static void onAction(CAction p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !PacketRateLimit.allow(player, PacketRateLimit.EXCHANGE)) return;
            if (IdCards.personalCard(player) == null) {
                player.displayClientMessage(Component.translatable("message.mio_icif.card.hold_personal"), true);
                return;
            }
            Result r = switch (p.kind) {
                case CANCEL_ORDER -> ExchangeService.cancel(player, p.a);
                case CANCEL_AUCTION -> ExchangeService.cancelAuction(player, p.a, true);
                case REVOKE, REVOKE_ALL -> {
                    int n = IdCards.revoke(player.server, player.getUUID(), p.kind == REVOKE_ALL ? -1 : p.a);
                    yield n > 0 ? Result.done("card.revoked", n) : Result.fail("card.none_revoked");
                }
                default -> Result.fail("unknown_action");
            };
            player.displayClientMessage(Component.translatable(r.key(), r.args()), true);
            send(player);
        });
    }

    static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new View(build(player)));
    }

    /** The account of the card's holder. */
    static CardView build(ServerPlayer player) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        UUID uuid = player.getUUID();
        String me = uuid.toString();
        ExchangeData.Mailbox box = d.mail.getOrDefault(uuid, new ExchangeData.Mailbox());
        long items = 0;
        List<CardView.MailLine> mail = new ArrayList<>();
        for (Map.Entry<String, Long> e : box.items.entrySet()) {
            items += e.getValue();
            if (mail.size() < CardView.MAX_LINES) mail.add(new CardView.MailLine(e.getKey(), e.getValue()));
        }
        List<CardView.OrderLine> orders = new ArrayList<>();
        for (ExchangeData.Book b : d.books.values())
            for (OrderBook.Order o : b.book.orders())
                if (o.owner().equals(me) && orders.size() < CardView.MAX_LINES)
                    orders.add(new CardView.OrderLine(o.id(), b.commodity, o.buy(), o.price(), o.remaining(), o.day()));
        List<CardView.AuctionLine> auctions = new ArrayList<>();
        for (ExchangeData.AuctionOrder a : d.auction)
            if (a.owner.equals(uuid) && auctions.size() < CardView.MAX_LINES) auctions.add(new CardView.AuctionLine(a.id, a.item, a.quantity, a.minPercent));
        List<CardView.CardLine> cards = new ArrayList<>();
        for (ExchangeData.CardRecord c : IdCards.issued(server, uuid))
            if (cards.size() < CardView.MAX_LINES) cards.add(new CardView.CardLine(c.serial, c.holderName, c.level, c.expiry));
        long allowance = ExchangeService.allowanceLeft(server, d, uuid, day);
        return new CardView(player.getGameProfile().getName(), day, box.coins, box.uu, items, allowance == Long.MAX_VALUE ? -1 : allowance,
            ExchangeService.depthFactor(server), mail, orders, auctions, cards);
    }
}
