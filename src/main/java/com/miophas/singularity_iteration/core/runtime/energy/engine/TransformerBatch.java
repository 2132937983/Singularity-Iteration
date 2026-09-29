// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;

/** Transformer output batch, with receiver order and route losses explicitly supplied. */
public final class TransformerBatch {
    private TransformerBatch() { }

    public static final class Allocation {
        private final long debit;
        private final long[] credits;
        private final long dissipated;
        private final List<Delivery> deliveries;
        private Allocation(long debit, long[] credits, long dissipated, List<Delivery> deliveries) {
            this.debit = debit; this.credits = credits; this.dissipated = dissipated;
            this.deliveries = List.copyOf(deliveries);
        }
        public long debit() { return debit; }
        public long credit(int receiver) { return credits[receiver]; }
        public int receiverCount() { return credits.length; }
        public long dissipated() { return dissipated; }
        /** Numeric packet decomposition; not a claim about reference event ordering. */
        public List<Delivery> deliveries() { return deliveries; }
    }

    public record Delivery(int receiver, long sourceDebit, long credit, long pathLoss) {
        public Delivery {
            if (receiver < 0 || credit <= 0 || pathLoss < 0 || sourceDebit != Math.addExact(credit, pathLoss))
                throw new IllegalArgumentException("Invalid packet delivery");
        }
    }

    /**
     * Quotes whole output packets from the starting buffer. A demand no larger
     * than one nominal packet receives its exact amount. Each quoted packet
     * visits receivers in priority order; a hungry receiver can accept all
     * four step-down packets. Actual remaining capacity bounds every credit.
     * Partial consumption can leave a residual for a later receiver. Inputs are
     * not mutated. Caller owns topology, ordering, input/output scheduling and
     * commit; overload is not modeled.
     */
    public static Allocation allocate(TransformerAccounting.Configuration configuration,
                                      long buffer, long[] room, int[] priority) {
        Objects.requireNonNull(room, "room");
        return allocate(configuration, buffer, room, priority, new long[room.length]);
    }

    /** Whole-EU path loss is paid per positive packet, including a usable residual. */
    public static Allocation allocate(TransformerAccounting.Configuration configuration,
                                      long buffer, long[] room, int[] priority, long[] pathLoss) {
        Objects.requireNonNull(configuration, "configuration");
        if (buffer < 0 || buffer > configuration.capacity())
            throw new IllegalArgumentException("Invalid buffer");
        long packet = configuration.outputPacket();
        long budget = Math.min(buffer / packet, configuration.outputPackets()) * packet;
        return allocateQuoted(packet, budget, room, priority, pathLoss);
    }

    /** Continue a previously quoted batch across domains without rounding its residual again. */
    static Allocation allocateQuoted(long packet, long budget, long[] room, int[] priority, long[] pathLoss) {
        return allocateQuoted(packet, budget, room, priority, pathLoss, true);
    }

    static Allocation allocateQuoted(long packet, long budget, long[] room, int[] priority, long[] pathLoss, boolean trace) {
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(pathLoss, "pathLoss");
        int maximum = EnergyPacketPolicy.MAX_PACKETS_PER_ROUND;
        if (packet <= 0 || budget < 0 || budget / packet > maximum || budget / packet == maximum && budget % packet != 0
                || room.length != priority.length || room.length != pathLoss.length)
            throw new IllegalArgumentException("Invalid quoted budget or vector dimensions");
        boolean[] seen = new boolean[room.length];
        for (int id : priority) {
            if (id < 0 || id >= room.length || seen[id])
                throw new IllegalArgumentException("Priority must be a complete permutation");
            seen[id] = true;
        }
        for (long value : room) if (value < 0) throw new IllegalArgumentException("Negative demand");
        for (long value : pathLoss) if (value < 0) throw new IllegalArgumentException("Negative path loss");
        long remaining = budget;
        long[] credits = new long[room.length];
        long dissipated = 0;
        List<Delivery> deliveries = trace ? new ArrayList<>() : List.of();
        int packets = (int) (budget / packet + (budget % packet == 0 ? 0 : 1));
        for (int n = 0; n < packets && remaining > 0; n++) {
            long available = Math.min(packet, remaining), before = available;
            for (int id : priority) {
                long loss = pathLoss[id], demand = room[id] - credits[id];
                if (demand == 0 || available <= loss) continue;
                long credit = Math.min(demand, available - loss), debit = credit + loss;
                if (trace) deliveries.add(new Delivery(id, debit, credit, loss));
                credits[id] = Math.addExact(credits[id], credit);
                available -= debit;
                remaining -= debit;
                dissipated = Math.addExact(dissipated, loss);
            }
            if (available == before) break;
        }
        return new Allocation(budget - remaining, credits, dissipated, deliveries);
    }
}
