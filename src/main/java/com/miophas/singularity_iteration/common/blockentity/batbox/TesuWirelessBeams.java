// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.11.188: the particle streams of the TESU wireless field. A wireless output port or a relay that fills a machine
 * ({@link TesuWireless#draw}) is noted here with the machine; every {@link #SEND_EVERY} ticks each node that fed machines in
 * the last {@link #FRESH} ticks sends the players near it one {@link Packet} with those machines (at most {@link #MAX_TARGETS}),
 * and their clients draw the wireless node's particle stream from the node to each machine for a while (the same stream a relay
 * draws from the node it is linked to). One small packet a node every half second; nothing while no machine takes energy.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class TesuWirelessBeams {
    private TesuWirelessBeams() {}

    public static final int SEND_EVERY = 10, FRESH = 20, MAX_TARGETS = 64;

    /** Client side: a received packet (set by the client setup; the server never calls it). */
    public static volatile java.util.function.Consumer<Packet> CLIENT_HOOK = p -> { };

    /** By dimension: node -> machine -> the game time it was last fed. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Map<BlockPos, Long>>> FED = new ConcurrentHashMap<>();

    /** The node at {@code node} has just filled the machine at {@code machine}. */
    static void fed(ServerLevel level, BlockPos node, BlockPos machine) {
        FED.computeIfAbsent(level.dimension(), k -> new HashMap<>()).computeIfAbsent(node, k -> new HashMap<>())
            .put(machine.immutable(), level.getGameTime());
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (FED.isEmpty() || event.getServer().getTickCount() % SEND_EVERY != 0) return;
        for (Iterator<Map.Entry<ResourceKey<Level>, Map<BlockPos, Map<BlockPos, Long>>>> d = FED.entrySet().iterator(); d.hasNext(); ) {
            var dimension = d.next();
            ServerLevel level = event.getServer().getLevel(dimension.getKey());
            if (level == null) { d.remove(); continue; }
            long now = level.getGameTime();
            for (Iterator<Map.Entry<BlockPos, Map<BlockPos, Long>>> it = dimension.getValue().entrySet().iterator(); it.hasNext(); ) {
                var node = it.next();
                node.getValue().values().removeIf(t -> now - t > FRESH || now < t);
                if (node.getValue().isEmpty() || !level.isLoaded(node.getKey())) { it.remove(); continue; }
                long[] targets = node.getValue().keySet().stream().limit(MAX_TARGETS).mapToLong(BlockPos::asLong).toArray();
                boolean relay = level.getBlockEntity(node.getKey()) instanceof TesuRelayBlockEntity;
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(node.getKey()), new Packet(node.getKey(), relay, targets));
            }
            if (dimension.getValue().isEmpty()) d.remove();
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) { FED.clear(); }

    /** The node at {@code node} (a relay, or a wireless port) is filling the machines at {@code targets} ({@link BlockPos#asLong}). */
    public record Packet(BlockPos node, boolean relay, long[] targets) implements CustomPacketPayload {
        public static final Type<Packet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tesu_wireless_beam"));
        public static final StreamCodec<FriendlyByteBuf, Packet> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.node);
                buf.writeBoolean(p.relay);
                buf.writeVarInt(p.targets.length);
                for (long t : p.targets) buf.writeLong(t);
            },
            buf -> {
                BlockPos node = buf.readBlockPos();
                boolean relay = buf.readBoolean();
                int n = buf.readVarInt();
                if (n < 0 || n > MAX_TARGETS) throw new IllegalArgumentException("tesu beam targets: " + n);
                long[] targets = new long[n];
                for (int i = 0; i < n; i++) targets[i] = buf.readLong();
                return new Packet(node, relay, targets);
            });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Packet p, IPayloadContext context) {
            context.enqueueWork(() -> CLIENT_HOOK.accept(p));
        }
    }

    /** The nodes noted in a dimension (tests). */
    public static int fedNodes(Level level) {
        Map<BlockPos, Map<BlockPos, Long>> nodes = FED.get(level.dimension());
        return nodes == null ? 0 : nodes.size();
    }

    /** Whether the node at {@code node} is noted as filling the machine at {@code machine} (tests). */
    public static boolean feeds(Level level, BlockPos node, BlockPos machine) {
        Map<BlockPos, Map<BlockPos, Long>> nodes = FED.get(level.dimension());
        return nodes != null && nodes.containsKey(node) && nodes.get(node).containsKey(machine);
    }
}
