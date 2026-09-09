package com.singularity_iteration.mio_icif.energy.grid;

import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;

/**
 * A tile that conducts energy (wire/cable).
 */
public interface IEnergyConductor extends IEnergyAcceptor, IEnergyEmitter {
    /**
     * @return The energy loss per conducted packet (in EU).
     */
    double getConductionLoss();

    /**
     * @return The maximum energy absorption of the insulation before it starts taking damage.
     */
    double getInsulationEnergyAbsorption();

    /**
     * @return The energy threshold at which insulation is destroyed.
     */
    double getInsulationBreakdownEnergy();

    /**
     * @return The energy threshold at which the conductor itself is destroyed.
     */
    double getConductorBreakdownEnergy();

    /**
     * Called when insulation should be removed (e.g. from overload).
     */
    void removeInsulation();

    void removeConductor();

    default void onEnergyPass() {}

    CableTier getCableTier();
}