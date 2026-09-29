// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/** Declared cable behavior supplied by the admitted SI wire implementation. */
public interface IEnergyConductor extends IEnergyEmitter, IEnergyAcceptor {
    double getConductionLoss();
    double getInsulationEnergyAbsorption();
    double getInsulationBreakdownEnergy();
    double getConductorBreakdownEnergy();
    void removeInsulation();
    void removeConductor();
    void onEnergyPass();
}
