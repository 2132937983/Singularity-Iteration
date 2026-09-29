package com.miophas.singularity_iteration.core.api.energy;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Six-bit direction masks, in Direction ordinal order. Null sides query the aggregate port. */
public record EnergyPortPolicy(int inputs, int outputs, boolean generator) {
    public EnergyPortPolicy {
        if ((inputs & ~63) != 0 || (outputs & ~63) != 0) throw new IllegalArgumentException("Invalid energy faces");
    }

    public static EnergyPortPolicy consumer() { return new EnergyPortPolicy(63, 0, false); }
    public static EnergyPortPolicy source() { return new EnergyPortPolicy(0, 63, true); }
    public boolean accepts(@Nullable Direction side) { return (inputs & (side == null ? 63 : 1 << side.ordinal())) != 0; }
    public boolean emits(@Nullable Direction side) { return (outputs & (side == null ? 63 : 1 << side.ordinal())) != 0; }
}
