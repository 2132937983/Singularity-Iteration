// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Immutable undirected conductor graph, including cycles and disconnected parts.
 * An independently chosen minimum-loss path model, not a claim about a game's
 * internal algorithm or which equal-cost physical path it selects.
 *
 * <h2>Chain compression</h2>
 * Real cable networks are mostly long runs with few branches. Every vertex whose
 * degree is not 2 is a <em>key</em> vertex (junctions, dead ends, isolated
 * conductors); each pure degree-2 cycle promotes one member to key. The remaining
 * vertices lie on exactly one <em>chain</em>: a maximal run of degree-2 vertices
 * between two key vertices (possibly the same key, for a loop). A chain with no
 * interior is a plain key-to-key link.
 *
 * <p>Shortest paths are searched over key vertices only, with each chain acting as
 * one weighted edge. The cost of an interior vertex follows in O(1) from its
 * chain's two end costs and a prefix sum. Search work and retained route memory
 * are therefore proportional to the number of key vertices in a component
 * ({@link #searchWeight(int)}), not to its cable length. The costs are identical
 * to a full per-vertex search; when equal-cost paths exist, the path picked may
 * differ.
 */
public final class ConductorGraph {
    private final long[] losses;
    private final int[] offsets;
    private final int[] neighbours;
    private final int[] components;
    private final int[] componentSizes;

    // --- chain compression -------------------------------------------------
    /** vertex -> component-local key index, or -1 for chain interiors. */
    private final int[] keyLocal;
    /** component label -> dense component ordinal (labels are seed vertex IDs). */
    private final int[] componentOrdinal;
    /** ordinal -> [start, end) into keyVertices. */
    private final int[] componentKeyStart;
    private final int[] keyVertices;
    /** interior vertex -> chain, and its index along that chain (A to B). */
    private final int[] chainOf;
    private final int[] chainIndex;
    /** chain -> end keys (global vertex IDs) and its interior slice. */
    private final int[] chainA;
    private final int[] chainB;
    private final int[] chainStart;
    private final int[] chainInterior;
    /** prefix[chainPrefixStart[c] + i] = loss of the first i interior vertices. */
    private final int[] chainPrefixStart;
    private final long[] chainPrefix;
    /** key vertex -> arcs; arc = chain * 2 + direction (0: leave via A, 1: leave via B). */
    private final int[] arcOffsets;
    private final int[] arcs;

    public ConductorGraph(long[] conductorLossMilli, int[][] links) {
        Objects.requireNonNull(conductorLossMilli, "conductorLossMilli");
        Objects.requireNonNull(links, "links");
        int count = conductorLossMilli.length;
        if (count == 0 || count == Integer.MAX_VALUE || links.length > (Integer.MAX_VALUE - 1) / 2) {
            throw new IllegalArgumentException("Graph dimensions outside representable range");
        }
        losses = conductorLossMilli.clone();
        for (long loss : losses) {
            if (loss < 0) { throw new IllegalArgumentException("Negative conductor loss"); }
        }
        offsets = new int[count + 1];
        int[] ends = new int[links.length * 2];
        var unique = new HashSet<Long>();
        for (int i = 0; i < links.length; i++) {
            int[] link = Objects.requireNonNull(links[i], "link");
            if (link.length != 2) { throw new IllegalArgumentException("Link needs two endpoints"); }
            int a = link[0]; int b = link[1];
            check(a, count); check(b, count);
            long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
            if (a == b || !unique.add(key)) { throw new IllegalArgumentException("Self or duplicate link"); }
            ends[2 * i] = a; ends[2 * i + 1] = b;
            offsets[a + 1]++; offsets[b + 1]++;
        }
        for (int i = 1; i <= count; i++) { offsets[i] += offsets[i - 1]; }
        neighbours = new int[ends.length];
        int[] cursor = offsets.clone();
        for (int i = 0; i < ends.length; i += 2) {
            neighbours[cursor[ends[i]]++] = ends[i + 1];
            neighbours[cursor[ends[i + 1]]++] = ends[i];
        }

        // Label physical wire components once (label = lowest vertex ID of the
        // component). Endpoint machines are not wire vertices and therefore
        // cannot accidentally merge distinct domains.
        components = new int[count]; Arrays.fill(components, -1);
        componentOrdinal = new int[count]; Arrays.fill(componentOrdinal, -1);
        int[] queue = new int[count]; int componentCount = 0;
        for (int seed = 0; seed < count; seed++) {
            if (components[seed] >= 0) continue;
            componentOrdinal[seed] = componentCount++;
            int head = 0, tail = 0; queue[tail++] = seed; components[seed] = seed;
            while (head < tail) {
                int vertex = queue[head++];
                for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                    int next = neighbours[i];
                    if (components[next] >= 0) continue;
                    components[next] = seed; queue[tail++] = next;
                }
            }
        }
        componentSizes = new int[componentCount];
        for (int vertex = 0; vertex < count; vertex++) componentSizes[componentOrdinal[components[vertex]]]++;

        // Key vertices: degree != 2, plus one member of every pure degree-2 cycle.
        boolean[] key = new boolean[count];
        for (int v = 0; v < count; v++) key[v] = degree(v) != 2;
        boolean[] walked = new boolean[count];
        for (int start = 0; start < count; start++) {
            if (key[start] || walked[start]) continue;
            // Walk one direction; reaching a key or an already walked vertex means
            // this run ends at a key. Returning to the start means a pure cycle.
            int previous = start, vertex = start;
            walked[start] = true;
            while (true) {
                int next = neighbours[offsets[vertex]] != previous ? neighbours[offsets[vertex]] : neighbours[offsets[vertex] + 1];
                if (next == start) { key[start] = true; break; }
                if (key[next] || walked[next]) break;
                walked[next] = true; previous = vertex; vertex = next;
            }
        }

        // Dense per-component key numbering, ascending by vertex ID within a component.
        componentKeyStart = new int[componentCount + 1];
        for (int v = 0; v < count; v++) if (key[v]) componentKeyStart[componentOrdinal[components[v]] + 1]++;
        for (int i = 1; i <= componentCount; i++) componentKeyStart[i] += componentKeyStart[i - 1];
        keyVertices = new int[componentKeyStart[componentCount]];
        keyLocal = new int[count]; Arrays.fill(keyLocal, -1);
        int[] fill = Arrays.copyOf(componentKeyStart, componentCount);
        for (int v = 0; v < count; v++) {
            if (!key[v]) continue;
            int ordinal = componentOrdinal[components[v]];
            keyLocal[v] = fill[ordinal] - componentKeyStart[ordinal];
            keyVertices[fill[ordinal]++] = v;
        }

        // Chains. Each is discovered once: from its lower-ID end key, or for a
        // direct key-key link only when a < b. Interiors are claimed on the walk.
        chainOf = new int[count]; Arrays.fill(chainOf, -1);
        chainIndex = new int[count];
        int chainCount = 0, interiorCount = 0;
        int[] tmpA = new int[Math.max(1, links.length)], tmpB = new int[Math.max(1, links.length)];
        int[] tmpStart = new int[Math.max(1, links.length) + 1];
        int[] tmpInterior = new int[count];
        for (int a = 0; a < count; a++) {
            if (!key[a]) continue;
            for (int i = offsets[a]; i < offsets[a + 1]; i++) {
                int first = neighbours[i];
                if (key[first]) {
                    if (a < first) { tmpA[chainCount] = a; tmpB[chainCount] = first; tmpStart[++chainCount] = interiorCount; }
                    continue;
                }
                if (chainOf[first] >= 0) continue; // claimed from the other end
                int previous = a, vertex = first, index = 0;
                while (!key[vertex]) {
                    chainOf[vertex] = chainCount; chainIndex[vertex] = index++;
                    tmpInterior[interiorCount++] = vertex;
                    int next = neighbours[offsets[vertex]] != previous ? neighbours[offsets[vertex]] : neighbours[offsets[vertex] + 1];
                    // A two-vertex loop through the same key (a-x-a) is impossible in a simple graph,
                    // but a longer loop returns to 'a' here and ends the chain correctly.
                    previous = vertex; vertex = next;
                }
                tmpA[chainCount] = a; tmpB[chainCount] = vertex; tmpStart[++chainCount] = interiorCount;
            }
        }
        chainA = Arrays.copyOf(tmpA, chainCount);
        chainB = Arrays.copyOf(tmpB, chainCount);
        chainStart = Arrays.copyOf(tmpStart, chainCount + 1);
        chainInterior = Arrays.copyOf(tmpInterior, interiorCount);
        chainPrefixStart = new int[chainCount + 1];
        for (int c = 0; c < chainCount; c++) chainPrefixStart[c + 1] = chainPrefixStart[c] + (chainStart[c + 1] - chainStart[c]) + 1;
        chainPrefix = new long[chainPrefixStart[chainCount]];
        for (int c = 0; c < chainCount; c++) {
            int base = chainPrefixStart[c]; long sum = 0;
            for (int i = chainStart[c]; i < chainStart[c + 1]; i++) {
                sum = saturatedAdd(sum, losses[chainInterior[i]]);
                chainPrefix[base + (i - chainStart[c]) + 1] = sum;
            }
        }
        // Arcs per key vertex (a loop chain contributes both directions to the same key).
        arcOffsets = new int[count + 1];
        for (int c = 0; c < chainCount; c++) { arcOffsets[chainA[c] + 1]++; arcOffsets[chainB[c] + 1]++; }
        for (int i = 1; i <= count; i++) arcOffsets[i] += arcOffsets[i - 1];
        arcs = new int[arcOffsets[count]];
        int[] arcCursor = Arrays.copyOf(arcOffsets, count);
        for (int c = 0; c < chainCount; c++) {
            arcs[arcCursor[chainA[c]]++] = 2 * c;     // leave A, travel toward B
            arcs[arcCursor[chainB[c]]++] = 2 * c + 1; // leave B, travel toward A
        }
    }

    private int degree(int vertex) { return offsets[vertex + 1] - offsets[vertex]; }

    /** Overflowing sums saturate; a saturated cost can never be selected as reachable. */
    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }

    /** Stable snapshot-local ID of the physical conductor component. */
    public int componentOf(int vertex) { check(vertex, losses.length); return components[vertex]; }

    /** Number of physical conductors in this vertex's component (path/effect bound). */
    public int componentSize(int vertex) {
        check(vertex, losses.length);
        return componentSizes[componentOrdinal[components[vertex]]];
    }

    /**
     * Search work and retained memory of one {@link #routesFrom} call: the number of
     * key vertices in the component. Always at least 1 and at most componentSize.
     */
    public int searchWeight(int vertex) {
        check(vertex, losses.length);
        int ordinal = componentOrdinal[components[vertex]];
        return Math.max(1, componentKeyStart[ordinal + 1] - componentKeyStart[ordinal]);
    }

    private long interiorLoss(int chain) { return chainPrefix[chainPrefixStart[chain + 1] - 1]; }
    private int interiorLength(int chain) { return chainStart[chain + 1] - chainStart[chain]; }
    /** Loss of interior vertices [from, to) of a chain, counted from its A end. */
    private long interiorRange(int chain, int from, int to) {
        int base = chainPrefixStart[chain];
        return chainPrefix[base + to] - chainPrefix[base + from];
    }

    /** Minimum-loss routes from one conductor over its component. */
    public Routes routesFrom(int sourceContact) {
        check(sourceContact, losses.length);
        int ordinal = componentOrdinal[components[sourceContact]];
        int keyBase = componentKeyStart[ordinal];
        int keyCount = componentKeyStart[ordinal + 1] - keyBase;
        long[] costs = new long[keyCount];
        int[] parents = new int[keyCount];
        int[] counts = new int[keyCount];
        byte[] roots = new byte[keyCount];
        boolean[] known = new boolean[keyCount];
        boolean[] settled = new boolean[keyCount];
        var heap = new VertexHeap(costs);
        int sourceChain = keyLocal[sourceContact] >= 0 ? -1 : chainOf[sourceContact];
        int sourceIndex = sourceChain < 0 ? -1 : chainIndex[sourceContact];
        if (sourceChain < 0) {
            int local = keyLocal[sourceContact];
            costs[local] = losses[sourceContact]; parents[local] = Routes.FROM_SOURCE; counts[local] = 1;
            known[local] = true; heap.update(local);
        } else {
            // Leave the source's chain toward each end key.
            int length = interiorLength(sourceChain);
            int a = keyLocal[chainA[sourceChain]], b = keyLocal[chainB[sourceChain]];
            long towardA = saturatedAdd(interiorRange(sourceChain, 0, sourceIndex + 1), losses[chainA[sourceChain]]);
            long towardB = saturatedAdd(interiorRange(sourceChain, sourceIndex, length), losses[chainB[sourceChain]]);
            seed(heap, costs, parents, counts, roots, known, a, towardA, Routes.SEED_A, sourceIndex + 2);
            seed(heap, costs, parents, counts, roots, known, b, towardB, Routes.SEED_B, length - sourceIndex + 1);
        }
        while (!heap.empty()) {
            int local = heap.take();
            settled[local] = true;
            if (costs[local] == Long.MAX_VALUE) throw new ArithmeticException("A reachable route cost cannot fit in long");
            int vertex = keyVertices[keyBase + local];
            for (int i = arcOffsets[vertex]; i < arcOffsets[vertex + 1]; i++) {
                int arc = arcs[i], chain = arc >>> 1;
                int far = (arc & 1) == 0 ? chainB[chain] : chainA[chain];
                int next = keyLocal[far];
                if (settled[next]) continue;
                long proposed = saturatedAdd(saturatedAdd(costs[local], interiorLoss(chain)), losses[far]);
                if (!known[next] || proposed < costs[next]) {
                    costs[next] = proposed; parents[next] = arc;
                    counts[next] = counts[local] + interiorLength(chain) + 1;
                    roots[next] = roots[local];
                    known[next] = true; heap.update(next);
                }
            }
        }
        return new Routes(this, components[sourceContact], keyBase, costs, parents, counts, roots, settled,
            sourceContact, sourceChain, sourceIndex);
    }

    private static void seed(VertexHeap heap, long[] costs, int[] parents, int[] counts, byte[] roots, boolean[] known,
                             int local, long cost, int parent, int count) {
        if (known[local] && costs[local] <= cost) return;
        costs[local] = cost; parents[local] = parent; counts[local] = count; known[local] = true;
        roots[local] = (byte) (parent == Routes.SEED_A ? 1 : 2);
        heap.update(local);
    }

    private static void check(int vertex, int count) {
        if (vertex < 0 || vertex >= count) { throw new IllegalArgumentException("Invalid conductor ID"); }
    }

    /** No world references; callers replace the index when its topology changes. */
    public static final class Routes implements RouteCosts {
        static final int FROM_SOURCE = -1, SEED_A = -2, SEED_B = -3;
        private static final int VIA_DIRECT = 0, VIA_A = 1, VIA_B = 2;

        private final ConductorGraph graph;
        private final int component, keyBase;
        private final long[] costs;
        private final int[] parents, counts;
        /** Seed side (1 = left source toward A, 2 = toward B) each key's route started from; 0 for a key source. */
        private final byte[] roots;
        private final boolean[] settled;
        private final int source, sourceChain, sourceIndex;

        private Routes(ConductorGraph graph, int component, int keyBase, long[] costs, int[] parents, int[] counts,
                       byte[] roots, boolean[] settled, int source, int sourceChain, int sourceIndex) {
            this.graph = graph; this.component = component; this.keyBase = keyBase;
            this.costs = costs; this.parents = parents; this.counts = counts; this.roots = roots; this.settled = settled;
            this.source = source; this.sourceChain = sourceChain; this.sourceIndex = sourceIndex;
        }

        /** Chosen access to an interior target: which side, and its cost/count. */
        private long bestCost; private int bestCount, bestVia;

        private boolean resolve(int target) {
            check(target, graph.losses.length);
            if (graph.components[target] != component) return false;
            int local = graph.keyLocal[target];
            if (local >= 0) {
                if (!settled[local]) return false;
                bestCost = costs[local]; bestCount = counts[local]; bestVia = -1;
                return true;
            }
            int chain = graph.chainOf[target], index = graph.chainIndex[target], length = graph.interiorLength(chain);
            boolean found = false;
            if (chain == sourceChain) {
                int from = Math.min(index, sourceIndex), to = Math.max(index, sourceIndex) + 1;
                bestCost = graph.interiorRange(chain, from, to); bestCount = to - from; bestVia = VIA_DIRECT; found = true;
            }
            // Enter the chain only from an end whose own route does not already run
            // along it; such an entry is never cheaper and would repeat vertices.
            int a = graph.keyLocal[graph.chainA[chain]];
            if (settled[a] && entersFresh(a, chain, index, true)) {
                long cost = saturatedAdd(costs[a], graph.interiorRange(chain, 0, index + 1));
                if (!found || cost < bestCost) { bestCost = cost; bestCount = counts[a] + index + 1; bestVia = VIA_A; found = true; }
            }
            int b = graph.keyLocal[graph.chainB[chain]];
            if (settled[b] && entersFresh(b, chain, index, false)) {
                long cost = saturatedAdd(costs[b], graph.interiorRange(chain, index, length));
                if (!found || cost < bestCost) { bestCost = cost; bestCount = counts[b] + length - index; bestVia = VIA_B; found = true; }
            }
            if (found && bestCost == Long.MAX_VALUE) throw new ArithmeticException("A reachable route cost cannot fit in long");
            return found;
        }

        private boolean entersFresh(int local, int chain, int index, boolean fromA) {
            if (chain == sourceChain) {
                // Every route here starts on this chain: from A only if it left the
                // source toward B and the target lies on A's side, and vice versa.
                return fromA ? roots[local] == 2 && index < sourceIndex : roots[local] == 1 && index > sourceIndex;
            }
            int parent = parents[local];
            return parent < 0 || (parent >>> 1) != chain;
        }

        @Override
        public boolean reaches(int receiverContact) { return resolve(receiverContact); }

        @Override
        public long lossMilliTo(int receiverContact) {
            if (!resolve(receiverContact)) { throw new IllegalStateException("Unreachable contact"); }
            return bestCost;
        }

        /** Exact visitPath callback count, including both source and receiver. */
        public int vertexCountTo(int receiverContact) {
            if (!resolve(receiverContact)) { throw new IllegalStateException("Unreachable contact"); }
            return bestCount;
        }

        /** Visits the chosen path from the receiver back to the source, both inclusive. */
        public void visitPath(int receiverContact, IntConsumer visitor) {
            Objects.requireNonNull(visitor, "visitor");
            if (!resolve(receiverContact)) { throw new IllegalStateException("Unreachable contact"); }
            var g = graph;
            int key;
            if (bestVia < 0) {
                key = receiverContact;
            } else {
                int chain = g.chainOf[receiverContact], index = g.chainIndex[receiverContact];
                int base = g.chainStart[chain];
                if (bestVia == VIA_DIRECT) {
                    int step = sourceIndex >= index ? 1 : -1;
                    for (int i = index; ; i += step) { visitor.accept(g.chainInterior[base + i]); if (i == sourceIndex) return; }
                }
                if (bestVia == VIA_A) { for (int i = index; i >= 0; i--) visitor.accept(g.chainInterior[base + i]); key = g.chainA[chain]; }
                else { for (int i = index; i < g.interiorLength(chain); i++) visitor.accept(g.chainInterior[base + i]); key = g.chainB[chain]; }
            }
            while (true) {
                visitor.accept(key);
                int parent = parents[g.keyLocal[key]];
                if (parent == FROM_SOURCE) return;
                if (parent == SEED_A || parent == SEED_B) {
                    int base = g.chainStart[sourceChain];
                    if (parent == SEED_A) for (int i = 0; i <= sourceIndex; i++) visitor.accept(g.chainInterior[base + i]);
                    else for (int i = g.interiorLength(sourceChain) - 1; i >= sourceIndex; i--) visitor.accept(g.chainInterior[base + i]);
                    return;
                }
                int chain = parent >>> 1, base = g.chainStart[chain], length = g.interiorLength(chain);
                if ((parent & 1) == 0) { // arrived at B from A: walk interior B->A, then A
                    for (int i = length - 1; i >= 0; i--) visitor.accept(g.chainInterior[base + i]);
                    key = g.chainA[chain];
                } else {                 // arrived at A from B
                    for (int i = 0; i < length; i++) visitor.accept(g.chainInterior[base + i]);
                    key = g.chainB[chain];
                }
            }
        }
    }

    /** Indexed binary heap avoids duplicate/stale queue entries after decreases. */
    private static final class VertexHeap {
        private final int[] nodes;
        private final int[] positions;
        private final long[] costs;
        private int size;

        private VertexHeap(long[] costs) {
            this.costs = costs; nodes = new int[costs.length]; positions = new int[costs.length];
            Arrays.fill(positions, -1);
        }

        private boolean empty() { return size == 0; }

        private boolean less(int a, int b) {
            return costs[a] < costs[b] || (costs[a] == costs[b] && a < b);
        }

        private void swap(int a, int b) {
            int old = nodes[a]; nodes[a] = nodes[b]; nodes[b] = old;
            positions[nodes[a]] = a; positions[nodes[b]] = b;
        }

        private void update(int vertex) {
            int at = positions[vertex];
            if (at < 0) { at = size++; nodes[at] = vertex; positions[vertex] = at; }
            while (at > 0) {
                int parent = (at - 1) / 2;
                if (!less(nodes[at], nodes[parent])) { break; }
                swap(at, parent); at = parent;
            }
        }

        private int take() {
            int result = nodes[0];
            positions[result] = -1;
            if (--size > 0) {
                nodes[0] = nodes[size]; positions[nodes[0]] = 0;
                int at = 0;
                while (at <= (size - 2) / 2 && size > 1) {
                    int child = 2 * at + 1;
                    if (child >= size) { break; }
                    if (child + 1 < size && less(nodes[child + 1], nodes[child])) { child++; }
                    if (!less(nodes[child], nodes[at])) { break; }
                    swap(at, child); at = child;
                }
            }
            return result;
        }
    }
}
