package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.Direction;

/**
 * A tile that can accept energy from adjacent emitters.
 */
public interface IEnergyAcceptor extends IEnergyTile {
    boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction);

    /** Convenience method - accepts energy from an IEnergyTile target (checks if it's an emitter). */
    default boolean acceptsEnergyFrom(IEnergyTile target, Direction dir) {
        if (target instanceof IEnergyEmitter) {
            return acceptsEnergyFrom((IEnergyEmitter) target, dir);
        }
        return false;
    }
}


