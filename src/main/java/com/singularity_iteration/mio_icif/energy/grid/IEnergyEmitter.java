package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.Direction;

/**
 * A tile that can emit energy to adjacent acceptors.
 */
public interface IEnergyEmitter extends IEnergyTile {
    boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction);

    /** Convenience method - emits energy to an IEnergyTile target (checks if it's an acceptor). */
    default boolean emitsToEnergyAcceptor(IEnergyTile target, Direction dir) {
        if (target instanceof IEnergyAcceptor) {
            return emitsEnergyTo((IEnergyAcceptor) target, dir);
        }
        return false;
    }
}


