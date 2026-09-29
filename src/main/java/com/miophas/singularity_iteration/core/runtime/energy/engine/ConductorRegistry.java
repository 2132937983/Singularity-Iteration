// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Thread-confined, bounded registration of conductors on a six-neighbour lattice.
 * This new lifecycle mechanism is a platform-neutral design, not an observation
 * about an existing game's implementation. No method accesses or loads a world.
 */
public final class ConductorRegistry implements AutoCloseable {
    /** Coordinates are supplied by the platform adapter; no position is loaded. */
    public record Position(int x, int y, int z) implements Comparable<Position> {
        @Override
        public int compareTo(Position other) {
            int result = Integer.compare(x, other.x);
            if (result == 0) { result = Integer.compare(y, other.y); }
            if (result == 0) { result = Integer.compare(z, other.z); }
            return result;
        }
    }

    /**
     * Thread-confined work allowance shared by every routing pass in a world tick.
     * Charges component vertices before a cache-miss search starts. Repeating
     * reset for the same tick cannot replenish an exhausted allowance.
     */
    public static final class SearchBudget {
        private final Thread owner = Thread.currentThread();
        private final long maximumVertices;
        private long tick;
        private long remainingVertices;
        private boolean initialized;

        public SearchBudget(long maximumVerticesPerTick) {
            if (maximumVerticesPerTick <= 0) throw new IllegalArgumentException("Positive search budget required");
            maximumVertices = maximumVerticesPerTick;
        }

        private void active() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("Search budget is thread-confined");
        }

        public void reset(long currentTick) {
            active();
            if (initialized && tick == currentTick) return;
            tick = currentTick;
            remainingVertices = maximumVertices;
            initialized = true;
        }

        private void ready() {
            active();
            if (!initialized) throw new IllegalStateException("Search budget must be reset before use");
        }

        /** An unaffordable search leaves the allowance unchanged and must resume later. */
        public boolean tryConsume(int vertices) {
            ready();
            if (vertices < 0) throw new IllegalArgumentException("Negative search cost");
            if (vertices > remainingVertices) return false;
            remainingVertices -= vertices;
            return true;
        }

