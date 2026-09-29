// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/** Public offer/debit contract implemented by the admitted SI generators. */
public interface IEnergySource extends IEnergyEmitter {
    double getOfferedEnergy();
    void drawEnergy(double amount);
    int getSourceTier();

    /**
     * Owned native sources may offer multiple packets per settlement (0 disables).
     * The runtime bounds legacy implementations to 64 packets. Explicit
     * IEnergyPacketSource policy takes precedence; existing sources default to one.
     */
    default int getPacketCount() { return 1; }
}
