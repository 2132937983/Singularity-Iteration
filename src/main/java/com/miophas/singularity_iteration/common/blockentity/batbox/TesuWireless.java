package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.core.api.energy.WirelessEnergySources;
import com.miophas.singularity_iteration.core.runtime.TickMarks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.158: the TESU wireless field. A wireless output port in the port slot of a formed TESU covers a cube of ±{@link #RANGE}
 * blocks round itself; a relay covers the same round itself once it is linked (with the TESU wireless connector) to a port or
 * to a relay that leads to one. A relay can only be linked to a node whose cube it stands in, so the field grows step by step
 * and never jumps; links lose nothing.
 *
 * <p>A machine with the wireless upgrade inside the field is filled every tick from the TESU, whatever its voltage, as far as
 * the TESU has energy ({@link WirelessEnergySources}, installed by {@link #install()}). The nodes of each dimension are kept here
 * while their chunks are loaded.
 */
public final class TesuWireless {
    private TesuWireless() {}

    /** Blocks the cube reaches from a node on each axis. */
    public static final int RANGE = 16;
    /** Links followed from a relay to its port, at most. */
    public static final int MAX_LINKS = 256;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> NODES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Set<BlockPos>>> NODES_BY_CHUNK = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, TickFeedCache> FEED_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean eventsRegistered;

    private static final class TickFeedCache {
        long tick = Long.MIN_VALUE;
        final Map<BlockPos, TesuControllerBlockEntity> byNode = new HashMap<>();
    }

    public static void install() {
        WirelessEnergySources.install(TesuWireless::draw);
        if (!eventsRegistered) {
            eventsRegistered = true;
            NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> clearAll());
            NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
                if (e.getLevel() instanceof Level level) clearDimension(level.dimension());
            });
        }
    }

    private static void clearAll() {
        NODES.clear();
        NODES_BY_CHUNK.clear();
        FEED_CACHE.clear();
    }

    private static void clearDimension(ResourceKey<Level> dim) {
        NODES.remove(dim);
        NODES_BY_CHUNK.remove(dim);
        FEED_CACHE.remove(dim);
    }

    static void add(Level level, BlockPos pos) {
        BlockPos imm = pos.immutable();
        ResourceKey<Level> dim = level.dimension();
        NODES.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet()).add(imm);
        Long2ObjectOpenHashMap<Set<BlockPos>> byChunk = NODES_BY_CHUNK.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>());
        synchronized (byChunk) {
            byChunk.computeIfAbsent(ChunkPos.asLong(imm), k -> ConcurrentHashMap.newKeySet()).add(imm);
        }
        FEED_CACHE.remove(dim);
    }

    static void remove(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        Set<BlockPos> set = NODES.get(dim);
        if (set != null) set.remove(pos);
        Long2ObjectOpenHashMap<Set<BlockPos>> byChunk = NODES_BY_CHUNK.get(dim);
        if (byChunk != null) {
            synchronized (byChunk) {
                long ck = ChunkPos.asLong(pos);
                Set<BlockPos> bucket = byChunk.get(ck);
                if (bucket != null) {
                    bucket.remove(pos);
                    if (bucket.isEmpty()) byChunk.remove(ck);
                }
            }
        }
        FEED_CACHE.remove(dim);
    }

    /** Whether the cube of a node at {@code node} holds {@code at}. */
    public static boolean covers(BlockPos node, BlockPos at) {
        return Math.abs(node.getX() - at.getX()) <= RANGE && Math.abs(node.getY() - at.getY()) <= RANGE && Math.abs(node.getZ() - at.getZ()) <= RANGE;
    }

    /** Whether a wireless port or a relay stands at {@code at}. */
    public static boolean isNode(@Nullable BlockEntity be) { return be instanceof TesuWirelessPortBlockEntity || be instanceof TesuRelayBlockEntity; }

    /**
     * The TESU feeding the node at {@code node}, or {@code null}: a port's formed controller; for a relay, its links followed to a
     * port (each relay within the cube of the node it is linked to, every chunk on the way loaded, no loop).
     */
    @Nullable
    public static TesuControllerBlockEntity feed(Level level, BlockPos node) {
        Set<BlockPos> seen = new HashSet<>();
        BlockPos at = node;
        for (int i = 0; i < MAX_LINKS; i++) {
            if (!seen.add(at) || !level.isLoaded(at)) return null;
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof TesuWirelessPortBlockEntity port) return port.controller();
            if (!(be instanceof TesuRelayBlockEntity relay) || relay.parent() == null) return null;
            BlockPos parent = relay.parent();
            if (!covers(parent, at)) return null;
            at = parent;
        }
        return null;
    }

    /** Whether following the links from {@code from} reaches {@code target} (linking target to from would close a loop). */
    public static boolean leadsTo(Level level, BlockPos from, BlockPos target) {
        Set<BlockPos> seen = new HashSet<>();
        BlockPos at = from;
        for (int i = 0; i < MAX_LINKS && at != null; i++) {
            if (at.equals(target)) return true;
            if (!seen.add(at) || !level.isLoaded(at) || !(level.getBlockEntity(at) instanceof TesuRelayBlockEntity relay)) return false;
            at = relay.parent();
        }
        return false;
    }

    /** EU for a machine at {@code at}: from the first node whose cube holds it and that a TESU feeds, at most {@code wanted}. */
    static long draw(ServerLevel level, BlockPos at, long wanted) {
        ResourceKey<Level> dim = level.dimension();
        Long2ObjectOpenHashMap<Set<BlockPos>> byChunk = NODES_BY_CHUNK.get(dim);
        if (byChunk == null || byChunk.isEmpty()) return 0L;
        TickFeedCache cache = FEED_CACHE.computeIfAbsent(dim, k -> new TickFeedCache());
        long now = level.getGameTime();
        if (cache.tick != now) {
            cache.tick = now;
            cache.byNode.clear();
        }
        int cx = at.getX() >> 4, cz = at.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Set<BlockPos> bucket;
                synchronized (byChunk) {
                    bucket = byChunk.get(ChunkPos.asLong(cx + dx, cz + dz));
                }
                if (bucket == null || bucket.isEmpty()) continue;
                for (BlockPos node : bucket) {
                    if (!covers(node, at)) continue;
                    TesuControllerBlockEntity core;
                    if (cache.byNode.containsKey(node)) {
                        core = cache.byNode.get(node);
                    } else {
                        core = feed(level, node);
                        cache.byNode.put(node, core);
                    }
                    if (core == null || core.isRemoved() || core.getStoredEnergy() <= 0) continue;
                    long taken = core.consumeEnergy(Math.min(wanted, core.getStoredEnergy()), false);
                    if (taken > 0) {
                        TickMarks.saved(core);
                        TesuWirelessBeams.fed(level, node, at);   // 0.1.11.188: the stream from the node to the machine
                        return taken;
                    }
                }
            }
        }
        return 0L;
    }

    /** The nodes known in a dimension (tests). */
    public static int nodeCount(Level level) {
        Set<BlockPos> set = NODES.get(level.dimension());
        return set == null ? 0 : set.size();
    }
}
