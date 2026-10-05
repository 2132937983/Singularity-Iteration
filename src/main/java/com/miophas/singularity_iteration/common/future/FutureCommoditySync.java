package com.miophas.singularity_iteration.common.future;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends the datapack commodity list to clients (the market GUI builds its tabs and list from it)
 * together with the commodities the player has not unlocked yet. Sent on login and /reload, and
 * again when the player earns an advancement that unlocks a commodity.
 */
public record FutureCommoditySync(List<Entry> entries, List<String> locked) implements CustomPacketPayload {
    public record Entry(String item, int basePrice, float volatility, String category, int periodDays, String unlock) {
        static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.of((buf, e) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, e.item);
            ByteBufCodecs.VAR_INT.encode(buf, e.basePrice);
            ByteBufCodecs.FLOAT.encode(buf, e.volatility);
            ByteBufCodecs.STRING_UTF8.encode(buf, e.category);
            ByteBufCodecs.VAR_INT.encode(buf, e.periodDays);
            ByteBufCodecs.STRING_UTF8.encode(buf, e.unlock);
        }, buf -> new Entry(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));

        FutureCommodity toCommodity() {
            return new FutureCommodity(item, basePrice, volatility, CommodityCategory.fromId(category), periodDays,
                unlock.isEmpty() ? null : ResourceLocation.parse(unlock));
        }
    }

    public static final Type<FutureCommoditySync> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "future_commodities"));
    public static final StreamCodec<ByteBuf, FutureCommoditySync> CODEC = StreamCodec.composite(
        Entry.CODEC.apply(ByteBufCodecs.list(4096)), FutureCommoditySync::entries,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096)), FutureCommoditySync::locked,
        FutureCommoditySync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FutureCommoditySync packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // the integrated server already holds the same list (and its trades compare commodity identity)
            if (!context.connection().isMemoryConnection()) {
                List<FutureCommodity> list = new ArrayList<>();
                for (Entry e : packet.entries) {
                    try { list.add(e.toCommodity()); } catch (RuntimeException ignored) { }
                }
                FutureCommodityManager.apply(list);
            }
            FutureCommodityManager.setClientLocked(packet.locked);
        });
    }

    /** Only to connections that negotiated the channel (vanilla clients, fake and test players are skipped). */
    public static void send(ServerPlayer player) {
        if (player.connection == null || !player.connection.hasChannel(TYPE)) return;
        PacketDistributor.sendToPlayer(player, forPlayer(player));
    }

    public static FutureCommoditySync forPlayer(ServerPlayer player) {
        List<String> locked = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities())
            if (!FutureCommodityManager.isUnlocked(player, c)) locked.add(c.getItemId());
        return new FutureCommoditySync(FutureCommodityLoader.entries(), locked);
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new FutureCommodityLoader());
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            event.getRelevantPlayers().forEach(FutureCommoditySync::send);
        }

        @SubscribeEvent
        public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            ResourceLocation earned = event.getAdvancement().id();
            for (FutureCommodity c : FutureCommodityManager.getCommodities()) {
                if (earned.equals(c.getUnlockAdvancement())) {
                    send(player);
                    return;
                }
            }
        }
    }
}
