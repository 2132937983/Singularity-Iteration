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
 * Public-platform lifecycle adapter for registered block-entity conductors.
 * It observes supplied block IDs and losses, and never accesses an SI/IC2 Java
 * class or transfers energy. Chunk loads are deferred until getChunkNow succeeds.
 * The future energy adapter must use isCurrent again immediately before commit.
 */
public final class PlatformTopology implements AutoCloseable {
    /** Index-only callbacks on the server thread; they must not mutate the world. */
    public interface Observer {
        default void position(ServerLevel level, LevelChunk chunk, BlockPos at) { }
        /** Called only for an already loaded conductor chunk, on the server thread. */
        default int conductorFaces(ServerLevel level, LevelChunk chunk, BlockPos at) { return ConductorRegistry.ALL_FACES; }
        default void blockChanged(ServerLevel level, BlockPos at, BlockState before, BlockState after) { }
        default void chunkRemoved(ServerLevel level, int chunkX, int chunkZ) { }
        default void levelRemoved(ServerLevel level) { }
        default void cleared() { }
    }
    private static final Observer NO_OBSERVER = new Observer() { };
    private static final Map<MinecraftServer, ArrayList<PlatformTopology>> INSTANCES = new java.util.IdentityHashMap<>();

    /** Public chunk mutation callback. Only observer metadata may change here. */
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
    private enum Kind { SAMPLE, LOAD_CHUNK, DEFERRED_LOAD, SCAN_CHUNK, UNLOAD_CHUNK, UNLOAD_LEVEL }
    private enum Scope { POSITION, CHUNK, LEVEL }
    private record Key(ServerLevel level, Scope scope, long coordinate) { }
    private record Change(Key key, Kind kind, BlockPos position, ChunkScan scan) {
        Change(Key key, Kind kind, BlockPos position) { this(key, kind, position, null); }
    }
    private static final class ChunkScan {
        final WeakReference<LevelChunk> source;
        /**
         * Encoded positions keep the scan snapshot compact and detached from
         * the chunk's mutable block-entity map.  The previous implementation
         * cloned every position into a fresh BlockPos, which doubled the
         * allocation rate for large machine chunks and left a large temporary
         * object graph for the GC to reclaim.  A packed long is the native
         * representation used by ChunkPos/BlockPos and is materialized only
         * for the position currently consumed by the tick budget.
         */
        final long[] positions;
        int next;
        ChunkScan(LevelChunk chunk) {
            source = new WeakReference<>(chunk);
            // The public position set and this immutable snapshot still cost
            // O(B) once. Only per-BE sampling/materialization is tick-budgeted;
            // neither a live iterator nor a strong chunk reference spans ticks.
            positions = chunk.getBlockEntitiesPos().stream().mapToLong(BlockPos::asLong).toArray();
        }
    }
    public record Metrics(int dimensions, int queued, long chunkLoads, long chunkUnloads,
                          long blockSignals, long sampledPositions, long deferredLoads, long accessibilityChanges,
                          boolean closed, String failure) { }

