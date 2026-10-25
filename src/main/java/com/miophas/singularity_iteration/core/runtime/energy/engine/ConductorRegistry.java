// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.ArrayDeque;
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
 *
 * <h2>0.1.10.20: persistent connected components (grid zones, G1)</h2>
 * <p>Before 0.1.10.20 a snapshot was one graph of every conductor of the dimension. Any edit, anywhere,
 * sorted and linked all conductors again (O(N log N)) and dropped every cached route search, so one cable
 * placed in one base re-planned every grid of the dimension. Now the registry keeps one graph per connected
 * component. An edit marks the edited position and its six neighbours; the next {@link #snapshot()} rebuilds
 * only the components that contain a marked position, and the other components keep their graphs and their
 * cached route searches. A snapshot is a composite view of the components:
 * <ul>
 *   <li>Vertex IDs: the components are ordered by their smallest position; a component's vertices are its
 *       positions in ascending order, numbered from the offset of the component. Thus the vertices of one
 *       component keep the relative order of the old global numbering, and route searches choose the same
 *       paths (including equal-cost ties) as before.</li>
 *   <li>{@link Snapshot#componentOf}: the lowest vertex ID of the component (as before).</li>
 *   <li>The route cache is one LRU over all components with the old limits (sources and route vertices).</li>
 *   <li>A snapshot still becomes invalid with any edit; a route search bound to it still checks the lease.</li>
 * </ul>
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

    // ---------------------------------------------------------------- 0.1.10.20: components

    /** One connected component: its positions (ascending), their local IDs, face masks and the graph. */
    private static final class Component {
        final Position[] positions;
        final HashMap<Position, Integer> local;
        final int[] faces;
        final ConductorGraph graph;
        /** Local source IDs with a cached route search. */
        final HashSet<Integer> cachedSources = new HashSet<>();
        /** Vertex ID of {@code positions[0]} in the current snapshot. */
        int offset;
        boolean live = true;

        Component(Position[] positions, HashMap<Position, Integer> local, int[] faces, ConductorGraph graph) {
            this.positions = positions; this.local = local; this.faces = faces; this.graph = graph;
        }
    }

    /** A cached route search: the component (by identity) and the local source ID. */
    private record RouteKey(Component component, int source) { }

    /** Active conductors (registered, chunk available) in a built component. */
    private final HashMap<Position, Component> componentAt = new HashMap<>();
    /** Live components. */
    private final HashSet<Component> components = new HashSet<>();
    /** Positions whose component must be rebuilt (edited positions and their neighbours). */
    private final HashSet<Position> dirty = new HashSet<>();
    /** One LRU of route searches over all components (the old per-snapshot limits). */
    private final LinkedHashMap<RouteKey, ConductorGraph.Routes> routeCache = new LinkedHashMap<>(16, .75f, true);
    private long cachedRouteVertices;
    /** Components rebuilt / kept by the last {@link #snapshot()} rebuild (diagnostics and tests). */
    private int lastRebuiltComponents, lastKeptComponents;
    /** Components rebuilt by all rebuilds of this registry. */
    private long rebuiltComponentsTotal;

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

    /** Marks a position and its six neighbours: their components are rebuilt by the next snapshot. */
    private void touch(Position at) {
        dirty.add(at);
        for (int face = 0; face < 6; face++) {
            Position next = adjacent(at, face);
            if (next != null) dirty.add(next);
        }
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
        touch(at);
        return true;
    }

    public boolean remove(Position at) {
        active(); Objects.requireNonNull(at, "at");
        if (!conductors.containsKey(at)) { return false; }
        changed(); conductors.remove(at);
        long key = chunkKey(at); var members = chunks.get(key); members.remove(at);
        if (members.isEmpty()) { chunks.remove(key); inactiveChunks.remove(key); }
        touch(at);
        return true;
    }

    /** Work is proportional to registered nodes in that chunk, not the world. */
    public int unloadChunk(int chunkX, int chunkZ) {
        active(); long key = chunkKey(chunkX, chunkZ); var members = chunks.get(key);
        if (members == null) { return 0; }
        changed();
        for (Position at : members) { conductors.remove(at); touch(at); }
        chunks.remove(key); inactiveChunks.remove(key);
        return members.size();
    }

    /** Keep registration history while excluding unavailable chunks from every route lease. */
    public boolean setChunkActive(int chunkX, int chunkZ, boolean available) {
        active(); long key = chunkKey(chunkX, chunkZ);
        if (!chunks.containsKey(key) || inactiveChunks.contains(key) == !available) return false;
        changed();
        if (available) inactiveChunks.remove(key); else inactiveChunks.add(key);
        for (Position at : chunks.get(key)) touch(at);
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
    /** 0.1.10.20: components that the last snapshot rebuild built again and kept (diagnostics, tests). */
    public int lastRebuiltComponents() { active(); return lastRebuiltComponents; }
    public int lastKeptComponents() { active(); return lastKeptComponents; }
    public long rebuiltComponentsTotal() { active(); return rebuiltComponentsTotal; }

    private boolean available(Position at) {
        return conductors.containsKey(at) && !inactiveChunks.contains(chunkKey(at));
    }

    /** Both conductors are available and open towards each other (the link rule of the old global build). */
    private boolean linked(Position from, int face, Position to) {
        if (to == null || !available(to)) return false;
        return (conductors.get(from).faces & (1 << face)) != 0 && (conductors.get(to).faces & (1 << (face ^ 1))) != 0;
    }

    /**
     * Coalesces any number of edits into one lazily rebuilt immutable topology. 0.1.10.20: only the
     * components that contain an edited position or a neighbour of one are built again.
     */
    public Snapshot snapshot() {
        active();
        if (cached != null) { return cached; }
        long nextRebuild = Math.incrementExact(rebuilds);
        rebuildDirty();
        Component[] ordered = components.toArray(new Component[0]);
        Arrays.sort(ordered, (a, b) -> a.positions[0].compareTo(b.positions[0]));
        int[] offsets = new int[ordered.length];
        int total = 0;
        for (int i = 0; i < ordered.length; i++) {
            ordered[i].offset = total;
            offsets[i] = total;
            total = Math.addExact(total, ordered[i].positions.length);
        }
        cached = new Snapshot(this, owner, revision, ordered, offsets, total);
        rebuilds = nextRebuild;
        return cached;
    }

    /** Rebuilds the components of the dirty positions; the other components stay as they are. */
    private void rebuildDirty() {
        int kept = components.size();
        if (dirty.isEmpty()) { lastRebuiltComponents = 0; lastKeptComponents = kept; return; }
        ArrayDeque<Position> work = new ArrayDeque<>();
        for (Position at : dirty) {
            dissolve(componentAt.get(at), work);
            if (available(at)) work.add(at);
        }
        dirty.clear();
        kept = components.size();
        HashSet<Position> assigned = new HashSet<>();
        int rebuilt = 0;
        while (!work.isEmpty()) {
            Position start = work.poll();
            if (assigned.contains(start) || !available(start)) continue;
            // a breadth-first search over the link rule; a live component it meets is merged
            ArrayList<Position> group = new ArrayList<>();
            ArrayDeque<Position> queue = new ArrayDeque<>();
            queue.add(start); assigned.add(start);
            while (!queue.isEmpty()) {
                Position at = queue.poll();
                group.add(at);
                for (int face = 0; face < 6; face++) {
                    Position next = adjacent(at, face);
                    if (!linked(at, face, next) || assigned.contains(next)) continue;
                    Component other = componentAt.get(next);
                    if (other != null) { dissolve(other, work); kept--; }
                    assigned.add(next); queue.add(next);
                }
            }
            build(group);
            rebuilt++;
        }
        lastRebuiltComponents = rebuilt;
        lastKeptComponents = kept;
        rebuiltComponentsTotal += rebuilt;
    }

    /** Removes a live component; its positions go to {@code work} and its cached route searches are released. */
    private void dissolve(Component component, ArrayDeque<Position> work) {
        if (component == null || !component.live) return;
        component.live = false;
        components.remove(component);
        for (Position at : component.positions) {
            if (componentAt.get(at) == component) componentAt.remove(at);
            work.add(at);
        }
        for (int source : component.cachedSources) {
            if (routeCache.remove(new RouteKey(component, source)) != null) cachedRouteVertices -= component.graph.searchWeight(source);
        }
        component.cachedSources.clear();
    }

    private void build(ArrayList<Position> group) {
        Position[] positions = group.toArray(new Position[0]);
        Arrays.sort(positions);
        var local = new HashMap<Position, Integer>(positions.length * 2);
        long[] losses = new long[positions.length];
        int[] faces = new int[positions.length];
        for (int i = 0; i < positions.length; i++) {
            local.put(positions[i], i);
            Conductor conductor = conductors.get(positions[i]);
            losses[i] = conductor.loss; faces[i] = conductor.faces;
        }
        var links = new ArrayList<int[]>();
        for (int i = 0; i < positions.length; i++) {
            Position at = positions[i];
            // +X, +Y and +Z once per pair, in the order of the old global build
            link(links, local, faces, i, 5, at.x < Integer.MAX_VALUE ? new Position(at.x + 1, at.y, at.z) : null);
            link(links, local, faces, i, 1, at.y < Integer.MAX_VALUE ? new Position(at.x, at.y + 1, at.z) : null);
            link(links, local, faces, i, 3, at.z < Integer.MAX_VALUE ? new Position(at.x, at.y, at.z + 1) : null);
        }
        var component = new Component(positions, local, faces, new ConductorGraph(losses, links.toArray(int[][]::new)));
        components.add(component);
        for (Position at : positions) componentAt.put(at, component);
    }

    private static void link(ArrayList<int[]> links, Map<Position, Integer> ids, int[] faces, int from, int face, Position to) {
        Integer target = to == null ? null : ids.get(to);
        if (target != null && (faces[from] & (1 << face)) != 0 && (faces[target] & (1 << (face ^ 1))) != 0) {
            links.add(new int[]{from, target});
        }
    }

    private static Position adjacent(Position at, int face) {
        long x = at.x, y = at.y, z = at.z;
        switch (face) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> throw new IllegalArgumentException("Invalid face");
        }
        return x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE
            || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE ? null : new Position((int) x, (int) y, (int) z);
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
        conductors.clear(); chunks.clear(); inactiveChunks.clear();
        componentAt.clear(); components.clear(); dirty.clear(); routeCache.clear(); cachedRouteVertices = 0;
        closed = true;
    }

    /** Cached search of {@code source} in {@code component}, or a new one within the budget (null when it does not fit). */
    private ConductorGraph.Routes routes(Snapshot snapshot, Component component, int source, SearchBudget budget) {
        var key = new RouteKey(component, source);
        var result = routeCache.get(key);
        if (result != null) return result;
        // Chain-compressed search: work and route arrays scale with key vertices.
        int weight = component.graph.searchWeight(source);
        if (budget != null && !budget.tryConsume(weight)) return null;
        result = component.graph.routesFrom(source);
        snapshot.routeComputations++;
        if (weight <= maximumCachedRouteVertices) {
            while (!routeCache.isEmpty() && (routeCache.size() >= maximumCachedSources
                    || cachedRouteVertices > maximumCachedRouteVertices - weight)) {
                var iterator = routeCache.entrySet().iterator();
                var evicted = iterator.next().getKey();
                iterator.remove();
                evicted.component().cachedSources.remove(evicted.source());
                cachedRouteVertices -= evicted.component().graph.searchWeight(evicted.source());
            }
            routeCache.put(key, result);
            component.cachedSources.add(source);
            cachedRouteVertices += weight;
        }
        return result;
    }

    /**
     * A topology lease. Edits/close reject further lease access. Previously returned pure route values remain
     * old calculations; their continued existence never authorizes applying them to a world. 0.1.10.20: a
     * composite view of the registry's components; it reads the registry's component index, which no edit can
     * change while the lease is valid (every edit invalidates the lease first).
     */
    public static final class Snapshot {
        private final ConductorRegistry registry;
        private final Thread owner;
        private final long revision;
        private final Component[] ordered;
        private final int[] offsets;
        private final int size;
        private long routeComputations;
        private boolean valid = true;

        private Snapshot(ConductorRegistry registry, Thread owner, long revision, Component[] ordered, int[] offsets, int size) {
            this.registry = registry; this.owner = owner; this.revision = revision; this.ordered = ordered; this.offsets = offsets;
            this.size = size;
        }

        private void active() {
            if (Thread.currentThread() != owner || !valid) { throw new IllegalStateException("Stale or foreign-thread snapshot"); }
        }
        private void invalidate() { valid = false; }
        public long revision() { active(); return revision; }
        public int size() { active(); return size; }
        public boolean contains(Position at) { active(); return registry.componentAt.containsKey(Objects.requireNonNull(at, "at")); }

        private Component component(Position at) {
            active();
            Component result = registry.componentAt.get(Objects.requireNonNull(at, "at"));
            if (result == null) { throw new IllegalArgumentException("Unregistered conductor position"); }
            return result;
        }

        private Component componentOfVertex(int vertex) {
            active();
            if (vertex < 0 || vertex >= size) throw new IllegalArgumentException("Unknown conductor ID");
            int index = Arrays.binarySearch(offsets, vertex);
            if (index < 0) index = -index - 2;
            return ordered[index];
        }

        public int vertex(Position at) {
            Component c = component(at);
            return c.offset + c.local.get(at);
        }
        /** A wire-to-machine contact must also be allowed by the wire's facing side. */
        public boolean permits(Position at, int face) { checkFace(face); return (openFaces(at) & (1 << face)) != 0; }
        public int openFaces(Position at) { Component c = component(at); return c.faces[c.local.get(at)]; }
        public int cachedSources() { active(); return registry.routeCache.size(); }
        public long cachedRouteVertices() { active(); return registry.cachedRouteVertices; }
        public long routeComputations() { active(); return routeComputations; }
        public Position position(int vertex) {
            Component c = componentOfVertex(vertex);
            return c.positions[vertex - c.offset];
        }
        /** A path tied to this lease; retaining it never authorizes a later world write. */
        public Path path(Position source, Position receiver) {
            var index = routesFrom(source); int target = vertex(receiver);
            if (!index.reaches(target)) throw new IllegalArgumentException("Unreachable conductor");
            return index.path(target);
        }
        /** The lowest vertex ID of the component (a label of the component within this snapshot). */
        public int componentOf(Position at) { return component(at).offset; }
        public int componentSize(Position at) { return component(at).positions.length; }
        /** Search work / retained route size for a source in this component (key vertices only). */
        public int searchWeight(Position at) { Component c = component(at); return c.graph.searchWeight(c.local.get(at)); }

        /** The route search of {@code source} without a budget (cached or computed now). */
        public RouteIndex routesFrom(Position source) {
            Component c = component(source);
            return new RouteIndex(this, c, registry.routes(this, c, c.local.get(source), null));
        }

        /**
         * Returns null if an uncached search cannot fit this tick's allowance.
         * Cached searches are free (0.1.10.20: also across snapshots, while the component is unchanged).
         * The allowance must cover the largest single component to guarantee eventual progress; searches
         * are not interruptible.
         */
        public RouteIndex routesFrom(Position source, SearchBudget budget) {
            Objects.requireNonNull(budget, "budget").ready();
            Component c = component(source);
            var result = registry.routes(this, c, c.local.get(source), budget);
            return result == null ? null : new RouteIndex(this, c, result);
        }
    }

    /**
     * A computed search bound to its originating topology lease. Creating paths
     * from it never searches again, even when the LRU evicts the source.
     * Callers should retain only their bounded in-progress routing working set.
     * Receiver arguments are vertex IDs of the snapshot; a receiver in another component is not reached.
     */
    public static final class RouteIndex implements RouteCosts {
        private final Snapshot snapshot;
        private final Component component;
        private final ConductorGraph.Routes routes;

        private RouteIndex(Snapshot snapshot, Component component, ConductorGraph.Routes routes) {
            this.snapshot = snapshot;
            this.component = component;
            this.routes = routes;
        }

        /** The local vertex of {@code receiver} in this component, or -1. */
        private int local(int receiver) {
            snapshot.active();
            int local = receiver - component.offset;
            return local >= 0 && local < component.positions.length ? local : -1;
        }

        @Override
        public boolean reaches(int receiver) { int local = local(receiver); return local >= 0 && routes.reaches(local); }

        @Override
        public long lossMilliTo(int receiver) {
            int local = local(receiver);
            if (local < 0) throw new IllegalStateException("Unreachable contact");
            return routes.lossMilliTo(local);
        }

        public Path path(Position receiver) { return path(snapshot.vertex(receiver)); }

        public Path path(int receiver) {
            int local = local(receiver);
            if (local < 0 || !routes.reaches(local)) throw new IllegalArgumentException("Unreachable conductor");
            return new Path(snapshot, component, routes, local, receiver);
        }
    }

    /** Immutable selected route. Every visit rechecks the snapshot lease. */
    public static final class Path {
        private final Snapshot snapshot;
        private final Component component;
        private final ConductorGraph.Routes routes;
        private final int local;
        private final int receiver;
        private Path(Snapshot snapshot, Component component, ConductorGraph.Routes routes, int local, int receiver) {
            this.snapshot = snapshot; this.component = component; this.routes = routes; this.local = local; this.receiver = receiver;
        }
        public long lossMilli() { snapshot.active(); return routes.lossMilliTo(local); }
        /** Exact visit callback count, available before admitting effect traversal. */
        public int vertexCount() { snapshot.active(); return routes.vertexCountTo(local); }
        public void visit(Consumer<Position> visitor) {
            Objects.requireNonNull(visitor, "visitor"); snapshot.active();
            routes.visitPath(local, id -> visitor.accept(component.positions[id]));
        }
        /**
         * 0.1.10.7: an identity of this path within its snapshot (the cached route search and the
         * receiver vertex). Two {@code Path} objects of the same search and receiver have equal keys,
         * so a caller can keep per-path data while the snapshot lives.
         */
        public Object key() { return new PathKey(routes, receiver); }
    }

    /** Route search (by identity: {@code Routes} has no equals) and receiver vertex of a path. */
    private record PathKey(ConductorGraph.Routes routes, int receiver) { }
}
