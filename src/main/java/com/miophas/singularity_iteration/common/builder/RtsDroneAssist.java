// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryRemoteService;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * RTS Building: Build From Above link (0.1.8.0, soft dependency, checked against 1.1.7).
 *
 * <p>When a player carries a paired Armory Remote and RTS Building runs a batch placement or an
 * area mining job for that player, builder drones fly out to the work: they go to each block
 * that the job places or mines, carry the batch material and show the placement beam.
 *
 * <p>Data: the public RTS API ({@code RtsAPI.get().placement()} and {@code .mining()}) gives the
 * remaining blocks and the batch item, not the positions. The positions come from the NeoForge
 * neighbour notify event, which every block change with neighbour updates fires. The listener
 * is active only while a job runs (one map lookup otherwise). A position counts only when it is
 * within {@link #RANGE} blocks of the player and the new block is the batch item (placement) or
 * air (area mining). The drones are client visuals; RTS Building places and mines the blocks.
 * The Material Interface exposes an item handler, so RTS Building can link it as storage.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class RtsDroneAssist {
    private RtsDroneAssist() {}

    static final String RTS_MOD = "rtsbuilding";
    static final double RANGE = 96;
    static final int MAX_QUEUE = 32, MAX_SEND = 8;

    private static final int UNKNOWN = 0, READY = 1, OFF = -1;
    private static int state = UNKNOWN;
    private static MethodHandle apiGet, placement, mining, placeRemaining, placeItem, mineRemaining;

    /** A running RTS job of one player. Server thread only. */
    private static final class Watch {
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        String itemId = "";
        boolean placing, mining;
        final ArrayDeque<BlockPos> sites = new ArrayDeque<>();
        Watch(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) { this.dimension = dimension; }
    }

    private static final Map<UUID, Watch> WATCH = new HashMap<>();

    // ------------------------------------------------------------------ RTS API (reflection, cached)
    private static boolean ready() {
        if (state == UNKNOWN) init();
        return state == READY;
    }

    private static void init() {
        if (!ModList.get().isLoaded(RTS_MOD)) { state = OFF; return; }
        try {
            ClassLoader cl = RtsDroneAssist.class.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> api = Class.forName("com.rtsbuilding.rtsbuilding.api.RtsAPI", false, cl);
            Class<?> place = Class.forName("com.rtsbuilding.rtsbuilding.api.RtsPlacementAPI", false, cl);
            Class<?> mine = Class.forName("com.rtsbuilding.rtsbuilding.api.RtsMiningAPI", false, cl);
            apiGet = lookup.findStatic(api, "get", MethodType.methodType(api));
            placement = lookup.findVirtual(api, "placement", MethodType.methodType(place));
            mining = lookup.findVirtual(api, "mining", MethodType.methodType(mine));
            placeRemaining = lookup.findVirtual(place, "getPlaceBatchRemainingBlocks", MethodType.methodType(int.class, ServerPlayer.class));
            placeItem = lookup.findVirtual(place, "getPlaceBatchItemId", MethodType.methodType(String.class, ServerPlayer.class));
            mineRemaining = lookup.findVirtual(mine, "getAreaDestroyRemainingBlocks", MethodType.methodType(int.class, ServerPlayer.class));
            state = READY;
            Singularity_Iteration.LOGGER.info("Armory: RTS Building found, builder drones assist its jobs");
        } catch (Throwable t) {
            state = OFF;
            Singularity_Iteration.LOGGER.warn("Armory: RTS Building API link failed, no drone assist ({})", t.toString());
        }
    }

    // ------------------------------------------------------------------ server events
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 10 != 0 || !ready()) return;
        try {
            Object api = apiGet.invoke();
            Object place = placement.invoke(api), mine = mining.invoke(api);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int placeLeft = 0, mineLeft = 0;
                String item = "";
                if (!ArmoryRemoteService.findRemote(player).isEmpty()) {
                    placeLeft = (int) placeRemaining.invoke(place, player);
                    mineLeft = (int) mineRemaining.invoke(mine, player);
                    if (placeLeft > 0) { Object id = placeItem.invoke(place, player); item = id == null ? "" : id.toString(); }
                }
                if (placeLeft <= 0 && mineLeft <= 0) { WATCH.remove(player.getUUID()); continue; }
                Watch w = WATCH.get(player.getUUID());
                if (w == null || w.dimension != player.level().dimension()) {
                    w = new Watch(player.level().dimension());
                    WATCH.put(player.getUUID(), w);
                }
                w.placing = placeLeft > 0;
                w.mining = mineLeft > 0;
                w.itemId = item;
                if (!w.sites.isEmpty()) {
                    List<BlockPos> send = new ArrayList<>();
                    while (!w.sites.isEmpty() && send.size() < MAX_SEND) send.add(w.sites.pollFirst());
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new Sites(player.getId(), w.placing ? w.itemId : "", w.mining && !w.placing, send));
                }
            }
        } catch (Throwable t) {
            state = OFF;
            WATCH.clear();
            Singularity_Iteration.LOGGER.warn("Armory: RTS Building API call failed, drone assist off ({})", t.toString());
        }
    }

    @SubscribeEvent
    static void onBlockChange(BlockEvent.NeighborNotifyEvent event) {
        if (WATCH.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        boolean air = event.getState().isAir();
        String id = air ? "" : BuiltInRegistries.ITEM.getKey(event.getState().getBlock().asItem()).toString();
        for (Map.Entry<UUID, Watch> e : WATCH.entrySet()) {
            Watch w = e.getValue();
            if (w.dimension != level.dimension()) continue;
            boolean match = air ? w.mining : w.placing && id.equals(w.itemId);
            if (!match) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (player == null || player.level() != level || player.blockPosition().distSqr(pos) > RANGE * RANGE) continue;
            if (pos.equals(w.sites.peekLast())) continue;
            w.sites.addLast(pos.immutable());
            while (w.sites.size() > MAX_QUEUE) w.sites.pollFirst();
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WATCH.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        WATCH.clear();
        state = UNKNOWN;
    }

    // ------------------------------------------------------------------ packet
    /**
     * Server to client: new RTS work sites of one player (at most {@link #MAX_SEND} positions).
     * Sent to that player and to the players that track the player, at most twice a second.
     * {@code itemId} is the batch item (empty for mining). The client only draws drones.
     */
    public record Sites(int playerId, String itemId, boolean mining, List<BlockPos> sites) implements CustomPacketPayload {
        public static final Type<Sites> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rts_drone_sites"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sites> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.playerId);
                buf.writeUtf(p.itemId, 128);
                buf.writeBoolean(p.mining);
                int n = Math.min(MAX_SEND, p.sites.size());
                buf.writeVarInt(n);
                for (int i = 0; i < n; i++) buf.writeBlockPos(p.sites.get(i));
            },
            buf -> {
                int id = buf.readVarInt();
                String item = buf.readUtf(128);
                boolean mining = buf.readBoolean();
                int n = Math.min(MAX_SEND, buf.readVarInt());
                List<BlockPos> sites = new ArrayList<>(n);
                for (int i = 0; i < n; i++) sites.add(buf.readBlockPos());
                return new Sites(id, item, mining, sites);
            });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Sites packet, IPayloadContext context) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) return;
            context.enqueueWork(() -> {
                Runnable hook = CLIENT_HOOK;
                LAST = packet;
                if (hook != null) hook.run();
            });
        }
    }

    /** Client side: the last received sites and the visual hook (installed by the client setup). */
    public static volatile Sites LAST;
    public static volatile Runnable CLIENT_HOOK;
}