    private final Map<ResourceLocation, Long> losses;
    private Observer observer;
    private MinecraftServer server;
    private final int maximumNodes;
    private final int maximumSources;
    private final int maximumQueued;
    private final int workPerTick;
    private final Map<ServerLevel, ConductorRegistry> worlds = new HashMap<>();
    private final LinkedHashMap<Key, Change> pending = new LinkedHashMap<>();
    // Readiness is queried at every transaction boundary. Keep the level count
    // beside the ordered queue instead of scanning other dimensions each time.
    private final Map<ServerLevel, Integer> pendingByLevel = new java.util.IdentityHashMap<>();
    // Only a load already observed missing, with its old indexes revoked, may
    // wait without blocking unrelated loaded networks in the same dimension.
    private final Map<ServerLevel, Integer> blockingByLevel = new java.util.IdentityHashMap<>();
    private final Map<ServerLevel, Integer> processingByLevel = new java.util.IdentityHashMap<>();
    private final Map<Key, Integer> pendingChunks = new HashMap<>(), processingChunks = new HashMap<>();
    private final Map<ServerLevel, java.util.LinkedHashSet<Long>> knownChunks = new java.util.IdentityHashMap<>();
    private final Map<ServerLevel, Recovery> recovering = new java.util.IdentityHashMap<>();
    private final Map<Key, Long> quarantined = new HashMap<>();
    private long recoveries, isolatedFailures, readinessRevision;
    public synchronized long readinessRevision() { return readinessRevision; }
    private String lastRecovery = "";
    private static final class Recovery {
        final long[] chunks; int next;
        Recovery(java.util.Set<Long> chunks) { this.chunks = chunks.stream().mapToLong(Long::longValue).toArray(); }
    }
    public record RecoveryMetrics(long rebuilds, long isolatedFailures, int recoveringLevels, int quarantinedChunks, String lastReason) { }
    public synchronized RecoveryMetrics recoveryMetrics() {
        return new RecoveryMetrics(recoveries, isolatedFailures, recovering.size(), quarantined.size(), lastRecovery);
    }
    private static Key chunkKey(Change change) {
        return new Key(change.key.level, Scope.CHUNK,
            change.key.scope == Scope.CHUNK ? change.key.coordinate : ChunkPos.asLong(change.position));
    }
    private synchronized void quarantine(Key key, RuntimeException error) {
        readinessRevision++;
        if (quarantined.put(key, (long) server.getTickCount() + 20) == null) {
            isolatedFailures++;
            com.mojang.logging.LogUtils.getLogger().warn("Energy topology isolated chunk {} in {}: {}",
                key.coordinate, key.level.dimension().location(), error.toString());
        }
        lastRecovery = error.toString();
        try { removeChunk(key.level, ChunkPos.getX(key.coordinate), ChunkPos.getZ(key.coordinate)); }
        catch (RuntimeException removalFailure) { lastRecovery = removalFailure.toString(); }
    }
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
    private void pumpRecovery() {
        for (var entry : recovering.entrySet()) {
            var scan = entry.getValue();
            while (pending.size() < maximumQueued && scan.next < scan.chunks.length) {
                long chunk = scan.chunks[scan.next++];
                // A chunk may unload after the rebuild snapshot was taken.
                // Replaying that old entry would resurrect an endless deferred load.
                if (!knownChunks.getOrDefault(entry.getKey(), new java.util.LinkedHashSet<>()).contains(chunk)) continue;
                enqueue(new Change(new Key(entry.getKey(), Scope.CHUNK, chunk), Kind.LOAD_CHUNK, BlockPos.ZERO), true);
            }
        }
        for (var entry : java.util.List.copyOf(quarantined.entrySet())) {
            if (pending.size() >= maximumQueued) break;
            if (entry.getValue() <= server.getTickCount()) {
                quarantined.put(entry.getKey(), (long) server.getTickCount() + 20);
                enqueue(new Change(entry.getKey(), Kind.LOAD_CHUNK, BlockPos.ZERO), true);
            }
        }
        recovering.entrySet().removeIf(entry -> entry.getValue().next == entry.getValue().chunks.length
            && !blockingByLevel.containsKey(entry.getKey()) && !processingByLevel.containsKey(entry.getKey()));
    }

    public synchronized void isolateChunk(ServerLevel level, BlockPos position, RuntimeException error) {
        if (!closed && level.getServer() == server && server.isSameThread())
            quarantine(new Key(level, Scope.CHUNK, ChunkPos.asLong(position)), error);
    }
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
    private volatile String failure = "";
    private long chunkLoads, chunkUnloads, blockSignals, sampledPositions, deferredLoads, accessibilityChanges;
    private long queueCompactions;
    private int budgetTick = Integer.MIN_VALUE, remainingWork;

    public PlatformTopology(MinecraftServer server, Map<ResourceLocation, Long> losses, int maximumNodes,
                            int maximumSources, int maximumQueued, int workPerTick) {
        this(server, losses, maximumNodes, maximumSources, maximumQueued, workPerTick, NO_OBSERVER);
    }

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

