// SPDX-License-Identifier: Apache-2.0
// SCEX: independent model from public 0.7/1.6 upgrade factors and recorded game observations.
// See the clean-room dossier, ic2-overclock-03; no IC2 code or API is used.
package com.miophas.singularity_iteration.core.runtime.upgrade;

public record StandardProcessingTiming(int ticks, int operations, long energyPerTick) {
    public static int operations(double duration) {
        return Math.max(1, Math.min(64, (int) Math.ceil(1.0 / duration)));
    }

    public static int cycleTicks(int baseTicks, double timeFactor) {
        double duration = Math.max(1, baseTicks) * timeFactor;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(duration * operations(duration))));
    }

    public static StandardProcessingTiming calculate(int baseTicks, long baseEnergy, double timeFactor, double powerFactor) {
        double duration = Math.max(1, baseTicks) * timeFactor;
        // A bounded batch preserves ordinary 64-item inventory behavior at very high counts.
        int operations = operations(duration);
        int ticks = cycleTicks(baseTicks, timeFactor);
        long power = Math.max(1L, Math.round(baseEnergy * powerFactor));
        return new StandardProcessingTiming(ticks, operations, power);
    }

    public long bufferWithStorage(long baseCapacity, int storageUpgrades) {
        long upgrades = Math.max(0L, storageUpgrades) * MachineUpgradeStats.ENERGY_STORAGE_BONUS;
        long storage = baseCapacity > Long.MAX_VALUE - upgrades ? Long.MAX_VALUE : Math.max(0L, baseCapacity) + upgrades;
        if (energyPerTick > (Long.MAX_VALUE - storage) / ticks) return Long.MAX_VALUE;
        return energyPerTick * ticks + storage;
    }
}