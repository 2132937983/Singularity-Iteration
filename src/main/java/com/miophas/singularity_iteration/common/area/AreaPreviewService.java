package com.miophas.singularity_iteration.common.area;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.network.AreaPreviewPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of the Area Preview. Work areas are always computed from the authoritative
 * server entity and sent to the requesting player only.
 *
 * <p>Menus in SI do not share a common "block position" API, so the GUI button relies on
 * the block the player last right-clicked: {@link #rememberInteraction} records it, and a
 * request is honoured only while that player still has a container open, is close to the
 * machine and the machine still provides an area.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class AreaPreviewService {
    public static final int DEFAULT_TICKS = 20 * 30;
    public static final int NEARBY_RADIUS = 32;
    private static final double MAX_REQUEST_DISTANCE_SQ = 12.0 * 12.0;

    private record Interaction(BlockPos pos, long time) { }
    private static final Map<UUID, Interaction> LAST_INTERACTION = new ConcurrentHashMap<>();

    private AreaPreviewService() {}

    @SubscribeEvent
    static void rememberInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof WorkAreaProvider) {
            LAST_INTERACTION.put(event.getEntity().getUUID(), new Interaction(event.getPos().immutable(), event.getLevel().getGameTime()));
        }
    }

    @SubscribeEvent
    static void forget(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_INTERACTION.remove(event.getEntity().getUUID());
    }

    /** Areas of one machine, or an empty list when it has none. Never throws. */
    public static List<WorkArea> areasOf(BlockEntity entity) {
        if (!(entity instanceof WorkAreaProvider provider)) return List.of();
        try {
            List<WorkArea> areas = provider.workAreas();
            return areas == null ? List.of() : areas;
        } catch (RuntimeException e) {
            Singularity_Iteration.LOGGER.warn("Work area of {} failed", entity.getBlockPos(), e);
            return List.of();
        }
    }

    /** GUI button: toggle the preview for the machine whose menu the player has open. */
    public static void requestFromMenu(ServerPlayer player, int containerId) {
        if (player.containerMenu == null || player.containerMenu.containerId != containerId) return;
        Interaction last = LAST_INTERACTION.get(player.getUUID());
        if (last == null) return;
        BlockPos pos = last.pos();
        if (player.distanceToSqr(pos.getCenter()) > MAX_REQUEST_DISTANCE_SQ) return;
        sendFor(player, pos, true);
    }

    /** Toggle the preview of one machine for a player; false when the block has no area. */
    public static boolean sendFor(ServerPlayer player, BlockPos pos, boolean toggle) {
        BlockEntity entity = player.level().getBlockEntity(pos);
        List<WorkArea> areas = areasOf(entity);
        if (areas.isEmpty()) return false;
        send(player, new AreaPreviewPacket(List.of(new AreaPreviewPacket.Entry(pos.immutable(), areas)), toggle, DEFAULT_TICKS));
        return true;
    }

    /** Show every ranged machine around the player (scanner used in the air). Returns the count. */
    public static int sendNearby(ServerPlayer player, int radius) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        List<AreaPreviewPacket.Entry> entries = new ArrayList<>();
        BlockPos center = player.blockPosition();
        int chunkRadius = (radius >> 4) + 1;
        for (int cx = -chunkRadius; cx <= chunkRadius && entries.size() < AreaPreviewPacket.MAX_ENTRIES; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius && entries.size() < AreaPreviewPacket.MAX_ENTRIES; cz++) {
                var chunk = level.getChunkSource().getChunkNow((center.getX() >> 4) + cx, (center.getZ() >> 4) + cz);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof WorkAreaProvider)) continue;
                    if (entity.getBlockPos().distSqr(center) > (double) radius * radius) continue;
                    List<WorkArea> areas = areasOf(entity);
                    if (!areas.isEmpty()) entries.add(new AreaPreviewPacket.Entry(entity.getBlockPos().immutable(), areas));
                    if (entries.size() >= AreaPreviewPacket.MAX_ENTRIES) break;
                }
            }
        }
        if (!entries.isEmpty()) send(player, new AreaPreviewPacket(entries, false, DEFAULT_TICKS));
        return entries.size();
    }

    /** Clears every preview on the player's client. */
    public static void clear(ServerPlayer player) {
        send(player, new AreaPreviewPacket(List.of(), false, 0));
    }

    /** Clients without SI (or test connections) did not negotiate the channel; skip them quietly. */
    private static void send(ServerPlayer player, AreaPreviewPacket packet) {
        if (player.connection != null && player.connection.hasChannel(AreaPreviewPacket.TYPE)) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }
}
