// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;

/** Server-owned source whose external fuel is acquired only after a routable electrical demand exists. */
public interface DemandEnergySource {
    CustomEUEnergyStorage ownedEnergy();
    int outputFaces();
    /** Simulation only; includes already owned EU. */
    EnergyAmount potentialEnergy();
    /** Acquire at most the fuel needed for this total EU offer; retain actual receipts in the one owned balance. */
    void prepareEnergy(EnergyAmount requested);
}
