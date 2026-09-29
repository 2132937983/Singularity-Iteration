package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import java.util.Objects;

/** A source's initial offer for one settlement, shared by every receiver window. */
public final class PacketAllowance {
    private final EnergyPacketPolicy policy;
    private EnergyAmount remaining;

    public PacketAllowance(EnergyPacketPolicy policy, EnergyAmount initialOffer) {
        this.policy = Objects.requireNonNull(policy);
        remaining = policy.partialPackets() ? initialOffer.min(EnergyAmount.of(policy.maximumTransfer()))
            : EnergyAmount.of(policy.quote(initialOffer.whole()));
    }

    public EnergyAmount quote(EnergyPacketPolicy current, EnergyAmount liveOffer, int windowPackets) {
        if (!policy.equals(current)) return EnergyAmount.ZERO;
        long ceiling = new EnergyPacketPolicy(policy.packetSize(), Math.min(windowPackets, policy.packetCount()), true).maximumTransfer();
        return remaining.min(liveOffer).min(EnergyAmount.of(ceiling));
    }

    /** Only a successful storage commit can spend this budget. */
    public void debit(EnergyAmount amount) { remaining = remaining.subtract(amount); }
    public EnergyAmount remaining() { return remaining; }
}
