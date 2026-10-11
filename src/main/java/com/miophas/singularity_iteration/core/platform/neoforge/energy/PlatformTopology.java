// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Keeps the conductor index of the energy network up to date from public NeoForge world events.
 *
 * <h2>What the topology is</h2>
 * <p>The topology has one {@link ConductorRegistry} for each {@link ServerLevel}. The registry is the
 * conductor index.
 * <ul>
 *   <li><b>Node.</b> A node is one block position whose block ID is a key of the {@code losses} map.
 *       The node stores the loss of that block ID and a mask of open faces. The loss unit is
 *       milli-units of energy per conductor ("lossMilli" in {@link ConductorRegistry}). The face mask
 *       comes from {@link Observer#conductorFaces}. Bit {@code n} of the mask is face {@code n} in
 *       {@link Direction} order. {@link ConductorRegistry#ALL_FACES} means that all six faces are open.</li>
 *   <li><b>Edge.</b> This class does not store edges. {@link ConductorRegistry#snapshot()} derives an
 *       edge between two nodes that are direct neighbours on one axis and that both have the facing
 *       face open.</li>
 *   <li><b>Component.</b> The snapshot also computes connected components and routes
 *       ({@code ConductorGraph}). This class does not search the graph.</li>
 * </ul>
 * <p>A block joins the conductor index only when its block ID is a key of {@code losses}. All other
 * sampled positions, for example machines, storage blocks and transformers, go only to the
 * {@link Observer}. The observer (the engine, {@code IndependentSiEnergy}) keeps its own index of
 * these endpoints. This class never reads or writes an energy balance. It never calls SI or IC2
 * energy classes, and it never transfers energy.
 *
 * <h2>How the topology is discovered and updated</h2>
 * <p>Event handlers do not change the index directly. They put a {@link Change} into the queue
 * {@link #pending}. The tick handlers {@link #onLevelTick} and {@link #onTick} drain the queue in
 * {@link #drain}. The events are:
 * <ul>
 *   <li>{@link ChunkEvent.Load}, and a ticket change that makes a chunk accessible: a
 *       {@code LOAD_CHUNK} change. It removes the old index entries of the chunk. Then it samples
 *       every block entity position of the chunk.</li>
 *   <li>{@link ChunkEvent.Unload}, and a ticket change that makes a chunk inaccessible: an
 *       {@code UNLOAD_CHUNK} change. It removes all index entries of the chunk.</li>
 *   <li>{@link BlockEvent.NeighborNotifyEvent}, and {@link #changed}: a {@code SAMPLE} change for one
 *       position. This is the incremental update.</li>
 *   <li>{@link LevelEvent.Unload}: an {@code UNLOAD_LEVEL} change. It closes the registry of the
 *       level.</li>
 *   <li>{@link #physicalBlockChanged}: no queue entry. It only calls {@link Observer#blockChanged}.</li>
 * </ul>
 * <p>A full rebuild of one level ("recovery", {@link #beginRecovery}) occurs in these cases. The queue
 * overflows and compaction cannot make it small enough. A level-scope change fails. A caller calls
 * {@link #rebuild}. A rebuild closes the registry of the level and replays one {@code LOAD_CHUNK}
 * change for every chunk in {@link #knownChunks}. A rebuild never deletes an energy balance.
 *
 * <h2>Scan algorithm and limits</h2>
 * <p>There is no graph search here. The work is a linear scan of the block entity positions of a
 * chunk, plus single-position samples. These limits apply:
 * <ul>
 *   <li>{@code workPerTick}: the work units for one server tick. One dequeued change costs one unit.
 *       Each further sampled position of a chunk scan costs one unit. A scan that runs out of units
 *       continues in a later tick as a {@code SCAN_CHUNK} change. This limit prevents a large chunk
 *       or a burst of chunk loads from stopping the server tick.</li>
 *   <li>{@code maximumQueued}: the maximum number of queue entries. On overflow, the queue is
 *       compacted to one entry for each chunk. If that is still too many, the affected levels are
 *       rebuilt. This limit keeps memory bounded during a flood of block updates.</li>
 *   <li>{@code maximumNodes}: the capacity of each registry. A put beyond the capacity throws, and the
 *       chunk goes into quarantine.</li>
 *   <li>{@code maximumSources}: the number of cached route sources in each registry snapshot. It does
 *       not limit the number of generators.</li>
 * </ul>
 * <p>The queue key ({@link Key}) coalesces changes. A new change for the same key replaces the queued
 * change and keeps its place in the queue order.
 *
 * <h2>Chunk-loading rules</h2>
 * <p>This class never loads a chunk and never adds a chunk ticket. It reads chunks only through
 * {@code getChunkNow}. If the chunk is not available, a chunk change becomes a {@code DEFERRED_LOAD}
 * change and is tried again later. A position change for a missing chunk is dropped. Before a change
 * waits for a missing chunk, the old index entries of that chunk are removed. Thus an unavailable
 * chunk never keeps old routes. A {@link ChunkScan} holds only a weak reference to its chunk between
 * ticks.
 *
 * <h2>What observers receive</h2>
 * <p>See {@link Observer}. All callbacks occur on the server thread. A callback that receives a
 * {@link LevelChunk} receives a chunk that is already loaded.
 *
 * <h2>Threading</h2>
 * <p>All index changes occur on the server thread. A {@link ConductorRegistry} is confined to the
 * thread that made it. The {@code synchronized} methods protect the queue counters against reentrant
 * observer callbacks and let other threads read diagnostic values. They do not make the index safe
 * for use from other threads.
 *
 * <h2>Invariants that the engine relies on</h2>
 * <ul>
 *   <li>{@link #chunkReady} is false while the chunk has a queued or running change that is not
 *       {@code DEFERRED_LOAD}, or while the chunk is in quarantine.</li>
 *   <li>{@link #ready} is false while any change of the level, other than {@code DEFERRED_LOAD}, is
 *       queued or running, while the level is in recovery, and while any chunk of the level is in
 *       quarantine.</li>
 *   <li>The index never keeps conductors of a chunk that this class saw unload, become inaccessible,
 *       or go missing.</li>
 *   <li>{@link #readinessRevision()} increases each time the queue or the readiness state changes.
 *       A caller that caches a readiness result must compare the revision.</li>
 *   <li>A snapshot is only a lease. The energy adapter must call {@link #isCurrent} again directly
 *       before it commits energy.</li>
 *   <li>{@link #pendingByLevel}, {@link #blockingByLevel} and {@link #pendingChunks} always agree with
 *       the content of {@link #pending}. Each path that adds or removes a queue entry updates them.</li>
 * </ul>
 */
public final class PlatformTopology implements AutoCloseable {
    /**
     * Receives index events from the topology.
     *
     * <p>All callbacks occur on the server thread. A callback must not change the world. A callback
     * must not load a chunk. A callback can call back into the topology, for example
     * {@link #changed} or {@link #close}. The topology accepts this reentry. A callback that throws a
     * {@link RuntimeException} puts the related chunk into quarantine, or starts a rebuild of the level.
     */
    public interface Observer {
        /**
         * Called once for each sampled position, after the conductor index entry for that position is
         * updated. The position can be a conductor, an endpoint, or an empty position. {@code chunk} is
         * loaded and contains {@code at}.
         */
        default void position(ServerLevel level, LevelChunk chunk, BlockPos at) { }
        /**
         * Returns the open-face mask of a conductor at {@code at}. Called only for an already loaded
         * conductor chunk, on the server thread. The result must be in the range
         * {@code 0..ConductorRegistry.ALL_FACES}.
         */
        default int conductorFaces(ServerLevel level, LevelChunk chunk, BlockPos at) { return ConductorRegistry.ALL_FACES; }
        /**
         * Called directly from a chunk block-state write, before any queued sample. Only observer
         * metadata may change here. The index does not change.
         */
        default void blockChanged(ServerLevel level, BlockPos at, BlockState before, BlockState after) { }
        /** Called after the index entries of one chunk are removed. Chunk coordinates are chunk units, not blocks. */
        default void chunkRemoved(ServerLevel level, int chunkX, int chunkZ) { }
        /** Called after the registry of a level is closed, at level unload or at the start of a rebuild. */
        default void levelRemoved(ServerLevel level) { }
        /** Called after all registries are closed, at {@link #close}. */
        default void cleared() { }
    }
    private static final Observer NO_OBSERVER = new Observer() { };
    /** All open topologies, by server. Guarded by the {@code PlatformTopology.class} lock. */
    private static final Map<MinecraftServer, ArrayList<PlatformTopology>> INSTANCES = new java.util.IdentityHashMap<>();

    /**
     * Forwards one chunk block-state write to the observer of every open topology of the server.
     *
     * <p>A chunk mixin calls this method for each block-state write. Only observer metadata may change
     * here. This method does not queue a sample. A neighbour event or an explicit {@link #changed}
     * call updates the index. If an observer throws, the chunk of {@code at} goes into quarantine for
     * that topology only.
     *
     * @throws IllegalStateException if the caller is not on the server thread
     */
    public static synchronized void physicalBlockChanged(ServerLevel level, BlockPos at, BlockState before, BlockState after) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block history outside server thread");
        var instances = INSTANCES.get(level.getServer());
        if (instances == null) return;
        // Observer callbacks may close or create another topology. Iterate over
        // a stable snapshot so that a reentrant lifecycle callback cannot mutate
        // the server's instance list and throw ConcurrentModificationException.
        for (var instance : instances.toArray(PlatformTopology[]::new)) {
            if (instance.closed || !instance.failure.isEmpty()) continue;
            try { instance.observer.blockChanged(level, at, before, after); }
            catch (RuntimeException error) {
                synchronized (instance) {
                    instance.quarantine(new Key(level, Scope.CHUNK, ChunkPos.asLong(at)), error);
                }
            }
        }
    }
    /**
     * Type of a queued change.
     * <ul>
     *   <li>{@code SAMPLE}: read one position again. The change is dropped if its chunk is missing.</li>
     *   <li>{@code LOAD_CHUNK}: remove the index entries of a chunk, then scan all its block entity
     *       positions.</li>
     *   <li>{@code DEFERRED_LOAD}: a chunk change whose chunk was missing. It is tried again. It does not
     *       block {@link #ready} or {@link #chunkReady}.</li>
     *   <li>{@code SCAN_CHUNK}: the rest of a chunk scan that ran out of work units.</li>
     *   <li>{@code UNLOAD_CHUNK}: remove the index entries of a chunk.</li>
     *   <li>{@code UNLOAD_LEVEL}: close the registry of a level.</li>
     * </ul>
     */
    private enum Kind { SAMPLE, LOAD_CHUNK, DEFERRED_LOAD, SCAN_CHUNK, UNLOAD_CHUNK, UNLOAD_LEVEL }
    /** Area of a change. It selects the meaning of {@link Key#coordinate}. */
    private enum Scope { POSITION, CHUNK, LEVEL }
    /**
     * Queue and quarantine key. One key holds at most one queued change.
     * {@code coordinate} is {@link BlockPos#asLong} for {@code POSITION}, {@link ChunkPos#asLong} for
     * {@code CHUNK}, and 0 for {@code LEVEL}.
     */
    private record Key(ServerLevel level, Scope scope, long coordinate) { }
    /**
     * One queued change. {@code position} is the sampled position for {@code POSITION} scope and
     * {@link BlockPos#ZERO} otherwise. {@code scan} is set only for {@code SCAN_CHUNK}.
     */
    private record Change(Key key, Kind kind, BlockPos position, ChunkScan scan) {
        Change(Key key, Kind kind, BlockPos position) { this(key, kind, position, null); }
    }
    /**
     * State of a chunk scan that can continue across ticks.
     *
     * <p>The scan holds the chunk through a {@link WeakReference}. Thus a queued scan does not keep an
     * unloaded chunk in memory. When the scan continues, {@link #apply} compares the reference with the
     * current chunk object. If the chunk object changed, the scan starts again from the beginning.
     */
    private static final class ChunkScan {
        final WeakReference<LevelChunk> source;
        /**
         * Encoded positions keep the scan snapshot compact and detached from
         * the chunk's mutable block-entity map. A copy of every position as a
         * fresh BlockPos doubles the allocation rate for large machine chunks
         * and leaves a large temporary object graph for the GC to reclaim.
         * A packed long is the native representation used by BlockPos. Only the
         * position that the tick budget currently consumes becomes a BlockPos.
         */
        final long[] positions;
        /** Index of the next position to sample. The range is {@code 0..positions.length}. */
        int next;
        ChunkScan(LevelChunk chunk) {
            source = new WeakReference<>(chunk);
            // The public position set and this immutable snapshot still cost
            // O(B) once, where B is the number of block entities. Only per-BE
            // sampling and materialization use the tick budget. No live iterator
            // and no strong chunk reference spans ticks.
            positions = chunk.getBlockEntitiesPos().stream().mapToLong(BlockPos::asLong).toArray();
        }
    }
    /** Diagnostic counters. The values are a snapshot and can be old when the caller reads them. */
    public record Metrics(int dimensions, int queued, long chunkLoads, long chunkUnloads,
                          long blockSignals, long sampledPositions, long deferredLoads, long accessibilityChanges,
                          boolean closed, String failure) { }

    /** Conductor loss by block ID, in milli-units per conductor. Immutable. All values are 0 or more. */
    private final Map<ResourceLocation, Long> losses;
    private Observer observer;
    /** The owning server. It is null after {@link #close}. */
    private MinecraftServer server;
    private final int maximumNodes;
    private final int maximumSources;
    private final int maximumQueued;
    private final int workPerTick;
    /** Conductor index for each level. A registry is made on the first conductor or snapshot. */
    private final Map<ServerLevel, ConductorRegistry> worlds = new HashMap<>();
    /** The change queue, in the order of first insertion of each key. The size is at most {@code maximumQueued}. */
    private final LinkedHashMap<Key, Change> pending = new LinkedHashMap<>();
    // Readiness is queried at every transaction boundary. Keep the level count
    // beside the ordered queue instead of scanning other dimensions each time.
    private final Map<ServerLevel, Integer> pendingByLevel = new java.util.IdentityHashMap<>();
    // Count of queued changes per level that block readiness. A DEFERRED_LOAD
    // is not counted. Only a load already observed missing, with its old indexes
    // revoked, may wait without blocking unrelated loaded networks in the same dimension.
    private final Map<ServerLevel, Integer> blockingByLevel = new java.util.IdentityHashMap<>();
    // Count of dequeued changes per level that drain has not finished yet.
    private final Map<ServerLevel, Integer> processingByLevel = new java.util.IdentityHashMap<>();
    // Blocking counts per chunk key: queued changes, and dequeued changes that are not finished.
    private final Map<Key, Integer> pendingChunks = new HashMap<>(), processingChunks = new HashMap<>();
    // Chunks that this topology saw load and not yet unload, per level. A rebuild replays them.
    private final Map<ServerLevel, java.util.LinkedHashSet<Long>> knownChunks = new java.util.IdentityHashMap<>();
    // Levels in rebuild, with the cursor over the chunks to replay.
    private final Map<ServerLevel, Recovery> recovering = new java.util.IdentityHashMap<>();
    // Chunk key to the server tick of the next retry. A chunk is here after a callback or sample failed.
    private final Map<Key, Long> quarantined = new HashMap<>();
    private long recoveries, isolatedFailures, readinessRevision;
    /**
     * Returns a counter that increases each time the queue or the readiness state can change.
     * A caller can cache {@link #ready} or {@link #chunkReady} results while this value stays the same.
     */
    public synchronized long readinessRevision() { return readinessRevision; }
    private String lastRecovery = "";
    /** Replay cursor of one level rebuild. {@code chunks} is a copy of the known chunks at rebuild start. */
    private static final class Recovery {
        final long[] chunks; int next;
        Recovery(java.util.Set<Long> chunks) { this.chunks = chunks.stream().mapToLong(Long::longValue).toArray(); }
    }
    /** Diagnostic counters of rebuilds and quarantine. */
    public record RecoveryMetrics(long rebuilds, long isolatedFailures, int recoveringLevels, int quarantinedChunks, String lastReason) { }
    public synchronized RecoveryMetrics recoveryMetrics() {
        return new RecoveryMetrics(recoveries, isolatedFailures, recovering.size(), quarantined.size(), lastRecovery);
    }
    /**
     * Returns the chunk key of a change. A position change maps to the chunk that contains the position.
     * Do not call this for a {@code LEVEL} scope change.
     */
    private static Key chunkKey(Change change) {
        return new Key(change.key.level, Scope.CHUNK,
            change.key.scope == Scope.CHUNK ? change.key.coordinate : ChunkPos.asLong(change.position));
    }
    /**
     * Puts one chunk into quarantine and removes its index entries.
     *
     * <p>A chunk in quarantine is not {@link #chunkReady}, and its level is not {@link #ready}. Thus a
     * failure in one chunk does not stop the other chunks of the level. {@link #pumpRecovery} queues a
     * new {@code LOAD_CHUNK} for the chunk 20 ticks later, and again every 20 ticks while it fails.
     * A successful chunk change removes the quarantine entry.
     *
     * @param key a {@code CHUNK} scope key
     */
    private synchronized void quarantine(Key key, RuntimeException error) {
        // An observer callback can close this topology before it throws. close() sets server to
        // null, so a quarantine after close would throw a NullPointerException (0.1.7.36).
        if (closed || server == null) return;
        readinessRevision++;
        // Retry time: 20 server ticks (1 second) from now. Log only the first failure of a chunk.
        if (quarantined.put(key, (long) server.getTickCount() + 20) == null) {
            isolatedFailures++;
            com.mojang.logging.LogUtils.getLogger().warn("Energy topology isolated chunk {} in {}: {}",
                key.coordinate, key.level.dimension().location(), error.toString());
        }
        lastRecovery = error.toString();
        try { removeChunk(key.level, ChunkPos.getX(key.coordinate), ChunkPos.getZ(key.coordinate)); }
        catch (RuntimeException removalFailure) { lastRecovery = removalFailure.toString(); }
    }
    /**
     * Starts a full rebuild of each given level.
     *
     * <p>For each level, this method closes the registry, tells the observer, and forgets the quarantine
     * entries of the level. It records the known chunks for replay. {@link #pumpRecovery} then queues
     * the replay in steps that fit into the queue. The level is not {@link #operational} until the
     * replay is complete.
     *
     * <p>Precondition: the caller holds the lock of this object and has removed the queued changes of
     * these levels.
     */
    private void beginRecovery(java.util.Set<ServerLevel> levels) {
        readinessRevision++;
        for (var level : levels) {
            // Drop indexes, never block-entity balances. Rebuild from public loaded-chunk observations.
            recovering.put(level, new Recovery(knownChunks.getOrDefault(level, new java.util.LinkedHashSet<>())));
            var previous = worlds.remove(level); if (previous != null) previous.close();
            observer.levelRemoved(level);
            quarantined.keySet().removeIf(key -> key.level == level);
            recoveries++;
        }
        lastRecovery = "Topology queue coalesced into bounded level rebuild";
    }
    /**
     * Moves rebuild and quarantine work into the queue, as far as the queue has free space.
     *
     * <p>This method never fills the queue above {@code maximumQueued}. Thus it never triggers a
     * compaction or a new rebuild. A level leaves recovery when all its chunks are replayed and no
     * blocking change of that level is queued or running.
     */
    private void pumpRecovery() {
        for (var entry : recovering.entrySet()) {
            var scan = entry.getValue();
            while (pending.size() < maximumQueued && scan.next < scan.chunks.length) {
                long chunk = scan.chunks[scan.next++];
                // A chunk may unload after the rebuild snapshot was taken.
                // Replaying that old entry would resurrect an endless deferred load.
                var known = knownChunks.get(entry.getKey());
                if (known == null || !known.contains(chunk)) continue;
                enqueue(new Change(new Key(entry.getKey(), Scope.CHUNK, chunk), Kind.LOAD_CHUNK, BlockPos.ZERO), true);
            }
        }
        for (var entry : java.util.List.copyOf(quarantined.entrySet())) {
            if (pending.size() >= maximumQueued) break;
            if (entry.getValue() <= server.getTickCount()) {
                // Set the next retry time before the retry, so that a failed retry waits 20 ticks again.
                quarantined.put(entry.getKey(), (long) server.getTickCount() + 20);
                enqueue(new Change(entry.getKey(), Kind.LOAD_CHUNK, BlockPos.ZERO), true);
            }
        }
        // A level with only DEFERRED_LOAD work left leaves recovery here. Missing chunks must not hold the level.
        recovering.entrySet().removeIf(entry -> entry.getValue().next == entry.getValue().chunks.length
            && !blockingByLevel.containsKey(entry.getKey()) && !processingByLevel.containsKey(entry.getKey()));
    }

    /**
     * Puts the chunk of {@code position} into quarantine. The engine calls this when its own work on
     * that chunk fails. The call has no effect after {@link #close}, for another server, or off the
     * server thread.
     */
    public synchronized void isolateChunk(ServerLevel level, BlockPos position, RuntimeException error) {
        if (!closed && level.getServer() == server && server.isSameThread())
            quarantine(new Key(level, Scope.CHUNK, ChunkPos.asLong(position)), error);
    }
    /**
     * Drops all queued changes of {@code level} and starts a full rebuild of its index.
     * {@code reason} is kept for diagnostics. The call has no effect after {@link #close}, for another
     * server, or off the server thread.
     */
    public synchronized void rebuild(ServerLevel level, String reason) {
        if (closed || level.getServer() != server || !server.isSameThread()) return;
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            var change = iterator.next();
            if (change.key.level == level) { iterator.remove(); removedPending(change); }
        }
        beginRecovery(java.util.Set.of(level)); lastRecovery = reason;
    }
    private volatile boolean closed;
    /** A failure text. An empty text means no failure. A non-empty text stops all work. */
    private volatile String failure = "";
    private long chunkLoads, chunkUnloads, blockSignals, sampledPositions, deferredLoads, accessibilityChanges;
    private long queueCompactions;
    /** Server tick of the current work budget, and the work units left in that tick. */
    private int budgetTick = Integer.MIN_VALUE, remainingWork;

    /** Makes a topology without an observer. See the full constructor. */
    public PlatformTopology(MinecraftServer server, Map<ResourceLocation, Long> losses, int maximumNodes,
                            int maximumSources, int maximumQueued, int workPerTick) {
        this(server, losses, maximumNodes, maximumSources, maximumQueued, workPerTick, NO_OBSERVER);
    }

    /**
     * Makes a topology and registers it on the NeoForge event bus.
     *
     * <p>The topology starts empty. It finds conductors only through later chunk load events and
     * block events. It does not scan chunks that are already loaded. A new topology must exist before
     * the levels load, or the caller must call {@link #changed} or {@link #rebuild} for old chunks.
     *
     * @param losses conductor loss by block ID, in milli-units per conductor. It must not be empty.
     *               All values must be 0 or more.
     * @param maximumNodes registry capacity per level. Range {@code 1..(Integer.MAX_VALUE - 1) / 6}.
     *                     The upper bound is the same bound that {@link ConductorRegistry} checks.
     * @param maximumSources number of cached route sources per snapshot. Must be positive.
     * @param maximumQueued maximum number of queued changes. Must be positive.
     * @param workPerTick work units per server tick. Must be positive.
     * @throws IllegalStateException if the caller is not on the server thread
     * @throws IllegalArgumentException if a limit or a loss is out of range
     */
    public PlatformTopology(MinecraftServer server, Map<ResourceLocation, Long> losses, int maximumNodes,
                            int maximumSources, int maximumQueued, int workPerTick, Observer observer) {
        this.server = Objects.requireNonNull(server, "server");
        this.observer = Objects.requireNonNull(observer, "observer");
        if (!server.isSameThread()) { throw new IllegalStateException("Attach on the server thread"); }
        this.losses = Map.copyOf(losses);
        if (this.losses.isEmpty() || this.losses.values().stream().anyMatch(v -> v < 0)
                || maximumNodes <= 0 || maximumNodes > (Integer.MAX_VALUE - 1) / 6
                || maximumSources <= 0 || maximumQueued <= 0 || workPerTick <= 0) {
            throw new IllegalArgumentException("Positive limits and nonnegative conductor losses required");
        }
        this.maximumNodes = maximumNodes; this.maximumSources = maximumSources;
        this.maximumQueued = maximumQueued; this.workPerTick = workPerTick;
        synchronized (PlatformTopology.class) { INSTANCES.computeIfAbsent(server, ignored -> new ArrayList<>()).add(this); }
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * Adds one change to the queue, or replaces the queued change with the same key.
     *
     * <p>This method also updates {@link #knownChunks}, the queue counters and
     * {@link #readinessRevision}. If the queue is full and the key is new, the queue is compacted to
     * chunk changes. If the compacted queue is still larger than {@code maximumQueued}, all queued
     * work is dropped and the affected levels are rebuilt.
     *
     * @param retry true for a change that the topology makes itself (a retry, a scan continuation or a
     *              replay). Such a change does not replace a newer queued change with the same key.
     */
    private synchronized void enqueue(Change change, boolean retry) {
        if (closed || !failure.isEmpty()) { return; }
        // Keep the known-chunk set current before any early exit. A rebuild replays this set.
        if (change.kind == Kind.UNLOAD_LEVEL) knownChunks.remove(change.key.level);
        else if (change.kind == Kind.UNLOAD_CHUNK) {
            var known = knownChunks.get(change.key.level); if (known != null) known.remove(change.key.coordinate);
            quarantined.remove(change.key);
        } else if (change.kind == Kind.LOAD_CHUNK || change.kind == Kind.SAMPLE
                && change.key.level.getChunkSource().getChunkNow(change.position.getX() >> 4, change.position.getZ() >> 4) != null) {
            // A SAMPLE adds its chunk only if the chunk is loaded now. The && binds before the ||.
            knownChunks.computeIfAbsent(change.key.level, ignored -> new java.util.LinkedHashSet<>()).add(chunkKey(change).coordinate);
        }
        if (retry && pending.containsKey(change.key)) { return; }
        readinessRevision++;
        if (!pending.containsKey(change.key) && pending.size() >= maximumQueued) {
            // A flood of position edits is represented by bounded chunk rescans.
            // LOAD revokes the old chunk index before rebuilding it, including
            // deleted entities that no longer occur in getBlockEntitiesPos().
            var compacted = new LinkedHashMap<Key, Change>();
            for (var queued : pending.values()) compactChange(compacted, queued);
            compactChange(compacted, change);
            if (compacted.size() > maximumQueued) {
                // Too many distinct chunks. A level rebuild has a bounded queue cost.
                var levels = new java.util.HashSet<ServerLevel>();
                for (var key : compacted.keySet()) levels.add(key.level);
                clearPending(); beginRecovery(levels); queueCompactions++; return;
            }
            clearPending();
            // Rebuild all counters from the compacted queue. This keeps the counter invariant.
            for (var queued : compacted.values()) {
                pending.put(queued.key, queued);
                pendingByLevel.merge(queued.key.level, 1, Integer::sum);
                if (queued.kind != Kind.DEFERRED_LOAD) blockingByLevel.merge(queued.key.level, 1, Integer::sum);
                if (queued.kind != Kind.DEFERRED_LOAD && queued.key.scope != Scope.LEVEL)
                    pendingChunks.merge(chunkKey(queued), 1, Integer::sum);
            }
            queueCompactions++;
            return;
        }
        var previous = pending.put(change.key, change);
        // A replaced entry keeps its level count. Remove only its blocking counts, then add the new ones.
        if (previous == null) pendingByLevel.merge(change.key.level, 1, Integer::sum);
        else if (previous.kind != Kind.DEFERRED_LOAD) {
            decrement(blockingByLevel, previous.key.level);
            if (previous.key.scope != Scope.LEVEL) decrementKey(pendingChunks, chunkKey(previous));
        }
        if (change.kind != Kind.DEFERRED_LOAD) blockingByLevel.merge(change.key.level, 1, Integer::sum);
        if (change.kind != Kind.DEFERRED_LOAD && change.key.scope != Scope.LEVEL) pendingChunks.merge(chunkKey(change), 1, Integer::sum);
    }

    /**
     * Merges one change into a compacted queue with at most one entry for each chunk.
     *
     * <p>Rules: an {@code UNLOAD_LEVEL} replaces all entries of its level and absorbs later entries of
     * that level. An {@code UNLOAD_CHUNK} stays an unload. Every other change becomes a
     * {@code LOAD_CHUNK} of its chunk. A position change does not replace a queued unload of its chunk.
     * A compacted {@code DEFERRED_LOAD} or {@code SCAN_CHUNK} becomes a blocking {@code LOAD_CHUNK}.
     */
    private static void compactChange(LinkedHashMap<Key, Change> compacted, Change change) {
        var levelKey = new Key(change.key.level, Scope.LEVEL, 0);
        if (change.kind == Kind.UNLOAD_LEVEL) {
            compacted.keySet().removeIf(key -> key.level == change.key.level);
            compacted.put(levelKey, change);
            return;
        }
        if (compacted.containsKey(levelKey)) return;
        long chunk = change.key.scope == Scope.CHUNK ? change.key.coordinate : ChunkPos.asLong(change.position);
        var key = new Key(change.key.level, Scope.CHUNK, chunk);
        if (change.key.scope == Scope.POSITION) {
            var previous = compacted.get(key);
            // A later coordinate notification cannot undo an explicit unload.
            if (previous != null && previous.kind == Kind.UNLOAD_CHUNK) return;
            compacted.put(key, new Change(key, Kind.LOAD_CHUNK, BlockPos.ZERO));
        } else {
            compacted.put(key, change.kind == Kind.UNLOAD_CHUNK ? change
                : new Change(key, Kind.LOAD_CHUNK, BlockPos.ZERO));
        }
    }

    /** Returns the number of queue compactions and overflow rebuilds since construction. Diagnostic only. */
    public synchronized long queueCompactions() { return queueCompactions; }

    /** Decreases a per-level count and removes the entry at 0. The entry must exist. */
    private static void decrement(Map<ServerLevel, Integer> counts, ServerLevel level) {
        counts.compute(level, (key, count) -> count == 1 ? null : count - 1);
    }
    /** Decreases a per-key count and removes the entry at 0. A missing entry stays missing. */
    private static void decrementKey(Map<Key, Integer> counts, Key at) {
        counts.computeIfPresent(at, (key, count) -> count == 1 ? null : count - 1);
    }

    /** Updates the queue counters for a change that the caller removed from {@link #pending}. */
    private void removedPending(Change change) {
        decrement(pendingByLevel, change.key.level);
        if (change.kind != Kind.DEFERRED_LOAD) decrement(blockingByLevel, change.key.level);
        if (change.kind != Kind.DEFERRED_LOAD && change.key.scope != Scope.LEVEL) decrementKey(pendingChunks, chunkKey(change));
    }

    /** Empties the queue and its counters. It does not touch the processing counters. */
    private void clearPending() { pending.clear(); pendingByLevel.clear(); blockingByLevel.clear(); pendingChunks.clear(); }

    /**
     * Explicit hook for mutations which intentionally suppress neighbour events.
     *
     * <p>Queues a {@code SAMPLE} of one position. Call it after a change that can alter the conductor
     * status, the face mask or the endpoint data of a position, when no neighbour event fires. The
     * sample runs in a later drain. The call has no effect for a level of another server.
     */
    public void changed(ServerLevel level, BlockPos position) {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(position, "position");
        if (level.getServer() != server) { return; }
        BlockPos immutable = position.immutable();
        enqueue(new Change(new Key(level, Scope.POSITION, immutable.asLong()), Kind.SAMPLE, immutable), false);
    }

    /** Queues a sample of the changed position and of each notified neighbour position. */
    @SubscribeEvent
    public void onNeighbours(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { blockSignals++; }
        changed(level, event.getPos());
        for (Direction direction : event.getNotifiedSides()) { changed(level, event.getPos().relative(direction)); }
    }

    /** Queues a full scan of the loaded chunk. */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { chunkLoads++; }
        enqueue(new Change(new Key(level, Scope.CHUNK, event.getChunk().getPos().toLong()), Kind.LOAD_CHUNK, BlockPos.ZERO), false);
    }

    /** Cancels queued work for the chunk and queues removal of its index entries. */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { chunkUnloads++; }
        queueChunkRemoval(level, event.getChunk().getPos().toLong());
    }

    /**
     * Follows chunk accessibility. A chunk is accessible when its ticket level gives a full status other
     * than {@code INACCESSIBLE}. Only a change of accessibility queues work.
     */
    @SubscribeEvent
    public void onTicketLevel(ChunkTicketLevelUpdatedEvent event) {
        ServerLevel level = event.getLevel();
        if (level.getServer() != server) { return; }
        boolean wasFull = ChunkLevel.fullStatus(event.getOldTicketLevel()) != FullChunkStatus.INACCESSIBLE;
        boolean nowFull = ChunkLevel.fullStatus(event.getNewTicketLevel()) != FullChunkStatus.INACCESSIBLE;
        if (wasFull == nowFull) { return; }
        synchronized (this) { accessibilityChanges++; }
        // A chunk can become inaccessible long before its physical unload event.
        // Conversely, a still-resident chunk can regain access without a new Load.
        // Coalesce public ticket transitions. Do not retain a ChunkHolder or load a chunk here.
        if (nowFull) {
            enqueue(new Change(new Key(level, Scope.CHUNK, event.getChunkPos()), Kind.LOAD_CHUNK, BlockPos.ZERO), false);
        } else { queueChunkRemoval(level, event.getChunkPos()); }
    }

    /**
     * Removes every queued chunk change and position change of one chunk, then queues an
     * {@code UNLOAD_CHUNK}. {@code key} is a {@link ChunkPos#asLong} value.
     */
    private synchronized void queueChunkRemoval(ServerLevel level, long key) {
            // Unload cancels retrying loads and pending coordinate reads for this chunk.
            var iterator = pending.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().level == level
                        && (entry.getKey().scope == Scope.CHUNK ? entry.getKey().coordinate == key
                            : entry.getKey().scope == Scope.POSITION && ChunkPos.asLong(entry.getValue().position) == key)) {
                    iterator.remove(); removedPending(entry.getValue());
                }
            }
            enqueue(new Change(new Key(level, Scope.CHUNK, key), Kind.UNLOAD_CHUNK, BlockPos.ZERO), false);
    }

    /**
     * Drops all queued work and all rebuild and quarantine state of the level, then queues an
     * {@code UNLOAD_LEVEL}. The level registry closes in the next drain.
     */
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) {
            // The counters of this level are removed as a whole. removedPending is not needed here.
            pending.entrySet().removeIf(entry -> entry.getKey().level == level);
            pendingByLevel.remove(level);
            blockingByLevel.remove(level);
            pendingChunks.keySet().removeIf(key -> key.level == level);
            recovering.remove(level); knownChunks.remove(level); quarantined.keySet().removeIf(key -> key.level == level);
            enqueue(new Change(new Key(level, Scope.LEVEL, 0), Kind.UNLOAD_LEVEL, BlockPos.ZERO), false);
        }
    }

    /** Drains queued changes of all levels with the work left in this tick. */
    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        if (event.getServer() != server) { return; }
        drain(null);
    }

    /** Drains queued changes of one level after that level ticks. */
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getServer() == server) drain(level);
    }

    /**
     * Takes queued changes in queue order, up to the work left in this tick, and applies them.
     *
     * <p>All level ticks and the server tick of one server tick share one budget of
     * {@code workPerTick} units. Each dequeued change reserves one unit. The changes stay counted as
     * "processing" until the whole batch is applied. Thus {@link #ready} and {@link #chunkReady} stay
     * false while a batch is half applied. A failure in one change puts its chunk into quarantine, or
     * rebuilds its level for a level change. The other changes of the batch still run.
     *
     * @param onlyLevel the level to drain, or null for all levels
     */
    private void drain(ServerLevel onlyLevel) {
        ArrayList<Change> batch;
        synchronized (this) {
            if (closed) { return; }
            if (!failure.isEmpty()) { clearWorlds(); return; }
            // A drain is already running further up the stack (an observer callback reentered). Do not nest.
            if (!processingByLevel.isEmpty()) return;
            pumpRecovery();
            int now = server.getTickCount();
            // A new server tick refills the budget.
            if (now != budgetTick) { budgetTick = now; remainingWork = workPerTick; }
            if (remainingWork == 0 || pending.isEmpty()
                    || onlyLevel != null && !pendingByLevel.containsKey(onlyLevel)) return;
            batch = new ArrayList<>(Math.min(remainingWork,
                    onlyLevel == null ? pending.size() : pendingByLevel.get(onlyLevel)));
            var iterator = pending.values().iterator();
            while (iterator.hasNext() && remainingWork > 0) {
                Change change = iterator.next();
                if (change.key.level.getServer() == server && (onlyLevel == null || change.key.level == onlyLevel)) {
                    batch.add(change); iterator.remove(); removedPending(change); remainingWork--;
                    processingByLevel.merge(change.key.level, 1, Integer::sum);
                    if (change.key.scope != Scope.LEVEL) processingChunks.merge(chunkKey(change), 1, Integer::sum);
                }
            }
        }
        try {
            for (Change change : batch) {
                if (closed) break;
                try {
                    apply(change);
                    // The chunk is healthy only if apply finished it. A requeued scan or deferral keeps the quarantine.
                    if (change.key.scope == Scope.CHUNK && !pending.containsKey(change.key)) quarantined.remove(change.key);
                } catch (RuntimeException error) {
                    if (change.key.scope != Scope.LEVEL) quarantine(chunkKey(change), error);
                    else { lastRecovery = error.toString(); beginRecovery(java.util.Set.of(change.key.level)); }
                }
            }
        } finally {
            // Dequeuing is not publication: observer callbacks must not obtain
            // a current lease while any entry in this batch is still unapplied.
            synchronized (this) {
                for (var change : batch) {
                    decrement(processingByLevel, change.key.level);
                    if (change.key.scope != Scope.LEVEL) decrementKey(processingChunks, chunkKey(change));
                }
                recovering.entrySet().removeIf(entry -> entry.getValue().next == entry.getValue().chunks.length
                    && !pendingByLevel.containsKey(entry.getKey()) && !processingByLevel.containsKey(entry.getKey()));
                readinessRevision++;
            }
        }
    }

    /**
     * Removes the index entries of one chunk and tells the observer. Chunk coordinates are chunk units.
     * The observer is told also when the level has no registry.
     */
    private void removeChunk(ServerLevel level, int chunkX, int chunkZ) {
        var registry = worlds.get(level);
        if (registry != null) registry.unloadChunk(chunkX, chunkZ);
        observer.chunkRemoved(level, chunkX, chunkZ);
    }

    /**
     * Returns true if a newer queued change makes {@code change} useless. A queued level unload, or a
     * queued load or unload of the same chunk, supersedes it. A queued sample or deferral does not.
     * The caller must already have removed {@code change} from the queue.
     */
    private synchronized boolean superseded(Change change) {
        var level = change.key.level;
        if (pending.containsKey(new Key(level, Scope.LEVEL, 0))) return true;
        long chunk = change.key.scope == Scope.CHUNK ? change.key.coordinate : ChunkPos.asLong(change.position);
        var newer = pending.get(new Key(level, Scope.CHUNK, chunk));
        return newer != null && (newer.kind == Kind.LOAD_CHUNK || newer.kind == Kind.UNLOAD_CHUNK);
    }

    /**
     * Samples positions of a chunk scan until the scan ends, the work budget ends, or a newer change
     * supersedes the scan. If positions are left, the scan goes back into the queue as a
     * {@code SCAN_CHUNK} change.
     *
     * @param prepaidPosition true if the reserved unit of the dequeued change pays for the first
     *                        position. False if that unit already paid for the scan snapshot.
     */
    private void scanChunk(Change change, LevelChunk chunk, ChunkScan scan, boolean prepaidPosition) {
        while (scan.next < scan.positions.length) {
            // An observer callback can close the topology or queue a newer change. Check before each position.
            if (closed || !failure.isEmpty() || superseded(change)) return;
            // Each dequeued change already reserves one unit. A SCAN uses it
            // for its first position. A LOAD uses it for snapshot initialization.
            if (!prepaidPosition) {
                if (remainingWork == 0) break;
                remainingWork--;
            }
            prepaidPosition = false;
            sample(change.key.level, chunk, BlockPos.of(scan.positions[scan.next++]));
        }
        if (scan.next < scan.positions.length && !closed && !superseded(change))
            enqueue(new Change(change.key, Kind.SCAN_CHUNK, BlockPos.ZERO, scan), true);
    }

    /**
     * Applies one dequeued change to the index. Runs only on the server thread.
     *
     * <p>A chunk change on a missing chunk removes the old index entries once, then waits as a
     * {@code DEFERRED_LOAD}. A position change on a missing chunk removes the chunk entries and is
     * dropped. A chunk change on a loaded chunk removes the old entries, then scans the chunk from the
     * beginning. A {@code SCAN_CHUNK} continues only if the chunk object is still the same.
     *
     * @throws IllegalStateException if the caller is not on the server thread
     */
    private void apply(Change change) {
        ServerLevel level = change.key.level;
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("World mutation outside server thread"); }
        if (change.kind == Kind.UNLOAD_LEVEL) {
            var old = worlds.remove(level); if (old != null) { old.close(); }
            observer.levelRemoved(level); return;
        }
        if (change.kind == Kind.UNLOAD_CHUNK) {
            removeChunk(level, ChunkPos.getX(change.key.coordinate), ChunkPos.getZ(change.key.coordinate));
            return;
        }
        if (superseded(change)) return;
        int cx = change.key.scope == Scope.CHUNK ? ChunkPos.getX(change.key.coordinate) : change.position.getX() >> 4;
        int cz = change.key.scope == Scope.CHUNK ? ChunkPos.getZ(change.key.coordinate) : change.position.getZ() >> 4;
        // getChunkNow never loads or generates a chunk. It returns null for a chunk that is not ready.
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            if (change.key.scope == Scope.CHUNK) {
                // Revoke old routes before making this retry nonblocking. Do
                // not repeat lifecycle callbacks on every still-missing retry.
                if (change.kind != Kind.DEFERRED_LOAD) removeChunk(level, cx, cz);
                if (closed || superseded(change)) return;
                deferredLoads++;
                enqueue(new Change(change.key, Kind.DEFERRED_LOAD, change.position), true);
            } else {
                removeChunk(level, cx, cz);
            }
            return;
        }
        if (change.kind == Kind.SCAN_CHUNK && change.scan.source.get() == chunk) {
            scanChunk(change, chunk, change.scan, true);
        } else if (change.key.scope == Scope.CHUNK) {
            // Full rescan: remove first, so that block entities deleted since the last scan disappear.
            removeChunk(level, cx, cz);
            if (closed || superseded(change)) return;
            scanChunk(change, chunk, new ChunkScan(chunk), false);
        } else { sample(level, chunk, change.position); }
    }

    /**
     * Reads one position of a loaded chunk and updates the index for it.
     *
     * <p>If the block ID is a key of {@code losses}, the position becomes a conductor with that loss
     * and with the face mask from the observer. Otherwise any conductor entry at the position is
     * removed. Then the observer receives {@link Observer#position}.
     *
     * @throws IllegalStateException if a registered conductor block has no block entity, or if the
     *                               registry is full
     */
    private void sample(ServerLevel level, LevelChunk chunk, BlockPos at) {
        if (closed) return;
        BlockState state = chunk.getBlockState(at);
        Long loss = losses.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        var registry = worlds.get(level);
        if (loss != null) {
            if (!state.hasBlockEntity()) { throw new IllegalStateException("Registered conductor requires a block entity"); }
            if (registry == null) { registry = new ConductorRegistry(maximumNodes, maximumSources); worlds.put(level, registry); }
            int faces = observer.conductorFaces(level, chunk, at);
            // The observer callback can close the topology. Do not write into a closed registry.
            if (closed) return;
            registry.put(new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ()), loss, faces);
        } else if (registry != null) { registry.remove(new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ())); }
        sampledPositions++;
        observer.position(level, chunk, at);
    }

    /** Returns true if a blocking change of the level is queued or running. */
    private synchronized boolean pendingFor(ServerLevel level) {
        return blockingByLevel.containsKey(level) || processingByLevel.containsKey(level);
    }

    /**
     * Cheap status query for transaction and tick callers. An empty text means no failure.
     * {@link #metrics()} stays a diagnostic snapshot.
     */
    public String failure() { return failure; }

    /**
     * Returns true if the whole index of the level is complete and current.
     *
     * <p>The level must be {@link #operational}. No blocking change of the level may be queued or
     * running. No chunk of the level may be in quarantine. A {@code DEFERRED_LOAD} does not block,
     * because its chunk entries are already removed. Call only on the server thread.
     */
    public boolean ready(ServerLevel level) {
        return operational(level) && !pendingFor(level) && quarantined.keySet().stream().noneMatch(key -> key.level == level);
    }

    /**
     * Unrelated loaded circuits may run while a different chunk is being sampled.
     *
     * <p>Returns true if the topology is open, the caller is on the server thread, the level belongs to
     * this server and is still registered, the level is not in a rebuild, and no level unload is
     * queued. This is the precondition of {@link #chunkReady}. It does not mean that every chunk is ready.
     */
    public synchronized boolean operational(ServerLevel level) {
        return !closed && failure.isEmpty() && level.getServer() == server && server.isSameThread()
            && server.getLevel(level.dimension()) == level && !recovering.containsKey(level)
            && !pending.containsKey(new Key(level, Scope.LEVEL, 0));
    }
    /**
     * Returns true if the index entries of one chunk are complete and current. {@code chunk} is a
     * {@link ChunkPos#asLong} value. The level must be {@link #operational}. No blocking change of the
     * chunk may be queued or running, and the chunk must not be in quarantine.
     */
    public synchronized boolean chunkReady(ServerLevel level, long chunk) {
        var key = new Key(level, Scope.CHUNK, chunk);
        return operational(level) && !pendingChunks.containsKey(key) && !processingChunks.containsKey(key) && !quarantined.containsKey(key);
    }

    /**
     * Returns an immutable graph lease of the level index. The registry builds the edges and
     * components lazily and caches the result until the next index change.
     *
     * @throws IllegalStateException if the level is not {@link #ready}
     */
    public ConductorRegistry.Snapshot snapshot(ServerLevel level) {
        if (!ready(level)) { throw new IllegalStateException("Topology is pending, closed, failed or on another thread"); }
        return worlds.computeIfAbsent(level, key -> new ConductorRegistry(maximumNodes, maximumSources)).snapshot();
    }

    /**
     * Returns true if the level is {@link #ready} and {@code snapshot} is still the current lease of
     * its registry. Call this directly before an energy commit. Do not commit with a lease that fails
     * this check.
     */
    public boolean isCurrent(ServerLevel level, ConductorRegistry.Snapshot snapshot) {
        if (!ready(level)) { return false; }
        var registry = worlds.get(level);
        return registry != null && registry.isCurrent(snapshot);
    }

    /** Returns a diagnostic snapshot of the counters. Safe to call from any thread. */
    public synchronized Metrics metrics() {
        return new Metrics(worlds.size(), pending.size(), chunkLoads, chunkUnloads, blockSignals,
                sampledPositions, deferredLoads, accessibilityChanges, closed, failure);
    }

    /** Closes every registry and tells the observer. */
    private void clearWorlds() {
        for (var registry : worlds.values()) { registry.close(); }
        worlds.clear(); observer.cleared();
    }

    /** Closes the topology when its server stops. */
    @SubscribeEvent
    public void onStopped(ServerStoppedEvent event) { if (event.getServer() == server) { close(); } }

    /**
     * Closes all registries, drops all queued work and unregisters from the event bus. A second call
     * has no effect. After this call, all status queries return false and all event handlers do
     * nothing.
     *
     * @throws IllegalStateException if the caller is not on the server thread
     */
    @Override
    public synchronized void close() {
        if (closed) { return; }
        if (!server.isSameThread()) { throw new IllegalStateException("Close on the server thread"); }
        // A lifecycle callback may close again or enqueue work. Revoke the
        // adapter before invoking it, including when close interrupts a batch.
        closed = true;
        synchronized (PlatformTopology.class) {
            var instances = INSTANCES.get(server);
            if (instances != null) { instances.remove(this); if (instances.isEmpty()) INSTANCES.remove(server); }
        }
        try { clearWorlds(); }
        finally {
            clearPending(); server = null; observer = NO_OBSERVER; NeoForge.EVENT_BUS.unregister(this);
            knownChunks.clear(); recovering.clear(); quarantined.clear(); processingChunks.clear();
        }
    }
}
