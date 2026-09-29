// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;

/**
 * Independent accounting for one storage/transformer/receiver path.
 * Order is supplied by a caller: this class neither guesses nor implements
 * the reference scheduler. Overload, topology changes and multiple ports are
 * deliberately outside this component's contract.
 */
public final class TransformerAccounting {
    private TransformerAccounting() { }

    public enum Order { INPUT_FIRST, OUTPUT_FIRST }

    public record Configuration(long lowPacket, boolean stepUp) {
        public Configuration {
            if (lowPacket <= 0 || lowPacket > Long.MAX_VALUE / 8)
                throw new IllegalArgumentException("Invalid transformer packet size");
        }
        public long capacity() { return lowPacket * 8; }
        public long inputLimit() { return stepUp ? lowPacket : lowPacket * 4; }
        public long outputPacket() { return stepUp ? lowPacket * 4 : lowPacket; }
        public int outputPackets() { return stepUp ? 1 : 4; }
    }

    /** Receiver energy can exceed nominal capacity after a multi-packet batch. */
    public record State(long source, long buffer, long receiver) {
        public State {
            if (source < 0 || buffer < 0 || receiver < 0)
                throw new IllegalArgumentException("Negative energy");
        }
    }

    public record Step(State after, long sourceDebit, long receiverCredit, long dissipated) {
        public Step(State after, long sourceDebit, long receiverCredit) {
            this(after, sourceDebit, receiverCredit, 0);
        }
    }

    /**
     * Output eligibility is quoted from the starting buffer. Receiver demand
     * up to one packet can terminate a batch with a partial packet; a larger
     * demand can accept several quoted packets before its next demand query.
     * This is the observed single-receiver batch boundary, not a general
     * multi-receiver routing policy.
     */
    public static Step advance(Configuration configuration, State before,
                               long sourcePacket, long receiverCapacity,
                               boolean receiverConnected, Order order) {
        return advance(configuration, before, sourcePacket, receiverCapacity, receiverConnected, order, 0);
    }

    /**
     * One output path with a nonnegative whole-EU loss per packet. Full batches
     * pay that loss for each packet; a demand ending within the first delivered
     * packet pays it once. Input-path losses are not represented here.
     */
    public static Step advance(Configuration configuration, State before,
                               long sourcePacket, long receiverCapacity,
                               boolean receiverConnected, Order order, long outputPathLoss) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(order, "order");
        if (sourcePacket <= 0 || sourcePacket > configuration.inputLimit())
            throw new IllegalArgumentException("Source outside nondestructive input range");
        if (receiverCapacity < 0 || before.buffer() > configuration.capacity() || outputPathLoss < 0)
            throw new IllegalArgumentException("Invalid capacity or buffer");

        long packet = configuration.outputPacket();
        long quotedPackets = Math.min(before.buffer() / packet, configuration.outputPackets());
        long demand = receiverConnected && before.receiver() < receiverCapacity
                ? receiverCapacity - before.receiver() : 0;
        long credit = 0, outputDebit = 0;
        if (quotedPackets != 0 && demand != 0 && outputPathLoss < packet) {
            if (demand <= packet - outputPathLoss) {
                credit = demand;
                outputDebit = demand + outputPathLoss;
            } else {
                outputDebit = quotedPackets * packet;
                credit = quotedPackets * (packet - outputPathLoss);
            }
        }

        long availableRoom = configuration.capacity() - before.buffer();
        if (order == Order.OUTPUT_FIRST) availableRoom += outputDebit;
        long debit = before.source() >= sourcePacket ? Math.min(sourcePacket, availableRoom) : 0;
        long buffer = Math.addExact(before.buffer() - outputDebit, debit);
        var after = new State(before.source() - debit, buffer,
                Math.addExact(before.receiver(), credit));
        if (buffer > configuration.capacity()) throw new IllegalStateException("Buffer overflow");
        return new Step(after, debit, credit, outputDebit - credit);
    }
}