        public long remainingVertices() { ready(); return remainingVertices; }
    }

    private final Thread owner = Thread.currentThread();
    private final int maximumNodes;
    private final int maximumCachedSources;
    private final long maximumCachedRouteVertices;
    /** Face bits: down, up, north (-Z), south (+Z), west (-X), east (+X). */
    public static final int ALL_FACES = 63;
    private record Conductor(long loss, int faces) { }
    private final Map<Position, Conductor> conductors = new HashMap<>();
    private final Map<Long, HashSet<Position>> chunks = new HashMap<>();
    private final HashSet<Long> inactiveChunks = new HashSet<>();
    private Snapshot cached;
    private boolean closed;
    private long revision;
    private long rebuilds;

    public ConductorRegistry(int maximumNodes, int maximumCachedSources) {
        this(maximumNodes, maximumCachedSources, (long) maximumNodes * maximumCachedSources);
    }

    /** Bound both retained source count and the total component-sized route arrays. */
    public ConductorRegistry(int maximumNodes, int maximumCachedSources, long maximumCachedRouteVertices) {
        if (maximumNodes <= 0 || maximumNodes > (Integer.MAX_VALUE - 1) / 6 || maximumCachedSources <= 0) {
            throw new IllegalArgumentException("Positive bounded registry/cache sizes required");
        }
        if (maximumCachedRouteVertices <= 0) throw new IllegalArgumentException("Positive route-array budget required");
        this.maximumNodes = maximumNodes;
        this.maximumCachedSources = maximumCachedSources;
        this.maximumCachedRouteVertices = maximumCachedRouteVertices;
    }

    private void active() {
        if (Thread.currentThread() != owner) { throw new IllegalStateException("Registry is thread-confined"); }
        if (closed) { throw new IllegalStateException("Registry is closed"); }
    }

    private static long chunkKey(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
    private static long chunkKey(Position at) { return chunkKey(at.x >> 4, at.z >> 4); }

    private void changed() {
        // Fail before mutating a registry if its version cannot be represented.
        revision = Math.incrementExact(revision);
        if (cached != null) { cached.invalidate(); cached = null; }
    }

    /** Register or replace a loss. Identical updates retain valid cached work. */
    public boolean put(Position at, long lossMilli) { return put(at, lossMilli, ALL_FACES); }

    /** Changing only a face mask invalidates every old route lease before publication. */
    public boolean put(Position at, long lossMilli, int openFaces) {
        active(); Objects.requireNonNull(at, "at");
        if ((openFaces & ~ALL_FACES) != 0) throw new IllegalArgumentException("Invalid conductor face mask");
        if (lossMilli < 0) { throw new IllegalArgumentException("Negative conductor loss"); }
        Conductor previous = conductors.get(at);
        if (previous != null && previous.loss == lossMilli && previous.faces == openFaces) { return false; }
        if (previous == null && conductors.size() >= maximumNodes) { throw new IllegalStateException("Registry capacity reached"); }
        changed();
        conductors.put(at, new Conductor(lossMilli, openFaces));
        if (previous == null) { chunks.computeIfAbsent(chunkKey(at), key -> new HashSet<>()).add(at); }
        return true;
    }

    public boolean remove(Position at) {
        active(); Objects.requireNonNull(at, "at");
        if (!conductors.containsKey(at)) { return false; }
        changed(); conductors.remove(at);
        long key = chunkKey(at); var members = chunks.get(key); members.remove(at);
        if (members.isEmpty()) { chunks.remove(key); inactiveChunks.remove(key); }
        return true;
    }

    /** Work is proportional to registered nodes in that chunk, not the world. */
    public int unloadChunk(int chunkX, int chunkZ) {
        active(); long key = chunkKey(chunkX, chunkZ); var members = chunks.get(key);
        if (members == null) { return 0; }
        changed();
        for (Position at : members) { conductors.remove(at); }
        chunks.remove(key); inactiveChunks.remove(key);
        return members.size();
    }

    /** Keep registration history while excluding unavailable chunks from every route lease. */
    public boolean setChunkActive(int chunkX, int chunkZ, boolean available) {
        active(); long key = chunkKey(chunkX, chunkZ);
        if (!chunks.containsKey(key) || inactiveChunks.contains(key) == !available) return false;
        changed();
        if (available) inactiveChunks.remove(key); else inactiveChunks.add(key);
        return true;
    }

    public int size() { active(); return conductors.size(); }
    /** Cheap membership observation without constructing or retaining a graph snapshot. */
    public boolean containsRegistered(Position at) { active(); return conductors.containsKey(Objects.requireNonNull(at, "at")); }
    /** Constant-time contact query; an absent conductor has no open face. */
    public boolean permitsRegistered(Position at, int face) {
        active(); checkFace(face); var conductor = conductors.get(Objects.requireNonNull(at, "at"));
        return conductor != null && (conductor.faces & (1 << face)) != 0;
    }
    private static void checkFace(int face) {
        if (face < 0 || face >= 6) throw new IllegalArgumentException("Unknown conductor face");
    }
    public long revision() { active(); return revision; }
    public long rebuildCount() { active(); return rebuilds; }

    /** Coalesces any number of edits into one lazily rebuilt immutable topology. */
    public Snapshot snapshot() {
        active();
        if (cached != null) { return cached; }
        long nextRebuild = Math.incrementExact(rebuilds);
        Position[] positions = conductors.keySet().stream()
            .filter(at -> !inactiveChunks.contains(chunkKey(at))).toArray(Position[]::new);
        Arrays.sort(positions);
        var ids = new HashMap<Position, Integer>();
        long[] losses = new long[positions.length]; int[] faces = new int[positions.length];
        for (int i = 0; i < positions.length; i++) {
            ids.put(positions[i], i); var conductor = conductors.get(positions[i]);
            losses[i] = conductor.loss; faces[i] = conductor.faces;
        }
        var links = new ArrayList<int[]>();
        for (int i = 0; i < positions.length; i++) {
            Position at = positions[i];
            if (at.x < Integer.MAX_VALUE) { link(links, ids, faces, i, 5, new Position(at.x + 1, at.y, at.z)); }
            if (at.y < Integer.MAX_VALUE) { link(links, ids, faces, i, 1, new Position(at.x, at.y + 1, at.z)); }
            if (at.z < Integer.MAX_VALUE) { link(links, ids, faces, i, 3, new Position(at.x, at.y, at.z + 1)); }
        }
        ConductorGraph graph = positions.length == 0 ? null : new ConductorGraph(losses, links.toArray(int[][]::new));
        cached = new Snapshot(owner, revision, Map.copyOf(ids), positions, faces, graph,
            maximumCachedSources, maximumCachedRouteVertices);
        rebuilds = nextRebuild;
        return cached;
    }

    private static void link(ArrayList<int[]> links, Map<Position, Integer> ids, int[] faces, int from, int face, Position to) {
        Integer target = ids.get(to);
        if (target != null && (faces[from] & (1 << face)) != 0 && (faces[target] & (1 << (face ^ 1))) != 0) {
            links.add(new int[]{from, target});
        }
    }

    /** Must be checked again by a future world adapter before committing energy. */
    public boolean isCurrent(Snapshot snapshot) {
        active(); return snapshot != null && cached == snapshot && snapshot.valid;
    }

    @Override
    public void close() {
        if (Thread.currentThread() != owner) { throw new IllegalStateException("Registry is thread-confined"); }
        if (closed) { return; }
        if (cached != null) { cached.invalidate(); cached = null; }
        conductors.clear(); chunks.clear(); inactiveChunks.clear(); closed = true;
    }

    /**
     * A topology lease. Edits/close reject further lease access and release its
     * route cache. Previously returned pure route values remain old calculations;
     * their continued existence never authorizes applying them to a world.
     */
    public static final class Snapshot {
        private final Thread owner;
        private final long revision;
        private final Map<Position, Integer> ids;
        private final Position[] positions;
        private final int[] faces;
        private final ConductorGraph graph;
        private final int maximumCachedSources;
        private final long maximumCachedRouteVertices;
        private final LinkedHashMap<Integer, ConductorGraph.Routes> routes = new LinkedHashMap<>(16, .75f, true);
        private long cachedRouteVertices;
        private long routeComputations;
        private boolean valid = true;

        private Snapshot(Thread owner, long revision, Map<Position, Integer> ids, Position[] positions, int[] faces, ConductorGraph graph, int limit, long vertexLimit) {
            this.owner = owner; this.revision = revision; this.ids = ids; this.positions = positions; this.faces = faces; this.graph = graph; maximumCachedSources = limit;
            maximumCachedRouteVertices = vertexLimit;
        }

        private void active() {
            if (Thread.currentThread() != owner || !valid) { throw new IllegalStateException("Stale or foreign-thread snapshot"); }
        }
        private void invalidate() { valid = false; routes.clear(); cachedRouteVertices = 0; }
        public long revision() { active(); return revision; }
        public int size() { active(); return ids.size(); }
        public boolean contains(Position at) { active(); return ids.containsKey(Objects.requireNonNull(at, "at")); }
        public int vertex(Position at) {
            active(); Integer result = ids.get(Objects.requireNonNull(at, "at"));
            if (result == null) { throw new IllegalArgumentException("Unregistered conductor position"); }
            return result;
        }
        /** A wire-to-machine contact must also be allowed by the wire's facing side. */
        public boolean permits(Position at, int face) { checkFace(face); return (faces[vertex(at)] & (1 << face)) != 0; }
        public int openFaces(Position at) { return faces[vertex(at)]; }
        public int cachedSources() { active(); return routes.size(); }
        public long cachedRouteVertices() { active(); return cachedRouteVertices; }
        public long routeComputations() { active(); return routeComputations; }
        public Position position(int vertex) {
            active();
            if (vertex < 0 || vertex >= positions.length) throw new IllegalArgumentException("Unknown conductor ID");
            return positions[vertex];
        }
        /** A path tied to this lease; retaining it never authorizes a later world write. */
        public Path path(Position source, Position receiver) {
            var index = routesFrom(source); int target = vertex(receiver);
            if (!index.reaches(target)) throw new IllegalArgumentException("Unreachable conductor");
            return new Path(this, index, target);
        }
        public int componentOf(Position at) { return graph.componentOf(vertex(at)); }
        public int componentSize(Position at) { return graph.componentSize(vertex(at)); }
        public ConductorGraph.Routes routesFrom(Position source) {
            return routesFrom(vertex(source), null);
        }

        /**
         * Returns null if an uncached search cannot fit this tick's allowance.
         * Cached searches are free. The allowance must cover the largest single
         * component to guarantee eventual progress; searches are not interruptible.
         */
        public RouteIndex routesFrom(Position source, SearchBudget budget) {
            Objects.requireNonNull(budget, "budget").ready();
            var result = routesFrom(vertex(source), budget);
            return result == null ? null : new RouteIndex(this, result);
        }

        private ConductorGraph.Routes routesFrom(int id, SearchBudget budget) {
            var result = routes.get(id);
            if (result == null) {
                int weight = graph.componentSize(id);
                if (budget != null && !budget.tryConsume(weight)) return null;
                result = graph.routesFrom(id);
                routeComputations++;
                if (weight <= maximumCachedRouteVertices) {
                    while (routes.size() >= maximumCachedSources
                            || cachedRouteVertices > maximumCachedRouteVertices - weight) {
                        var evicted = routes.pollFirstEntry();
                        cachedRouteVertices -= graph.componentSize(evicted.getKey());
                    }
                    routes.put(id, result);
                    cachedRouteVertices += weight;
                }
            }
            return result;
        }
    }

    /**
     * A computed search bound to its originating topology lease. Creating paths
     * from it never searches again, even when the snapshot's LRU evicts the source.
     * Callers should retain only their bounded in-progress routing working set.
     */
    public static final class RouteIndex implements RouteCosts {
        private final Snapshot snapshot;
        private final ConductorGraph.Routes routes;

        private RouteIndex(Snapshot snapshot, ConductorGraph.Routes routes) {
            this.snapshot = snapshot;
            this.routes = routes;
        }

        @Override
        public boolean reaches(int receiver) { snapshot.active(); return routes.reaches(receiver); }

        @Override
        public long lossMilliTo(int receiver) { snapshot.active(); return routes.lossMilliTo(receiver); }

        public Path path(Position receiver) { return path(snapshot.vertex(receiver)); }

        public Path path(int receiver) {
            snapshot.active();
            if (!routes.reaches(receiver)) throw new IllegalArgumentException("Unreachable conductor");
            return new Path(snapshot, routes, receiver);
        }
    }

    /** Immutable selected route. Every visit rechecks the snapshot lease. */
    public static final class Path {
        private final Snapshot snapshot;
        private final ConductorGraph.Routes routes;
        private final int receiver;
        private Path(Snapshot snapshot, ConductorGraph.Routes routes, int receiver) {
            this.snapshot = snapshot; this.routes = routes; this.receiver = receiver;
        }
        public long lossMilli() { snapshot.active(); return routes.lossMilliTo(receiver); }
        /** Exact visit callback count, available before admitting effect traversal. */
        public int vertexCount() { snapshot.active(); return routes.vertexCountTo(receiver); }
        public void visit(Consumer<Position> visitor) {
            Objects.requireNonNull(visitor, "visitor"); snapshot.active();
            routes.visitPath(receiver, id -> visitor.accept(snapshot.position(id)));
        }
    }
}
