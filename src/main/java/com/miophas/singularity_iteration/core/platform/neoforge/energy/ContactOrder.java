// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Independent publication-history journal. It supplies a tie order, never an
 * energy route or a world reference. The finite R19/R25 construction observations
 * support this component model and a limited early-contact alternative. The
 * optional independent seed supplies a stable construction choice; it does not
 * identify the reference implementation, its PRNG or an exact probability.
 */
public final class ContactOrder implements AutoCloseable {
    private static final long MAX_CACHED_POSITIONS = 3_200_000;
    private static final int[][] SIDES = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    private final Thread owner = Thread.currentThread();
    private final int limit, cacheLimit;
    private final boolean alternatives;
    private final long constructionSeed;
    // A negative value denotes an endpoint. Conductors retain fractional loss.
    private record Entry(long loss, long order, int faces) { }
    private final LinkedHashMap<Position, Entry> journal = new LinkedHashMap<>();
    private final Map<Position, Long> reservations = new HashMap<>();
    private final LinkedHashMap<Query, List<Position>> cache = new LinkedHashMap<>(16, .75f, true);
    private long cachedPositions, traces, cacheHits;
    private Graph graph;
    private boolean closed;
    private long rebuilds, nextOrder;

    public ContactOrder(int limit, int cacheLimit) {
        this(limit, cacheLimit, false, 0);
    }
    public ContactOrder(int limit, int cacheLimit, long constructionSeed) {
        this(limit, cacheLimit, true, constructionSeed);
    }
    private ContactOrder(int limit, int cacheLimit, boolean alternatives, long constructionSeed) {
        if (limit <= 0 || cacheLimit <= 0) throw new IllegalArgumentException("Positive journal/cache limits required");
        this.limit = limit; this.cacheLimit = cacheLimit;
        this.alternatives = alternatives; this.constructionSeed = constructionSeed;
    }
    private void active() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Closed or wrong journal thread");
    }
    public void putConductor(Position at, long lossMilli) { putConductor(at, lossMilli, 63); }
    public void putConductor(Position at, long lossMilli, int openFaces) {
        if (lossMilli < 0) throw new IllegalArgumentException("Negative conductor loss");
        if ((openFaces & ~63) != 0) throw new IllegalArgumentException("Invalid conductor face mask");
        put(at, lossMilli, openFaces);
    }
    public void putEndpoint(Position at) { put(at, -1, 63); }
    /** Record an actual placement without activating a route or reading the world. */
    public void reservePlacement(Position at) {
        active(); Objects.requireNonNull(at);
        if (!reservations.containsKey(at) && reservations.size() >= limit) throw new IllegalStateException("Placement reservation limit reached");
        reservations.put(at, nextOrder = Math.incrementExact(nextOrder));
    }
    public void cancelPlacement(Position at) { active(); reservations.remove(Objects.requireNonNull(at)); }
    public void forgetPlacements(java.util.function.Predicate<Position> predicate) {
        active(); reservations.keySet().removeIf(Objects.requireNonNull(predicate));
    }
    private void put(Position at, long value, int faces) {
        active(); Objects.requireNonNull(at);
        Entry previous = journal.get(at); Long reserved = reservations.remove(at);
        if (previous != null && previous.loss == value && reserved == null) {
            if (previous.faces == faces) return;
            // A port edit is not a new placement and must not reroll construction history.
            journal.put(at, new Entry(value, previous.order, faces)); invalidate(); return;
        }
        if (previous == null && journal.size() >= limit) throw new IllegalStateException("Contact journal capacity reached");
        long order = reserved == null ? (nextOrder = Math.incrementExact(nextOrder)) : reserved.longValue();
        journal.remove(at); journal.put(at, new Entry(value, order, faces)); invalidate();
    }
    public void remove(Position at) {
        active(); Objects.requireNonNull(at);
        if (journal.remove(at) != null) invalidate();
    }
    private void invalidate() { graph = null; cache.clear(); cachedPositions = 0; }
    public int size() { active(); return journal.size(); }
    public long rebuildCount() { active(); return rebuilds; }
    public record CacheMetrics(int queries, long positions, long traces, long hits) { }
    public CacheMetrics cacheMetrics() { active(); return new CacheMetrics(cache.size(), cachedPositions, traces, cacheHits); }

    /** Empty means this source/contact is absent or outside the supported history model. */
    public List<Position> receivers(Position source, Position contact) {
        active(); Objects.requireNonNull(source); Objects.requireNonNull(contact);
        if (graph == null) { graph = new Graph(journal, alternatives, constructionSeed); rebuilds = Math.incrementExact(rebuilds); }
        Node wire = graph.wires.get(contact);
        if (wire == null) return List.of();
        Component component = wire.component.root();
        Query key = new Query(source, component);
        List<Position> known = cache.get(key);
        if (known != null) { cacheHits++; return known; }
        Node start = component.endpoints.get(source);
        traces++;
        List<Position> result = start == null || start.root().sharedAtPlacement ? List.of() : trace(start.root());
        // Bound both query count and retained receiver references. A modest
        // query count alone causes every source in a larger grid to miss on
        // every tick, while an unweighted cache can retain quadratic memory.
        if (result.size() > MAX_CACHED_POSITIONS) return result;
        while (!cache.isEmpty() && (cache.size() >= cacheLimit || cachedPositions + result.size() > MAX_CACHED_POSITIONS)) {
            var oldest = cache.entrySet().iterator();
            cachedPositions -= oldest.next().getValue().size(); oldest.remove();
        }
        cachedPositions += result.size();
        cache.put(key, result); return result;
    }
    private static List<Position> trace(Node source) {
        var distances = new HashMap<Node, Long>(); var done = new HashSet<Node>();
        var queue = new PriorityQueue<Visit>(Comparator.comparingLong(Visit::distance));
        var result = new ArrayList<Position>();
        distances.put(source, 0L); queue.add(new Visit(source, 0));
        while (!queue.isEmpty()) {
            Visit visit = queue.remove(); Node node = visit.node;
            if (done.contains(node) || visit.distance != distances.get(node)) continue;
            done.add(node);
            if (node != source && node.loss < 0) { result.add(node.at); continue; }
            for (Node raw : node.links) {
                Node next = raw.root();
                long candidate = Math.addExact(visit.distance, Math.addExact(Math.max(0, node.loss), Math.max(0, next.loss)));
                if (candidate < distances.getOrDefault(next, Long.MAX_VALUE)) {
                    distances.put(next, candidate); queue.add(new Visit(next, candidate));
                }
            }
        }
        return List.copyOf(result);
    }
    private record Visit(Node node, long distance) { }
    private record Query(Position source, Component component) { }
    private static final class Node {
        final Position at; final long loss, birth; final int faces;
        final ArrayList<Node> links = new ArrayList<>();
        Node alias; Component component; boolean sharedAtPlacement;
        Node(Position at, long loss, long birth, int faces) { this.at = at; this.loss = loss; this.birth = birth; this.faces = faces; }
        Node root() {
            Node result = this; while (result.alias != null) result = result.alias;
            Node current = this;
            while (current.alias != null) { Node next = current.alias; current.alias = result; current = next; }
            return result;
        }
    }
    private static final class Component {
        Component parent;
        int size;
        final Map<Position, Node> endpoints = new HashMap<>();
        Component root() {
            Component result = this; while (result.parent != null) result = result.parent;
            Component current = this;
            while (current.parent != null) { Component next = current.parent; current.parent = result; current = next; }
            return result;
        }
        Node endpoint(Position at, long birth) { return endpoints.computeIfAbsent(at, p -> new Node(p, -1, birth, 63)).root(); }
    }
    private static void link(Node a, Node b) {
        a = a.root(); b = b.root();
        if (!a.links.contains(b)) a.links.add(b);
        if (!b.links.contains(a)) b.links.add(a);
    }
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
    private static boolean reverseEarly(Node first, Node second, long seed) {
        // Independent one-in-four choice supported only by the frozen coarse
        // R25 frequency screens. Keyed by construction identity, so rebuilding
        // a query cache or editing an unrelated component cannot reroll it.
        long identity = mix(first.birth) ^ Long.rotateLeft(mix(second.birth), 17);
        identity ^= mix(first.at.x()) ^ Long.rotateLeft(mix(first.at.y()), 21) ^ Long.rotateLeft(mix(first.at.z()), 42);
        return (mix(seed ^ identity) & 3) == 0;
    }
    private static Component merge(Component first, Component second, boolean alternatives, long seed) {
        first = first.root(); second = second.root();
        if (first == second) return first;
        // Union by size bounds map movement. The ordered endpoint links still
        // preserve the first encountered component's history, independently of
        // which allocation becomes the storage root.
        Component big = first.size >= second.size ? first : second;
        Component small = big == first ? second : first;
        for (var entry : small.endpoints.entrySet()) {
            Node other = big.endpoints.get(entry.getKey());
            if (other == null) { big.endpoints.put(entry.getKey(), entry.getValue()); continue; }
            Node a = (small == first ? entry.getValue() : other).root();
            Node b = (small == first ? other : entry.getValue()).root();
            if (a != b) {
                if (alternatives && first.size < second.size && a.birth < b.birth
                        && a.links.size() + b.links.size() == 3 && reverseEarly(a, b, seed)) {
                    java.util.Collections.reverse(b.links);
                }
                a.links.addAll(b.links); a.sharedAtPlacement |= b.sharedAtPlacement; b.alias = a;
            }
            big.endpoints.put(entry.getKey(), a);
        }
        big.size = Math.addExact(big.size, small.size); small.parent = big; small.endpoints.clear();
        return big;
    }
    private static final class Graph {
        final Map<Position, Node> wires = new HashMap<>();
        Graph(LinkedHashMap<Position, Entry> journal, boolean alternatives, long seed) {
            var placedEndpoints = new HashSet<Position>();
            var ordered = new ArrayList<>(journal.entrySet());
            ordered.sort(Comparator.comparingLong(entry -> entry.getValue().order));
            for (var entry : ordered) {
                Position at = entry.getKey(); long loss = entry.getValue().loss; int faces = entry.getValue().faces;
                var nearWires = new ArrayList<Node>(6); var nearEndpoints = new ArrayList<Position>(6);
                var groups = new ArrayList<Component>(6);
                for (int face = 0; face < SIDES.length; face++) {
                    int[] side = SIDES[face];
                    if ((faces & (1 << face)) == 0) continue;
                    long x=(long)at.x()+side[0], y=(long)at.y()+side[1], z=(long)at.z()+side[2];
                    if (x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || y<Integer.MIN_VALUE || y>Integer.MAX_VALUE || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE) continue;
                    Position near = new Position((int)x,(int)y,(int)z); Node wire = wires.get(near);
                    if (wire != null && (wire.faces & (1 << (face ^ 1))) != 0) {
                        nearWires.add(wire); Component group = wire.component.root();
                        if (!groups.contains(group)) groups.add(group);
                    } else if (wire == null && placedEndpoints.contains(near)) nearEndpoints.add(near);
                }
                if (loss >= 0) {
                    Component target = groups.isEmpty() ? new Component() : groups.getFirst();
                    for (Component group : groups) target = merge(target, group, alternatives, seed);
                    Node node = new Node(at, loss, entry.getValue().order, faces); node.component = target; target.size++; wires.put(at,node);
                    // Preserve the six-neighbour interleaving of wire and endpoint links.
                    for (int face = 0; face < SIDES.length; face++) {
                    int[] side = SIDES[face];
                    if ((faces & (1 << face)) == 0) continue;
                        long x=(long)at.x()+side[0], y=(long)at.y()+side[1], z=(long)at.z()+side[2];
                        if (x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || y<Integer.MIN_VALUE || y>Integer.MAX_VALUE || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE) continue;
                        Position near = new Position((int)x,(int)y,(int)z); Node wire = wires.get(near);
                        if (wire != null && (wire.faces & (1 << (face ^ 1))) != 0) link(node,wire);
                        else if (nearEndpoints.contains(near)) link(node,target.endpoint(near, entry.getValue().order));
                    }
                } else {
                    placedEndpoints.add(at);
                    for (Component group : groups) {
                        Node endpoint = group.endpoint(at, entry.getValue().order); int contacts = 0;
                        for (Node wire : nearWires) if (wire.component.root() == group) { link(endpoint,wire); contacts++; }
                        endpoint.sharedAtPlacement |= contacts > 1;
                    }
                }
            }
        }
    }
    @Override public void close() { active(); journal.clear(); reservations.clear(); invalidate(); closed = true; }
}
