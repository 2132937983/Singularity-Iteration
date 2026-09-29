// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.OptionalLong;

/**
 * Independent integer-energy model for one packet on a uniform straight line.
 * Effect thresholds are explicit inputs. Mixed paths, branching, shock damage,
 * explosion strength/geometry and timing are outside this model's evidence.
 */
public final class UniformPacketEffects {
    private UniformPacketEffects() { }

    /** A destroyed receiver's retained energy is not observable and is not zero. */
    public record Outcome(long sourceDebit, OptionalLong retainedReceiverCredit,
                          long pathLoss, boolean lineFused, boolean receiverDestroyed) { }

    public static Outcome evaluate(long reserve, long packet, long receiverRoom,
                                   PacketLedger.Line line, long receiverSafePacket) {
        if (receiverSafePacket <= 0) { throw new IllegalArgumentException("Positive receiver limit required"); }
        // Limit this call to one complete offer, regardless of the total reserve.
        var account = PacketLedger.settle(Math.min(reserve, packet), packet, receiverRoom, line);
        boolean atConductorBoundary = line.conductors() > 0 && account.sourceDebit() == line.safeDebit();
        // These conditions are an independent model to be checked against frozen
        // outcomes. They do not describe inspected target code or event ordering.
        boolean destroyed = !account.lineFused() && account.sourceDebit() > receiverSafePacket
                && (account.receiverCredit() > receiverSafePacket || atConductorBoundary);
        return new Outcome(account.sourceDebit(), destroyed ? OptionalLong.empty() : OptionalLong.of(account.receiverCredit()),
                account.dissipated(), account.lineFused(), destroyed);
    }
}
