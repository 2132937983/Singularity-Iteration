package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.Direction;

/**
 * A tile that consumes energy from the grid.
 */
public interface IEnergySink extends IEnergyAcceptor {
    /**
     * @return The amount of energy this sink demands (can accept this tick).
     */
    double getDemandedEnergy();

    /**
     * @return The sink tier (maximum voltage tier accepted before overload).
     */
    int getSinkTier();

    /**
     * Inject energy into this sink.
     * @param direction From which direction energy is coming.
     * @param amount Amount of energy to inject.
     * @param voltage Voltage (tier as double).
     * @return The amount of energy NOT accepted (rejected).
     */
    double injectEnergy(Direction direction, double amount, double voltage);
}


