package com.miophas.singularity_iteration.common.blockentity.wiring.terminal;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import com.miophas.singularity_iteration.core.runtime.energy.TopologyClock;
import com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Walks an EU cable network from a starting block and lists what is on it - shared by the
 * Energy Management Terminal, the EU meter / voltage detector and its HUD.
 *
 * <p>Conductors are every cable the engine routes through: SI wires ({@link ICableEnergyNode},
 * including the superconducting alloy cable) and the engine's special cables (detector /
 * splitter, {@link IndependentSpecialCableBlockEntity}). Transformers (legacy and engine) and
 * storage boxes (BatBox ... MFSU, GESU) are <b>buffers</b>: they end one voltage segment and start
 * another. {@link Scope#SEGMENT} stops at them; {@link Scope#SYSTEM} passes through them in every
 * direction, so a probe on the cable between a transformer and a storage box still finds the
 * generators and consumers behind both, each tagged with its sub-network.
 *
 * <p>Results are cached per (level, origin, scope) against the {@link TopologyClock}: re-reading a
 * busy factory every few ticks costs a map lookup until a cable or machine actually changes.
 */
public final class NetworkWalker {
    private NetworkWalker() {}

    public enum Scope {
        /** Only the cables reachable without crossing a transformer or storage box. */
        SEGMENT,
        /** The whole electrically connected system, through transformers and storage boxes. */
        SYSTEM
    }

    /** One sub-network: the cables flooded from one start, between buffers. */
    public static final class Subnet {
        public final int index;
        public int conductors;
        /** Highest packet the sub-network's cables are rated for (EU/t per packet), 0 if unknown. */
        public long ratedPacket;
        Subnet(int index) { this.index = index; }
    }

    public static final class Result {
        public final List<BlockPos> devices = new ArrayList<>();
        public final List<EnergyNetworkSnapshot.Category> categories = new ArrayList<>();
        /** Sub-network a device was reached from (a buffer bordering two belongs to the first). */
        public final List<Integer> deviceSubnet = new ArrayList<>();
        public final List<Subnet> subnets = new ArrayList<>();
        /** Sub-networks touching the origin: the segment a probe or terminal sits on. */
        public final IntOpenHashSet originSubnets = new IntOpenHashSet();
        public int conductors;
        public boolean truncated;
        long stamp, madeAt;

        public boolean inOriginSegment(int deviceIndex) {
            return deviceIndex < deviceSubnet.size() && originSubnets.contains((int) deviceSubnet.get(deviceIndex));
        }
    }

    public static boolean isConductor(@Nullable BlockEntity be) {
        if (be instanceof ICableEnergyNode) return true;
        return be instanceof IndependentSpecialCableBlockEntity special && special.conducts();
    }

    public static boolean isTransformer(@Nullable BlockEntity be) {
        return be instanceof mio_icif_transformer || be instanceof IndependentTransformerBlockEntity;
    }

    /** Transformers and storage boxes: they buffer energy and start a new voltage segment. */
    public static boolean isBuffer(@Nullable BlockEntity be) {
        return isTransformer(be) || be instanceof AbstractEnergyStorageBlockEntity
            || be instanceof com.miophas.singularity_iteration.core.api.machine.IStorageMember;
    }

    /**
     * The block entity whose buffer a storage device reports: itself, or the multiblock core of a
     * storage port (GESU). Null for an unformed port - it then has no store of its own to show.
     */
    @Nullable
    public static BlockEntity storageOwner(@Nullable BlockEntity be) {
        if (be instanceof com.miophas.singularity_iteration.core.api.machine.IStorageMember member) return member.storageOwner();
        return be;
    }

    /** Endpoint category of a block entity on a cable, or null when it is no EU endpoint. */
    @Nullable
    public static EnergyNetworkSnapshot.Category categorize(@Nullable BlockEntity be) {
        if (be == null || be instanceof EnergyTerminalBlockEntity) return null;
        if (isTransformer(be)) return EnergyNetworkSnapshot.Category.TRANSFORMER;
        if (be instanceof AbstractEnergyStorageBlockEntity
            || be instanceof com.miophas.singularity_iteration.core.api.machine.IStorageMember) return EnergyNetworkSnapshot.Category.STORAGE;
        if (be instanceof DemandEnergySource) return EnergyNetworkSnapshot.Category.GENERATOR;
        if (be instanceof AbstractEnergyBlockEntity machine && !(be instanceof ICableEnergyNode)) {
            return machine.isPowerSource() ? EnergyNetworkSnapshot.Category.GENERATOR : EnergyNetworkSnapshot.Category.CONSUMER;
        }
        if (be instanceof EnergyComponentHost) return EnergyNetworkSnapshot.Category.CONSUMER;
        return null;
    }

    private static long rating(BlockEntity be) {
        try {
            if (be instanceof AbstractWireBlockEntity wire && wire.getCableTier() != null) return wire.getCableTier().getMaxTransfer();
        } catch (RuntimeException ignored) { }
        return 0;
    }

    // ------------------------------------------------------------------ cache
    /** A cached walk is trusted for this long even without a topology edit (chunk loads, BE swaps). */
    private static final long MAX_AGE_TICKS = 200;
    private record Key(long origin, Scope scope, int maxConductors, int maxDevices) { }
    private static final Map<Level, Map<Key, Result>> CACHE = new WeakHashMap<>();
    private static final int CACHE_LIMIT = 256;

    /** Compatibility form: {@code global} = {@link Scope#SYSTEM}, otherwise {@link Scope#SEGMENT}. */
    public static Result walk(ServerLevel level, BlockPos origin, boolean global, int maxConductors, int maxDevices) {
        return walk(level, origin, global ? Scope.SYSTEM : Scope.SEGMENT, maxConductors, maxDevices);
    }

    /** Cached walk; see {@link #walkUncached}. */
    public static Result walk(ServerLevel level, BlockPos origin, Scope scope, int maxConductors, int maxDevices) {
        long stamp = TopologyClock.stamp(level), now = level.getGameTime();
        Key key = new Key(origin.asLong(), scope, maxConductors, maxDevices);
        Map<Key, Result> perLevel;
        synchronized (CACHE) {
            perLevel = CACHE.computeIfAbsent(level, l -> new java.util.LinkedHashMap<>(16, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Result> eldest) { return size() > CACHE_LIMIT; }
            });
            Result hit = perLevel.get(key);
            if (hit != null && hit.stamp == stamp && now - hit.madeAt < MAX_AGE_TICKS && now >= hit.madeAt) return hit;
        }
        Result fresh = walkUncached(level, origin, scope, maxConductors, maxDevices);
        fresh.stamp = stamp;
        fresh.madeAt = now;
        synchronized (CACHE) { perLevel.put(key, fresh); }
        return fresh;
    }

    /** Test / tooling hook: forget every cached walk. */
    public static void clearCache() { synchronized (CACHE) { CACHE.clear(); } }

    /**
     * Walks from {@code origin} (a terminal, cable, transformer, storage box or endpoint). An
     * origin that is itself a cable starts the flood; otherwise the cables touching it are flooded.
     * A buffer origin is the boundary of every segment it touches, so all of them are "origin".
     */
    public static Result walkUncached(ServerLevel level, BlockPos origin, Scope scope, int maxConductors, int maxDevices) {
        Result r = new Result();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet devices = new LongOpenHashSet();
        LongOpenHashSet expanded = new LongOpenHashSet();
        ArrayDeque<BlockPos> buffers = new ArrayDeque<>();
        boolean system = scope == Scope.SYSTEM;
        visited.add(origin.asLong());
        BlockEntity originBe = level.isLoaded(origin) ? level.getBlockEntity(origin) : null;
        if (isConductor(originBe)) {
            r.originSubnets.add(flood(level, origin, r, visited, devices, buffers, maxConductors, maxDevices));
        } else {
            EnergyNetworkSnapshot.Category originCat = categorize(originBe);
            boolean originIsBuffer = isBuffer(originBe);
            if (originCat != null) addDevice(r, devices, origin, originCat, -1, maxDevices);
            if (originIsBuffer) expanded.add(origin.asLong());
            for (Direction side : Direction.values()) {
                BlockPos next = origin.relative(side);
                if (visited.contains(next.asLong()) || !level.isLoaded(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (isConductor(be)) {
                    r.originSubnets.add(flood(level, next, r, visited, devices, buffers, maxConductors, maxDevices));
                } else if (originIsBuffer && system && isBuffer(be)) {
                    bufferNeighbour(r, devices, buffers, next, be, maxDevices);
                } else if (originCat == null || originIsBuffer) {
                    // a terminal or buffer bolted straight onto a machine still reports it
                    EnergyNetworkSnapshot.Category cat = categorize(be);
                    if (cat != null && !isBuffer(be)) {
                        Subnet s = new Subnet(r.subnets.size());
                        r.subnets.add(s);
                        r.originSubnets.add(s.index);
                        addDevice(r, devices, next, cat, s.index, maxDevices);
                    }
                }
            }
            // the origin device belongs to the first segment it touches
            if (originCat != null && !r.deviceSubnet.isEmpty() && !r.originSubnets.isEmpty())
                r.deviceSubnet.set(0, r.originSubnets.iterator().nextInt());
        }
        if (system) {
            while (!buffers.isEmpty()) {
                BlockPos b = buffers.poll();
                if (!expanded.add(b.asLong())) continue;
                for (Direction side : Direction.values()) {
                    BlockPos next = b.relative(side);
                    if (visited.contains(next.asLong()) || !level.isLoaded(next)) continue;
                    BlockEntity be = level.getBlockEntity(next);
                    if (isConductor(be)) {
                        flood(level, next, r, visited, devices, buffers, maxConductors, maxDevices);
                    } else if (isBuffer(be) && !devices.contains(next.asLong())) {
                        bufferNeighbour(r, devices, buffers, next, be, maxDevices);
                    } else if (!devices.contains(next.asLong())) {
                        // machine docked straight onto a storage box / transformer face
                        EnergyNetworkSnapshot.Category cat = categorize(be);
                        if (cat != null) {
                            Subnet s = new Subnet(r.subnets.size());
                            r.subnets.add(s);
                            addDevice(r, devices, next, cat, s.index, maxDevices);
                        }
                    }
                }
            }
        }
        return r;
    }

    /** A buffer touching a buffer: its own zero-cable sub-network, queued for expansion. */
    private static void bufferNeighbour(Result r, LongOpenHashSet devices, ArrayDeque<BlockPos> buffers, BlockPos at, BlockEntity be, int maxDevices) {
        Subnet s = new Subnet(r.subnets.size());
        r.subnets.add(s);
        addDevice(r, devices, at, categorize(be), s.index, maxDevices);
        buffers.add(at.immutable());
    }

    private static void addDevice(Result r, LongOpenHashSet devices, BlockPos at, EnergyNetworkSnapshot.Category cat, int subnet, int maxDevices) {
        if (cat == null || !devices.add(at.asLong())) return;
        if (r.devices.size() >= maxDevices) { r.truncated = true; return; }
        r.devices.add(at.immutable());
        r.categories.add(cat);
        r.deviceSubnet.add(subnet);
    }

    /** Floods one segment of cables; returns its sub-network index. Buffers found are queued. */
    private static int flood(ServerLevel level, BlockPos start, Result r, LongOpenHashSet visited, LongOpenHashSet devices,
                             ArrayDeque<BlockPos> buffers, int maxConductors, int maxDevices) {
        Subnet subnet = new Subnet(r.subnets.size());
        r.subnets.add(subnet);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.asLong());
        queue.add(start.immutable());
        BlockEntity startBe = level.getBlockEntity(start);
        subnet.conductors++; r.conductors++;
        subnet.ratedPacket = Math.max(subnet.ratedPacket, rating(startBe));
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            BlockEntity from = level.getBlockEntity(at);
            for (Direction side : Direction.values()) {
                if (from instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side)) continue;
                BlockPos next = at.relative(side);
                long key = next.asLong();
                if (visited.contains(key) || !level.isLoaded(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be == null) continue;
                if (isConductor(be)) {
                    if (be instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side.getOpposite())) continue;
                    if ((from instanceof IColoredEnergyTile || be instanceof IColoredEnergyTile) && !IColoredEnergyTile.connects(from, be, side)) continue;
                    if (r.conductors >= maxConductors) { r.truncated = true; continue; }
                    visited.add(key);
                    subnet.conductors++; r.conductors++;
                    subnet.ratedPacket = Math.max(subnet.ratedPacket, rating(be));
                    queue.add(next.immutable());
                    continue;
                }
                // a device already listed (e.g. a transformer bordering two segments) is not re-added,
                // but it must still be queued from this side - the old walker skipped it here
                EnergyNetworkSnapshot.Category cat = categorize(be);
                if (cat == null) continue;
                addDevice(r, devices, next, cat, subnet.index, maxDevices);
                if (isBuffer(be)) buffers.add(next.immutable());
            }
        }
        return subnet.index;
    }

    // ------------------------------------------------------------------ measurement
    /** One tick of energy through the probed segment, plus the whole system's make-up. */
    public record Reading(double throughput, double voltage, long ratedPacket,
                          int generators, int consumers, int storages, int transformers) { }

    /**
     * Energy through the origin segment this tick: what its devices took in (consumers, the
     * storage box or transformer downstream), or - when nothing downstream reports - what the
     * devices feeding it put out. Voltage is the largest packet seen on the segment.
     */
    public static Reading read(ServerLevel level, Result r) {
        double in = 0, out = 0, voltage = 0;
        long rated = 0;
        int gen = 0, use = 0, store = 0, xfmr = 0;
        java.util.Set<Object> stores = new java.util.HashSet<>();
        for (int i = 0; i < r.devices.size(); i++) {
            switch (r.categories.get(i)) {
                case GENERATOR -> gen++;
                case CONSUMER -> use++;
                case STORAGE -> {
                    // a GESU core and its ports are one store
                    BlockPos at = r.devices.get(i);
                    BlockEntity owner = level.isLoaded(at) ? storageOwner(level.getBlockEntity(at)) : null;
                    if (stores.add(owner != null ? owner.getBlockPos().asLong() : (Object) at)) store++;
                }
                case TRANSFORMER -> xfmr++;
            }
            if (!r.inOriginSegment(i)) continue;
            var st = EnergyNetGlobal.getCurrentTickNodeStats(level, r.devices.get(i));
            if (st == null) continue;
            in += st.getEnergyIn();
            out += st.getEnergyOut();
            voltage = Math.max(voltage, st.getVoltage());
        }
        for (int s : r.originSubnets) if (s >= 0 && s < r.subnets.size()) rated = Math.max(rated, r.subnets.get(s).ratedPacket);
        return new Reading(in > 0 ? in : out, voltage, rated, gen, use, store, xfmr);
    }

    /** IC2 voltage tier names by packet size (8 EU ULV ... ). */
    public static String tierName(double packet) {
        String[] names = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UMV", "UXV", "MAX"};
        if (packet <= 0) return "-";
        int tier = EnergyNetGlobal.getTierFromPower(packet);
        return names[Math.min(names.length - 1, tier)];
    }
}