    private synchronized void enqueue(Change change, boolean retry) {
        if (closed || !failure.isEmpty()) { return; }
        if (change.kind == Kind.UNLOAD_LEVEL) knownChunks.remove(change.key.level);
        else if (change.kind == Kind.UNLOAD_CHUNK) {
            var known = knownChunks.get(change.key.level); if (known != null) known.remove(change.key.coordinate);
            quarantined.remove(change.key);
        } else if (change.kind == Kind.LOAD_CHUNK || change.kind == Kind.SAMPLE
                && change.key.level.getChunkSource().getChunkNow(change.position.getX() >> 4, change.position.getZ() >> 4) != null) {
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
                var levels = new java.util.HashSet<ServerLevel>();
                for (var key : compacted.keySet()) levels.add(key.level);
                clearPending(); beginRecovery(levels); queueCompactions++; return;
            }
            clearPending();
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
        if (previous == null) pendingByLevel.merge(change.key.level, 1, Integer::sum);
        else if (previous.kind != Kind.DEFERRED_LOAD) {
            decrement(blockingByLevel, previous.key.level);
            if (previous.key.scope != Scope.LEVEL) decrementKey(pendingChunks, chunkKey(previous));
        }
        if (change.kind != Kind.DEFERRED_LOAD) blockingByLevel.merge(change.key.level, 1, Integer::sum);
        if (change.kind != Kind.DEFERRED_LOAD && change.key.scope != Scope.LEVEL) pendingChunks.merge(chunkKey(change), 1, Integer::sum);
    }

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

    public synchronized long queueCompactions() { return queueCompactions; }

    private static void decrement(Map<ServerLevel, Integer> counts, ServerLevel level) {
        counts.compute(level, (key, count) -> count == 1 ? null : count - 1);
    }
    private static void decrementKey(Map<Key, Integer> counts, Key at) {
        counts.computeIfPresent(at, (key, count) -> count == 1 ? null : count - 1);
    }

    private void removedPending(Change change) {
        decrement(pendingByLevel, change.key.level);
        if (change.kind != Kind.DEFERRED_LOAD) decrement(blockingByLevel, change.key.level);
        if (change.kind != Kind.DEFERRED_LOAD && change.key.scope != Scope.LEVEL) decrementKey(pendingChunks, chunkKey(change));
    }

    private void clearPending() { pending.clear(); pendingByLevel.clear(); blockingByLevel.clear(); pendingChunks.clear(); }

    /** Explicit hook for mutations which intentionally suppress neighbour events. */
    public void changed(ServerLevel level, BlockPos position) {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(position, "position");
        if (level.getServer() != server) { return; }
        BlockPos immutable = position.immutable();
        enqueue(new Change(new Key(level, Scope.POSITION, immutable.asLong()), Kind.SAMPLE, immutable), false);
    }

