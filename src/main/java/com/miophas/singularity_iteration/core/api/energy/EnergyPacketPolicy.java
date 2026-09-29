package com.miophas.singularity_iteration.core.api.energy;

/** Immutable native EU offer policy, separate from direction and storage/FE limits. */
public record EnergyPacketPolicy(long packetSize, int packetCount, boolean partialPackets) {
    /** Engineering work bound per source and settlement, not an IC2 voltage rule. */
    public static final int MAX_PACKETS_PER_ROUND = 64;

    public EnergyPacketPolicy {
        if (packetSize <= 0 || packetCount < 0 || packetCount > MAX_PACKETS_PER_ROUND)
            throw new IllegalArgumentException("Positive packet size and 0..64 packets required");
    }

    /** Saturating ceiling; never multiply a high tier into a negative allowance. */
    public long maximumTransfer() {
        if (packetCount == 0) return 0;
        return packetSize > Long.MAX_VALUE / packetCount ? Long.MAX_VALUE : packetSize * packetCount;
    }

    /** Whole-EU offer; exact fractional offers are handled by the runtime ledger. */
    public long quote(long available) {
        if (available < 0) throw new IllegalArgumentException("Negative source balance");
        return partialPackets ? Math.min(available, maximumTransfer())
            : Math.min(available / packetSize, packetCount) * packetSize;
    }
}
