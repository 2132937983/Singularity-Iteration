// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

/**
 * SI-owned EU storage surface implemented by CustomEUEnergyStorage.
 * Transfers return the amount accepted or removed; simulation does not mutate storage.
 * Internal work uses useEnergy/generateEnergy rather than external transfer limits.
 */
public interface IEnergyStorageAccess {
    long getAmount();
    long getCapacity();
    long useEnergy(long amount, boolean simulate);
    long generateEnergy(long amount, boolean simulate);
    long getMaxReceive();
    long getMaxExtract();
    ICableTier getCableTier();
    boolean isPowerSource();
    long getPowerOutput();
    boolean isOutputEnabled();
    long getPowerRating();
    boolean isOverloaded(long gridPower);

    /** Saturated integer view for SI menus; the underlying EU balance stays a long. */
    default int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, getAmount()));
    }

    default int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, getCapacity()));
    }
}
