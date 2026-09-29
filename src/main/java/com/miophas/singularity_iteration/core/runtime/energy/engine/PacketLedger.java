// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;

/**
 * Pure accounting for one finite source and one accepting receiver joined by a
 * uniform line. The caller supplies all numerical policy; there are no game IDs,
 * registries, platform interfaces, world updates or legacy implementation hooks.
 *
 * <p>This independently authored model covers only the observations in the
 * accompanying fixtures. It describes the settled balance, not tick scheduling.
 * Branches, mixed conductors, receiver damage and non-integral energy are outside
 * its scope. A plan must not be applied to a changing world without revalidation.
 */
public final class PacketLedger {
    private PacketLedger() { }

    /** Immutable homogeneous path; losses use thousandths of an energy unit. */
    public record Line(int conductors, long lossMilliPerConductor, long safeDebit) {
        public Line {
            if (conductors < 0 || lossMilliPerConductor < 0 || safeDebit <= 0) {
                throw new IllegalArgumentException("Invalid line parameters");
            }
            // Reject unrepresentable loss instead of silently wrapping it.
            Math.multiplyExact(conductors, lossMilliPerConductor);
        }

        public long wholeLoss() {
            return Math.multiplyExact(conductors, lossMilliPerConductor) / 1000;
        }
    }

    /** All conductors share the same fate in this uniform-path model. */
    public record Plan(long sourceDebit, long receiverCredit, long dissipated,
                       long transfers, boolean lineFused) { }

    /**
     * Computes the final balances in constant time and space, even for a very
     * large reserve. Each transfer requires a complete offered packet in reserve,
     * but a nearly full receiver can cause a smaller debit. The path's integral
     * loss is charged once per actual transfer. A fuse ends the sequence after
     * the transfer that exceeded the caller's safe-debit bound.
     */
    public static Plan settle(long reserve, long packet, long receiverRoom, Line line) {
        Objects.requireNonNull(line, "line");
        if (reserve < 0 || packet <= 0 || receiverRoom < 0) {
            throw new IllegalArgumentException("Invalid energy parameters");
        }
        long loss = line.wholeLoss();
        if (loss >= packet) {
            throw new IllegalArgumentException("Loss at or above packet size is outside the observed scope");
        }
        if (reserve < packet || receiverRoom == 0) {
            return new Plan(0, 0, 0, 0, false);
        }

        long deliveredPerFullTransfer = packet - loss;
        long completeTransfers = Math.min(reserve / packet, receiverRoom / deliveredPerFullTransfer);
        boolean completeTransferFuses = line.conductors() > 0 && packet > line.safeDebit();
        if (completeTransfers > 0 && completeTransferFuses) {
            return new Plan(packet, deliveredPerFullTransfer, loss, 1, true);
        }

        // Both products are bounded by their corresponding input balances.
        long debit = completeTransfers * packet;
        long credit = completeTransfers * deliveredPerFullTransfer;
        long transfers = completeTransfers;
        boolean fused = false;
        long remainingRoom = receiverRoom - credit;
        if (remainingRoom > 0 && reserve - debit >= packet) {
            // A positive remainder here is strictly smaller than one delivery.
            long partialDebit = remainingRoom + loss;
            debit += partialDebit;
            credit += remainingRoom;
            transfers++;
            fused = line.conductors() > 0 && partialDebit > line.safeDebit();
        }
        return new Plan(debit, credit, debit - credit, transfers, fused);
    }
}