    @SubscribeEvent
    public void onNeighbours(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { blockSignals++; }
        changed(level, event.getPos());
        for (Direction direction : event.getNotifiedSides()) { changed(level, event.getPos().relative(direction)); }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { chunkLoads++; }
        enqueue(new Change(new Key(level, Scope.CHUNK, event.getChunk().getPos().toLong()), Kind.LOAD_CHUNK, BlockPos.ZERO), false);
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) { chunkUnloads++; }
        queueChunkRemoval(level, event.getChunk().getPos().toLong());
    }

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
        // Coalesce public ticket transitions; do not retain a ChunkHolder or load here.
        if (nowFull) {
            enqueue(new Change(new Key(level, Scope.CHUNK, event.getChunkPos()), Kind.LOAD_CHUNK, BlockPos.ZERO), false);
        } else { queueChunkRemoval(level, event.getChunkPos()); }
    }

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

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getServer() != server) { return; }
        synchronized (this) {
            pending.entrySet().removeIf(entry -> entry.getKey().level == level);
            pendingByLevel.remove(level);
            blockingByLevel.remove(level);
            pendingChunks.keySet().removeIf(key -> key.level == level);
            recovering.remove(level); knownChunks.remove(level); quarantined.keySet().removeIf(key -> key.level == level);
            enqueue(new Change(new Key(level, Scope.LEVEL, 0), Kind.UNLOAD_LEVEL, BlockPos.ZERO), false);
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        if (event.getServer() != server) { return; }
        drain(null);
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getServer() == server) drain(level);
    }

    private void drain(ServerLevel onlyLevel) {
        ArrayList<Change> batch;
        synchronized (this) {
            if (closed) { return; }
            if (!failure.isEmpty()) { clearWorlds(); return; }
            if (!processingByLevel.isEmpty()) return;
            pumpRecovery();
            int now = server.getTickCount();
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

    private void removeChunk(ServerLevel level, int chunkX, int chunkZ) {
        var registry = worlds.get(level);
        if (registry != null) registry.unloadChunk(chunkX, chunkZ);
        observer.chunkRemoved(level, chunkX, chunkZ);
    }

    private synchronized boolean superseded(Change change) {
        var level = change.key.level;
        if (pending.containsKey(new Key(level, Scope.LEVEL, 0))) return true;
        long chunk = change.key.scope == Scope.CHUNK ? change.key.coordinate : ChunkPos.asLong(change.position);
        var newer = pending.get(new Key(level, Scope.CHUNK, chunk));
        return newer != null && (newer.kind == Kind.LOAD_CHUNK || newer.kind == Kind.UNLOAD_CHUNK);
    }

    private void scanChunk(Change change, LevelChunk chunk, ChunkScan scan, boolean prepaidPosition) {
        while (scan.next < scan.positions.length) {
            if (closed || !failure.isEmpty() || superseded(change)) return;
            // Each dequeued change already reserves one unit. A SCAN uses it
            // for its first position; a LOAD uses it for snapshot initialization.
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
            removeChunk(level, cx, cz);
            if (closed || superseded(change)) return;
            scanChunk(change, chunk, new ChunkScan(chunk), false);
        } else { sample(level, chunk, change.position); }
    }

    private void sample(ServerLevel level, LevelChunk chunk, BlockPos at) {
        if (closed) return;
        BlockState state = chunk.getBlockState(at);
        Long loss = losses.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        var registry = worlds.get(level);
        if (loss != null) {
            if (!state.hasBlockEntity()) { throw new IllegalStateException("Registered conductor requires a block entity"); }
            if (registry == null) { registry = new ConductorRegistry(maximumNodes, maximumSources); worlds.put(level, registry); }
            int faces = observer.conductorFaces(level, chunk, at);
            if (closed) return;
            registry.put(new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ()), loss, faces);
        } else if (registry != null) { registry.remove(new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ())); }
        sampledPositions++;
        observer.position(level, chunk, at);
    }

    private synchronized boolean pendingFor(ServerLevel level) {
        return blockingByLevel.containsKey(level) || processingByLevel.containsKey(level);
    }

    /** Cheap status query for transaction/tick callers; metrics remain diagnostic snapshots. */
    public String failure() { return failure; }

    public boolean ready(ServerLevel level) {
        return operational(level) && !pendingFor(level) && quarantined.keySet().stream().noneMatch(key -> key.level == level);
    }

    /** Unrelated loaded circuits may run while a different chunk is being sampled. */
    public synchronized boolean operational(ServerLevel level) {
        return !closed && failure.isEmpty() && level.getServer() == server && server.isSameThread()
            && server.getLevel(level.dimension()) == level && !recovering.containsKey(level)
            && !pending.containsKey(new Key(level, Scope.LEVEL, 0));
    }
    public synchronized boolean chunkReady(ServerLevel level, long chunk) {
        var key = new Key(level, Scope.CHUNK, chunk);
        return operational(level) && !pendingChunks.containsKey(key) && !processingChunks.containsKey(key) && !quarantined.containsKey(key);
    }

    public ConductorRegistry.Snapshot snapshot(ServerLevel level) {
        if (!ready(level)) { throw new IllegalStateException("Topology is pending, closed, failed or on another thread"); }
        return worlds.computeIfAbsent(level, key -> new ConductorRegistry(maximumNodes, maximumSources)).snapshot();
    }

    public boolean isCurrent(ServerLevel level, ConductorRegistry.Snapshot snapshot) {
        if (!ready(level)) { return false; }
        var registry = worlds.get(level);
        return registry != null && registry.isCurrent(snapshot);
    }

    public synchronized Metrics metrics() {
        return new Metrics(worlds.size(), pending.size(), chunkLoads, chunkUnloads, blockSignals,
                sampledPositions, deferredLoads, accessibilityChanges, closed, failure);
    }

    private void clearWorlds() {
        for (var registry : worlds.values()) { registry.close(); }
        worlds.clear(); observer.cleared();
    }

    @SubscribeEvent
    public void onStopped(ServerStoppedEvent event) { if (event.getServer() == server) { close(); } }

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
