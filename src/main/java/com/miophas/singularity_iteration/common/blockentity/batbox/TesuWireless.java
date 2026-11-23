package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.core.api.energy.WirelessEnergySources;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    public static void install() { WirelessEnergySources.install(TesuWireless::draw); }

    static void add(Level level, BlockPos pos) { NODES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable()); }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = NODES.get(level.dimension());
        if (set != null) set.remove(pos);
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
        Set<BlockPos> nodes = NODES.get(level.dimension());
        if (nodes == null || nodes.isEmpty()) return 0L;
        for (BlockPos node : nodes) {
            if (!covers(node, at)) continue;
            TesuControllerBlockEntity core = feed(level, node);
            if (core == null || core.getStoredEnergy() <= 0) continue;
            long taken = core.consumeEnergy(Math.min(wanted, core.getStoredEnergy()), false);
            if (taken > 0) {
                core.setChanged();
                TesuWirelessBeams.fed(level, node, at);   // 0.1.11.188: the stream from the node to the machine
                return taken;
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
