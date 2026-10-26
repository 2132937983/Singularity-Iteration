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
 * Walks an EU cable network from a starting block and lists what is on it. The Energy Management
 * Terminal, the EU meter, the voltage detector and the meter HUD use this class.
 *
 * <p>This walker is a read-only display tool. It does not take part in energy transfer. The engine
 * uses its own index ({@code PlatformTopology} and {@code ConductorRegistry}). The two can differ for
 * a short time after a change.
 *
 * <h2>Terms</h2>
 * <ul>
 *   <li><b>Conductor.</b> Every cable the engine routes through: SI wires ({@link ICableEnergyNode},
 *       including the superconducting alloy cable) and the engine's special cables (detector,
 *       splitter, {@link IndependentSpecialCableBlockEntity}) while they conduct.</li>
 *   <li><b>Buffer.</b> Transformers (legacy and engine) and storage boxes (BatBox to MFSU, GESU and its
 *       ports). A buffer ends one voltage segment and starts another.</li>
 *   <li><b>Device.</b> A block entity with an endpoint category ({@link #categorize}). Buffers are
 *       devices too.</li>
 *   <li><b>Sub-network ({@link Subnet}).</b> The conductors that one flood reaches, plus the devices
 *       that touch them. A buffer that touches only buffers or machines makes a sub-network with zero
 *       conductors.</li>
 * </ul>
 *
 * <h2>Scopes</h2>
 * <p>{@link Scope#SEGMENT} stops at buffers. {@link Scope#SYSTEM} passes through them in every
 * direction. Thus a probe on the cable between a transformer and a storage box still finds the
 * generators and consumers behind both. Each device is tagged with its sub-network.
 *
 * <h2>Algorithm</h2>
 * <p>The walk is a breadth-first search over the six neighbours of each block. One
 * {@code visited} set holds every conductor position and the origin. A conductor is visited at most
 * once in the whole walk. Thus each conductor belongs to exactly one sub-network. One
 * {@code devices} set holds every device position. A device is listed at most once, under the first
 * sub-network that reaches it. In {@code SYSTEM} scope, each buffer found goes into a queue. Each
 * buffer is expanded at most once ({@code expanded} set). Each conductor neighbour of an expanded
 * buffer that is not visited starts a new flood, and so a new sub-network.
 *
 * <p>Two conductors connect when neither blocks the shared face ({@link ICableEnergyNode#isDirectionBlocked})
 * and their colours match ({@link IColoredEnergyTile#connects}). A conductor connects to a device
 * when the conductor does not block that face. The walk does not check the face of the device.
 *
 * <h2>Limits</h2>
 * <p>{@code maxConductors} limits the conductors that a flood adds from a neighbour. {@code maxDevices}
 * limits the length of {@link Result#devices}. When a limit stops the walk, {@link Result#truncated}
 * is true. The limits protect the server tick from a very large network. They are not exact: the
 * start conductor of each flood is always counted, and buffers past the device limit are still
 * expanded in {@code SYSTEM} scope.
 *
 * <h2>Chunk rules and threading</h2>
 * <p>The walk never loads a chunk. It checks {@link Level#isLoaded} before it reads a neighbour.
 * A network that crosses an unloaded chunk is cut at that chunk, and {@code truncated} stays false.
 * Call the walk methods only on the server thread, because they read block entities.
 *
 * <h2>Cache</h2>
 * <p>Results are cached per (level, origin, scope, limits) against the {@link TopologyClock}.
 * Re-reading a busy factory every few ticks costs a map lookup until a cable or machine changes.
 * A cached result is used for at most {@link #MAX_AGE_TICKS} game ticks, because some changes
 * (chunk loads, block entity swaps) do not advance the clock.
 */
public final class NetworkWalker {
    private NetworkWalker() {}

    /** How far a walk goes past buffers. */
    public enum Scope {
        /** Only the cables reachable without crossing a transformer or storage box. */
        SEGMENT,
        /** The whole electrically connected system, through transformers and storage boxes. */
        SYSTEM
    }

    /** One sub-network: the cables flooded from one start, between buffers. */
    public static final class Subnet {
        /** Position of this sub-network in {@link Result#subnets}. */
        public final int index;
        /** Number of conductors in this sub-network. */
        public int conductors;
        /** Highest packet the sub-network's cables are rated for (EU/t per packet), 0 if unknown. */
        public long ratedPacket;
        Subnet(int index) { this.index = index; }
    }

    /**
     * Result of one walk.
     *
     * <p>{@link #devices}, {@link #categories} and {@link #deviceSubnet} are parallel lists. Index
     * {@code i} of each list describes the same device. A cached result is shared between callers.
     * Callers must not change its lists or fields.
     */
    public static final class Result {
        /** Device positions, in discovery order. */
        public final List<BlockPos> devices = new ArrayList<>();
        /** Category of each device. */
        public final List<EnergyNetworkSnapshot.Category> categories = new ArrayList<>();
        /**
         * Sub-network a device was reached from (a buffer bordering two belongs to the first).
         * The value is -1 for an origin device that touches no sub-network.
         */
        public final List<Integer> deviceSubnet = new ArrayList<>();
        /** All sub-networks, by index. */
        public final List<Subnet> subnets = new ArrayList<>();
        /** Sub-networks touching the origin: the segment a probe or terminal sits on. */
        public final IntOpenHashSet originSubnets = new IntOpenHashSet();
        /** Total number of conductors in all sub-networks. */
        public int conductors;
        /** True if a conductor or device limit stopped part of the walk. */
        public boolean truncated;
        /**
         * 0.1.10.22 (G7): links of the single-line diagram: {device index, sub-network} for each buffer or protective
         * cable that a sub-network other than its own touches (a transformer between two voltages, an open breaker).
         */
        public final List<int[]> links = new ArrayList<>();
        /** 0.1.10.22 (G7): the bounding box of every conductor and device of the walk (min x, y, z, max x, y, z). */
        public int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE,
            maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        /** Device index by position, for the links. */
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap indexOf = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();

        void include(BlockPos at) {
            minX = Math.min(minX, at.getX()); minY = Math.min(minY, at.getY()); minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX()); maxY = Math.max(maxY, at.getY()); maxZ = Math.max(maxZ, at.getZ());
        }

        /** True when {@code at} lies in the bounding box grown by {@code margin}. */
        public boolean near(BlockPos at, int margin) {
            return minX <= maxX && at.getX() >= minX - margin && at.getX() <= maxX + margin && at.getY() >= minY - margin
                && at.getY() <= maxY + margin && at.getZ() >= minZ - margin && at.getZ() <= maxZ + margin;
        }
        /** {@link TopologyClock} stamp and game time at which this result was made. Used by the cache. */
        long stamp, madeAt;

        /** Returns true if the device at {@code deviceIndex} is in a sub-network that touches the origin. */
        public boolean inOriginSegment(int deviceIndex) {
            return deviceIndex < deviceSubnet.size() && originSubnets.contains((int) deviceSubnet.get(deviceIndex));
        }
    }

    /**
     * Returns true if the walk floods through this block entity. A special cable counts only while it
     * conducts.
     */
    public static boolean isConductor(@Nullable BlockEntity be) {
        if (be instanceof ICableEnergyNode) return true;
        return be instanceof IndependentSpecialCableBlockEntity special && special.conducts();
    }

    /** Returns true for a legacy transformer or an engine transformer. */
    public static boolean isTransformer(@Nullable BlockEntity be) {
        return be instanceof mio_icif_transformer || be instanceof IndependentTransformerBlockEntity;
    }

    /** Transformers and storage boxes: they buffer energy and start a new voltage segment. */
    public static boolean isBuffer(@Nullable BlockEntity be) {
        return isTransformer(be) || be instanceof AbstractEnergyStorageBlockEntity
            || be instanceof com.miophas.singularity_iteration.core.api.machine.IStorageMember
            || isProtective(be) && !((IndependentSpecialCableBlockEntity) be).conducts();   // 0.1.10.22: an open breaker or fuse
    }

    /** 0.1.10.22 (G5 / G7): a breaker or fuse cable. */
    public static boolean isProtective(@Nullable BlockEntity be) {
        return be instanceof IndependentSpecialCableBlockEntity special && special.protective();
    }

    /**
     * The block entity whose buffer a storage device reports: itself, or the multiblock core of a
     * storage port (GESU). Null for an unformed port. It then has no store of its own to show.
     */
    @Nullable
    public static BlockEntity storageOwner(@Nullable BlockEntity be) {
        if (be instanceof com.miophas.singularity_iteration.core.api.machine.IStorageMember member) return member.storageOwner();
        return be;
    }

    /**
     * Endpoint category of a block entity on a cable, or null when it is no EU endpoint.
     *
     * <p>The checks run in this order, and the first match wins: terminal (null), transformer, storage,
     * demand source (generator), energy machine (generator or consumer by
     * {@code isPowerSource()}), component host (consumer). A cable is never a machine here.
     */
    @Nullable
    public static EnergyNetworkSnapshot.Category categorize(@Nullable BlockEntity be) {
        if (be == null || be instanceof EnergyTerminalBlockEntity) return null;
        if (isProtective(be)) return EnergyNetworkSnapshot.Category.PROTECTION;   // 0.1.10.22
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

    /**
     * Returns the rated packet of a wire in EU/t, or 0 if the block entity is not a wire or the tier is
     * unknown. A failure in the tier lookup also gives 0, so that a display walk never throws.
     */
    private static long rating(BlockEntity be) {
        try {
            if (be instanceof AbstractWireBlockEntity wire && wire.getCableTier() != null) return wire.getCableTier().getMaxTransfer();
        } catch (RuntimeException ignored) { }
        return 0;
    }

    // ------------------------------------------------------------------ cache
    /**
     * Maximum age of a cached walk, in game ticks. After this time the walk runs again, also without a
     * topology clock change. Chunk loads and block entity swaps do not always advance the clock.
     */
    private static final long MAX_AGE_TICKS = 200;
    /** Cache key. The limits are part of the key, because they change the result. */
    private record Key(long origin, Scope scope, int maxConductors, int maxDevices) { }
    /** Per-level LRU caches. Weak keys release the cache of an unloaded level. Guarded by its own lock. */
    private static final Map<Level, Map<Key, Result>> CACHE = new WeakHashMap<>();
    /** Maximum number of cached results per level. */
    private static final int CACHE_LIMIT = 256;

    /** Compatibility form: {@code global} = {@link Scope#SYSTEM}, otherwise {@link Scope#SEGMENT}. */
    public static Result walk(ServerLevel level, BlockPos origin, boolean global, int maxConductors, int maxDevices) {
        return walk(level, origin, global ? Scope.SYSTEM : Scope.SEGMENT, maxConductors, maxDevices);
    }

    /**
     * Cached walk. See {@link #walkUncached}.
     *
     * <p>A cached result is used when its clock stamp is the current stamp of the level and it is
     * younger than {@link #MAX_AGE_TICKS}. The walk itself runs outside the cache lock. Thus two
     * threads can walk the same key at the same time. The last result wins. The returned result can
     * be shared. Do not change it.
     */
    public static Result walk(ServerLevel level, BlockPos origin, Scope scope, int maxConductors, int maxDevices) {
        long stamp = TopologyClock.stamp(level), now = level.getGameTime();
        Key key = new Key(origin.asLong(), scope, maxConductors, maxDevices);
        Map<Key, Result> perLevel;
        synchronized (CACHE) {
            // Access-order LinkedHashMap: the least recently used entry goes first when the size passes CACHE_LIMIT.
            perLevel = CACHE.computeIfAbsent(level, l -> new java.util.LinkedHashMap<>(16, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Result> eldest) { return size() > CACHE_LIMIT; }
            });
            Result hit = perLevel.get(key);
            // now >= madeAt rejects an entry with a game time after now. This protects against a game time that goes back.
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
     * origin that is itself a cable starts the flood. Otherwise the cables touching it are flooded.
     * A buffer origin is the boundary of every segment it touches, so all of them are "origin".
     *
     * <p>Steps:
     * <ol>
     *   <li>If the origin is a conductor, flood from it. The result is the only origin sub-network.</li>
     *   <li>Otherwise list the origin as a device (if it has a category), then look at its six
     *       neighbours. Each conductor neighbour starts a flood and an origin sub-network. In
     *       {@code SYSTEM} scope, a buffer origin also queues each neighbour buffer. A terminal or a
     *       buffer origin also lists each neighbour machine in its own origin sub-network.</li>
     *   <li>In {@code SYSTEM} scope, expand every queued buffer once. Each conductor neighbour starts
     *       a flood. Each buffer neighbour is listed and queued. Each machine neighbour is listed in a
     *       new zero-conductor sub-network.</li>
     * </ol>
     *
     * @param maxConductors the conductor limit, see the class comment
     * @param maxDevices the maximum length of {@link Result#devices}
     * @return a new result that the caller owns
     */
    public static Result walkUncached(ServerLevel level, BlockPos origin, Scope scope, int maxConductors, int maxDevices) {
        Result r = new Result();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet devices = new LongOpenHashSet();
        LongOpenHashSet expanded = new LongOpenHashSet();
        ArrayDeque<BlockPos> buffers = new ArrayDeque<>();
        boolean system = scope == Scope.SYSTEM;
        // The origin is never entered again from a neighbour.
        visited.add(origin.asLong());
        BlockEntity originBe = level.isLoaded(origin) ? level.getBlockEntity(origin) : null;
        if (isConductor(originBe)) {
            r.originSubnets.add(flood(level, origin, r, visited, devices, buffers, maxConductors, maxDevices));
        } else {
            EnergyNetworkSnapshot.Category originCat = categorize(originBe);
            boolean originIsBuffer = isBuffer(originBe);
            // The origin device is listed first, so it has device index 0. Its sub-network is set below.
            if (originCat != null) addDevice(r, devices, origin, originCat, -1, maxDevices);
            // Mark a buffer origin as expanded. Its neighbours are handled in this loop.
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
            // (IntOpenHashSet iteration order is hash order, not insertion order)
            if (originCat != null && !r.deviceSubnet.isEmpty() && !r.originSubnets.isEmpty())
                r.deviceSubnet.set(0, r.originSubnets.iterator().nextInt());
        }
        if (system) {
            while (!buffers.isEmpty()) {
                BlockPos b = buffers.poll();
                // A buffer can be queued from several sides. Expand it only once.
                if (!expanded.add(b.asLong())) continue;
                for (Direction side : Direction.values()) {
                    BlockPos next = b.relative(side);
                    if (visited.contains(next.asLong()) || !level.isLoaded(next)) continue;
                    BlockEntity be = level.getBlockEntity(next);
                    if (isConductor(be)) {
                        int flooded = flood(level, next, r, visited, devices, buffers, maxConductors, maxDevices);
                        touch(r, b, flooded);   // 0.1.10.22: the buffer links its own sub-network and this one
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
        // The buffer is queued also when the device list is full.
        buffers.add(at.immutable());
    }

    /**
     * Lists one device. A position is listed at most once. When the list already holds
     * {@code maxDevices} entries, the device is marked as seen but not listed, and the result is
     * marked as truncated.
     */
    private static void addDevice(Result r, LongOpenHashSet devices, BlockPos at, EnergyNetworkSnapshot.Category cat, int subnet, int maxDevices) {
        // The position goes into the seen set before the limit check, so a dropped device is not tried again.
        if (cat == null || !devices.add(at.asLong())) return;
        if (r.devices.size() >= maxDevices) { r.truncated = true; return; }
        r.indexOf.put(at.asLong(), r.devices.size());
        r.devices.add(at.immutable());
        r.categories.add(cat);
        r.deviceSubnet.add(subnet);
        r.include(at);
    }

    /** 0.1.10.22 (G7): sub-network {@code subnet} touches the listed buffer or protective cable at {@code at}. */
    private static void touch(Result r, BlockPos at, int subnet) {
        if (subnet < 0 || !r.indexOf.containsKey(at.asLong())) return;
        int index = r.indexOf.get(at.asLong());
        if (r.deviceSubnet.get(index) == subnet) return;
        for (int[] link : r.links) if (link[0] == index && link[1] == subnet) return;
        if (r.links.size() < 512) r.links.add(new int[] {index, subnet});
    }

    /**
     * Floods one segment of cables. Returns its sub-network index. Buffers found are queued.
     *
     * <p>This is a breadth-first search from {@code start}. It adds a new {@link Subnet}. It follows
     * only conductor-to-conductor links. It does not pass through a device. Each device neighbour of
     * the flood is listed under this sub-network. Each buffer neighbour is added to {@code buffers},
     * also when it is already listed, so that {@code SYSTEM} scope can expand it from this side.
     *
     * <p>Precondition: {@code start} is a loaded conductor and is not visited by an earlier flood.
     * The start conductor is always counted, also when the conductor limit is already reached.
     */
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
        r.include(start);
        // 0.1.10.22: a closed breaker or fuse is a conductor and also a device of its sub-network
        if (isProtective(startBe)) addDevice(r, devices, start, EnergyNetworkSnapshot.Category.PROTECTION, subnet.index, maxDevices);
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            BlockEntity from = level.getBlockEntity(at);
            for (Direction side : Direction.values()) {
                // The current cable blocks this face: no link to any neighbour on this side.
                if (from instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side)) continue;
                BlockPos next = at.relative(side);
                long key = next.asLong();
                // Never load a chunk. A neighbour in an unloaded chunk is skipped.
                if (visited.contains(key) || !level.isLoaded(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be == null) continue;
                if (isConductor(be)) {
                    // Both cables must accept the shared face, and the colours must match.
                    if (be instanceof ICableEnergyNode cable && cable.isDirectionBlocked(side.getOpposite())) continue;
                    if ((from instanceof IColoredEnergyTile || be instanceof IColoredEnergyTile) && !IColoredEnergyTile.connects(from, be, side)) continue;
                    // The conductor stays unvisited, so a later flood can still reach it.
                    if (r.conductors >= maxConductors) { r.truncated = true; continue; }
                    visited.add(key);
                    subnet.conductors++; r.conductors++;
                    subnet.ratedPacket = Math.max(subnet.ratedPacket, rating(be));
                    r.include(next);
                    if (isProtective(be)) addDevice(r, devices, next, EnergyNetworkSnapshot.Category.PROTECTION, subnet.index, maxDevices);
                    queue.add(next.immutable());
                    continue;
                }
                // a device already listed (e.g. a transformer bordering two segments) is not re-added,
                // but it must still be queued from this side - the old walker skipped it here
                EnergyNetworkSnapshot.Category cat = categorize(be);
                if (cat == null) continue;
                addDevice(r, devices, next, cat, subnet.index, maxDevices);
                if (isBuffer(be)) { buffers.add(next.immutable()); touch(r, next, subnet.index); }
            }
        }
        return subnet.index;
    }

    // ------------------------------------------------------------------ measurement
    /**
     * One tick of energy through the probed segment, plus the whole system's make-up.
     *
     * @param throughput EU that passed through the origin segment in the current tick
     * @param voltage the largest packet seen on the origin segment in the current tick, in EU
     * @param ratedPacket the highest cable rating in the origin sub-networks, in EU/t per packet
     */
    public record Reading(double throughput, double voltage, long ratedPacket,
                          int generators, int consumers, int storages, int transformers) { }

    /**
     * Energy through the origin segment this tick: what its devices took in (consumers, the
     * storage box or transformer downstream). When nothing downstream reports, it is what the
     * devices feeding it put out. Voltage is the largest packet seen on the segment.
     *
     * <p>The device counts cover the whole result, not only the origin segment. A GESU core and its
     * ports count as one storage. Only loaded positions are read. Call on the server thread.
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
                    // (an unformed port has no owner and counts by its own position)
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
        // Defensive bounds check. All indices come from this result, so the check normally passes.
        for (int s : r.originSubnets) if (s >= 0 && s < r.subnets.size()) rated = Math.max(rated, r.subnets.get(s).ratedPacket);
        return new Reading(in > 0 ? in : out, voltage, rated, gen, use, store, xfmr);
    }

    /**
     * IC2 voltage tier names by packet size (8 EU ULV ... ). Returns "-" for a packet of 0 or less.
     * A tier above the table returns "MAX".
     */
    public static String tierName(double packet) {
        String[] names = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UMV", "UXV", "MAX"};
        if (packet <= 0) return "-";
        int tier = EnergyNetGlobal.getTierFromPower(packet);
        return names[Math.min(names.length - 1, tier)];
    }
}
