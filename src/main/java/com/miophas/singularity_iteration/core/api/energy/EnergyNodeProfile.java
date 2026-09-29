package com.miophas.singularity_iteration.core.api.energy;

/**
 * Immutable network settings. A negative loss/fuse value means that no override is supplied.
 *
 * <p>{@code dynamicTier} marks a generator whose per-packet voltage is not the static
 * {@code outputPacket} but is derived from the live source tier, mirroring IC2's
 * {@code TileEntityConversionGenerator}: the engine uses
 * {@code min(currentOffer, getPowerFromTier(getSourceTier()))} so the melt threshold follows
 * the machine's instantaneous output instead of a fixed table entry.
 */
public record EnergyNodeProfile(Role role, long outputPacket, long conductorLossMilliEu,
        long fuseLimit, boolean standardProcessor, boolean specialCable, boolean measuredBlast,
        boolean dynamicTier) {
    public enum Role { CONSUMER, GENERATOR, STORAGE, CONDUCTOR }

    public EnergyNodeProfile {
        java.util.Objects.requireNonNull(role, "role");
        if (outputPacket < 0 || conductorLossMilliEu < -1 || fuseLimit < -1)
            throw new IllegalArgumentException("Invalid energy node profile");
        if (role == Role.CONDUCTOR && conductorLossMilliEu < 0)
            throw new IllegalArgumentException("Conductor loss is required");
        if (dynamicTier && role != Role.GENERATOR)
            throw new IllegalArgumentException("Only generators may declare a dynamic tier");
    }

    public static EnergyNodeProfile consumer() { return new EnergyNodeProfile(Role.CONSUMER, 0, -1, -1, false, false, false, false); }
    public static EnergyNodeProfile generator(long packet) { return generator(packet, false); }
    /** A generator whose packet tracks its live source tier instead of the static packet. */
    public static EnergyNodeProfile generator(long packet, boolean dynamicTier) { return new EnergyNodeProfile(Role.GENERATOR, packet, -1, -1, false, false, false, dynamicTier); }
    public static EnergyNodeProfile storage(long packet) { return new EnergyNodeProfile(Role.STORAGE, packet, -1, -1, false, false, false, false); }
}
