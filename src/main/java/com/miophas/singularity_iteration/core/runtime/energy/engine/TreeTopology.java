// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Immutable, platform-independent snapshot of a connected acyclic conductor
 * network. Vertex IDs are caller-assigned dense integers, not game registries.
 * This class computes unique tree paths; it makes no receiver scheduling choice.
 */
public final class TreeTopology {
    private final long[] lossMilli;
    private final int[] offsets;
    private final int[] neighbours;

    /** Copies inputs and rejects disconnected graphs, cycles and invalid IDs. */
    public TreeTopology(long[] conductorLossMilli, int[][] links) {
        Objects.requireNonNull(conductorLossMilli, "conductorLossMilli");
        Objects.requireNonNull(links, "links");
        int count = conductorLossMilli.length;
        if (count == 0 || links.length != count - 1 || count > (Integer.MAX_VALUE - 1) / 2) {
            throw new IllegalArgumentException("A tree needs at least one vertex and exactly V-1 edges");
        }
        lossMilli = conductorLossMilli.clone();
        for (long loss : lossMilli) {
            if (loss < 0) {
                throw new IllegalArgumentException("Negative conductor loss");
            }
        }
        // Compact adjacency uses O(V) storage without a collection per vertex.
        offsets = new int[count + 1];
        int[] ends = new int[links.length * 2];
        for (int i = 0; i < links.length; i++) {
            int[] link = Objects.requireNonNull(links[i], "link");
            if (link.length != 2) {
                throw new IllegalArgumentException("Each link has two endpoints");
            }
            int a = link[0];
            int b = link[1];
            checkVertex(a, count);
            checkVertex(b, count);
            if (a == b) {
                throw new IllegalArgumentException("Self link");
            }
            ends[2 * i] = a;
            ends[2 * i + 1] = b;
            offsets[a + 1]++;
            offsets[b + 1]++;
        }
        for (int i = 1; i <= count; i++) {
            offsets[i] += offsets[i - 1];
        }
        neighbours = new int[ends.length];
        int[] cursor = offsets.clone();
        for (int i = 0; i < ends.length; i += 2) {
            neighbours[cursor[ends[i]]++] = ends[i + 1];
            neighbours[cursor[ends[i + 1]]++] = ends[i];
        }
        // With exactly V-1 edges, connectivity also proves acyclicity and no
        // duplicate edge. Iteration avoids stack overflow on long cable chains.
        int[] queue = new int[count];
        boolean[] seen = new boolean[count];
        seen[0] = true;
        int reached = 1;
        for (int head = 0; head < reached; head++) {
            int vertex = queue[head];
            for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                int next = neighbours[i];
                if (!seen[next]) {
                    seen[next] = true;
                    queue[reached++] = next;
                }
            }
        }
        if (reached != count) {
            throw new IllegalArgumentException("Disconnected or cyclic network");
        }
    }

    public int conductorCount() {
        return lossMilli.length;
    }

    /** One O(V) traversal creates an index reusable for all receiver contacts. */
    public Routes routesFrom(int sourceContact) {
        checkVertex(sourceContact, lossMilli.length);
        int[] parents = new int[lossMilli.length];
        int[] counts = new int[lossMilli.length];
        long[] totals = new long[lossMilli.length];
        int[] queue = new int[lossMilli.length];
        queue[0] = sourceContact;
        parents[sourceContact] = -1;
        counts[sourceContact] = 1;
        totals[sourceContact] = lossMilli[sourceContact];
        int tail = 1;
        for (int head = 0; head < tail; head++) {
            int vertex = queue[head];
            for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                int next = neighbours[i];
                if (next != parents[vertex]) {
                    parents[next] = vertex;
                    counts[next] = counts[vertex] + 1;
                    totals[next] = Math.addExact(totals[vertex], lossMilli[next]);
                    queue[tail++] = next;
                }
            }
        }
        return new Routes(parents, counts, totals);
    }

    private static void checkVertex(int vertex, int count) {
        if (vertex < 0 || vertex >= count) {
            throw new IllegalArgumentException("Conductor ID outside snapshot");
        }
    }

    /** Path queries allocate nothing and retain no world or block entity. */
    public static final class Routes implements RouteCosts {
        private final int[] parents;
        private final int[] counts;
        private final long[] totals;

        private Routes(int[] parents, int[] counts, long[] totals) {
            this.parents = parents;
            this.counts = counts;
            this.totals = totals;
        }

        @Override
        public boolean reaches(int receiverContact) {
            checkVertex(receiverContact, parents.length);
            return true;
        }

        public long lossMilliTo(int receiverContact) {
            checkVertex(receiverContact, parents.length);
            return totals[receiverContact];
        }

        public long wholeLossTo(int receiverContact) {
            return lossMilliTo(receiverContact) / 1000;
        }

        public int conductorsTo(int receiverContact) {
            checkVertex(receiverContact, parents.length);
            return counts[receiverContact];
        }

        /** Visits the contact-to-source path without allocating a path list. */
        public void visitPath(int receiverContact, IntConsumer visitor) {
            checkVertex(receiverContact, parents.length);
            Objects.requireNonNull(visitor, "visitor");
            for (int vertex = receiverContact; vertex != -1; vertex = parents[vertex]) {
                visitor.accept(vertex);
            }
        }
    }
}
