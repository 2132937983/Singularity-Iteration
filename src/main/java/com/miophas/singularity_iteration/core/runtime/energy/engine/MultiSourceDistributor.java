// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.List;
import java.util.Objects;

/** One offered packet per source, sharing receiver demand quotes for a round. */
public final class MultiSourceDistributor {
    private MultiSourceDistributor() { }

    /** All receiver vectors use the same caller-assigned receiver IDs. */
    public static final class Offer {
        private final long reserve;
        private final long packet;
        private final RouteCosts routes;
        private final int[] contacts;
        private final int[] receiverPriority;

        public Offer(long reserve, long packet, RouteCosts routes, int[] contacts, int[] receiverPriority) {
            if (reserve < 0 || packet <= 0) { throw new IllegalArgumentException("Invalid source energy"); }
            this.reserve = reserve; this.packet = packet;
            this.routes = Objects.requireNonNull(routes, "routes");
            this.contacts = Objects.requireNonNull(contacts, "contacts").clone();
            this.receiverPriority = Objects.requireNonNull(receiverPriority, "receiverPriority").clone();
        }
    }

    /** Immutable receipts; negative remaining room reports observed-style overshoot. */
    public static final class Round {
        private final PacketDistributor.Allocation[] allocations;
        private final long[] remainingRoom;

        private Round(PacketDistributor.Allocation[] allocations, long[] remainingRoom) {
            this.allocations = allocations; this.remainingRoom = remainingRoom;
        }

        public PacketDistributor.Allocation source(int source) { return allocations[source]; }
        public int sourceCount() { return allocations.length; }
        public long remainingRoom(int receiver) { return remainingRoom[receiver]; }
        public long[] remainingRoom() { return remainingRoom.clone(); }
    }

    /**
     * Pure O(S*R) accounting in caller-supplied source and per-source receiver
     * orders within one conductor domain. A receiver closes when one source
     * satisfies its original quote; partial credits from different sources do
     * not accumulate towards this stop condition. R16's small-packet controls
     * distinguish this from the older aggregate-room stop condition. This can
     * overshoot capacity by more than one packet when many sources contribute.
     * Remaining room reports the actual total, independently of eligibility.
     * It is an independent model consistent with
     * black-box outcomes, not a description of inspected target internals.
     * Orders are explicit inputs, not an emulation of a game's random scheduler.
     * Callers must revalidate their complete world snapshot before applying this
     * result; this method neither mutates a world nor provides such a commit.
     */
    public static Round allocate(List<Offer> offers, long[] receiverRoom, int[] sourcePriority) {
        List<Offer> sources = List.copyOf(offers);
        long[] quoted = Objects.requireNonNull(receiverRoom, "receiverRoom").clone();
        long[] remaining = quoted.clone();
        boolean[] accepting = new boolean[quoted.length];
        int[] order = Objects.requireNonNull(sourcePriority, "sourcePriority").clone();
        if (order.length != sources.size()) { throw new IllegalArgumentException("Source order length"); }
        for (long room : remaining) { if (room < 0) { throw new IllegalArgumentException("Negative receiver room"); } }
        for (int i = 0; i < quoted.length; i++) accepting[i] = quoted[i] > 0;
        boolean[] seen = new boolean[sources.size()];
        for (int source : order) {
            if (source < 0 || source >= sources.size() || seen[source]) {
                throw new IllegalArgumentException("Source order must be a complete permutation");
            }
            seen[source] = true;
        }
        var receipts = new PacketDistributor.Allocation[sources.size()];
        for (int source : order) {
            Offer offer = sources.get(source);
            long[] activeQuotes = new long[remaining.length];
            for (int receiver = 0; receiver < remaining.length; receiver++) {
                if (accepting[receiver]) { activeQuotes[receiver] = quoted[receiver]; }
            }
            var receipt = PacketDistributor.allocate(offer.reserve, offer.packet, offer.routes,
                    offer.contacts, activeQuotes, offer.receiverPriority);
            receipts[source] = receipt;
            for (int receiver = 0; receiver < remaining.length; receiver++) {
                remaining[receiver] = Math.subtractExact(remaining[receiver], receipt.credit(receiver));
                if (receipt.credit(receiver) >= quoted[receiver]) accepting[receiver] = false;
            }
        }
        return new Round(receipts, remaining);
    }
}
