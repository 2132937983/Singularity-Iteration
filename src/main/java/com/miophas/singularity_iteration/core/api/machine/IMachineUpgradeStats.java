package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * API-level machine upgrade statistics.
 * Add-on developers use it so that they do not depend on the internal MachineUpgradeStats implementation.
 */
public interface IMachineUpgradeStats {
    
    /**
     * Gets the process time multiplier.
     * @return the process time multiplier (a lower value is faster)
     */
    double getProcessTimeMultiplier();
    
    /**
     * Gets the energy usage multiplier.
     * @return the energy usage multiplier
     */
    double getEnergyUsageMultiplier();
    
    /**
     * Gets the energy for each tick after the upgrades are applied.
     * @param baseEnergyPerTick base energy for each tick (EU/t)
     * @return actual energy for each tick (EU/t)
     */
    long getEnergyPerTick(long baseEnergyPerTick);
    
    /**
     * Gets the maximum progress after the upgrades are applied.
     * @param baseMaxProgress base maximum progress
     * @return actual maximum progress
     */
    int getMaxProgress(int baseMaxProgress);
    
    /**
     * Gets the process time after the upgrades are applied.
     * @param baseTicks base number of ticks
     * @return actual process time (ticks)
     */
    int getProcessTicks(int baseTicks);
    
    /**
     * Gets the energy capacity bonus from the upgrades.
     * @return the energy capacity bonus (EU)
     */
    long getEnergyCapacityBonus();

    /**
     * Gets the HU capacity after the energy storage upgrades are applied.
     *
     * @param baseCapacity base HU capacity
     * @return actual HU capacity
     */
    long getHeatCapacity(long baseCapacity);
    
    /**
     * Gets the effective cable tier after the transformer upgrades are applied.
     * @param baseTier base cable tier
     * @return effective cable tier
     */
    ICableTier getEffectiveCableTier(ICableTier baseTier);
    
    /**
     * Gets the number of overclocker upgrades.
     * @return the number of overclocker upgrades
     */
    int getOverclockerCount();
    
    /**
     * Gets the number of energy storage upgrades.
     * @return the number of energy storage upgrades
     */
    int getEnergyStorageCount();
    
    /**
     * Gets the number of transformer upgrades.
     * @return the number of transformer upgrades
     */
    int getTransformerCount();
    
    /**
     * Gets the number of ejector upgrades.
     * @return the number of ejector upgrades
     */
    int getEjectorCount();
    
    /**
     * Gets the number of pulling upgrades.
     * @return the number of pulling upgrades
     */
    int getPullingCount();
    
    /**
     * Gets the number of fluid ejector upgrades.
     * @return the number of fluid ejector upgrades
     */
    int getFluidEjectorCount();
    
    /**
     * Gets the number of fluid pulling upgrades.
     * @return the number of fluid pulling upgrades
     */
    int getFluidPullingCount();
    
    /**
     * Checks if the redstone signal is inverted.
     * @return true if it is inverted
     */
    boolean isRedstoneInverted();
    
    /**
     * Gets the ejector directions.
     * @return the list of directions
     */
    List<Direction> getEjectorDirections();
    
    /**
     * Gets the pulling directions.
     * @return the list of directions
     */
    List<Direction> getPullingDirections();
    
    /**
     * Gets the fluid ejector directions.
     * @return the list of directions
     */
    List<Direction> getFluidEjectorDirections();
    
    /**
     * Gets the fluid pulling directions.
     * @return the list of directions
     */
    List<Direction> getFluidPullingDirections();

    /** IC2 rate in mB/t for each adjacent side, for a stack of fluid upgrades: 50, 200, 800, 3200, 12800 (maximum). */
    static int fluidTransferPerStack(int count) {
        return count <= 0 ? 0 : 50 << (2 * Math.min(4, count - 1));
    }

    /** Ejection limit for each side, independent of the other sides. An implementation can keep different stack counts and directions for different upgrade slots. */
    default int getFluidEjectorTransferLimit(Direction side) {
        var directions = getFluidEjectorDirections();
        return directions.isEmpty() || directions.contains(side) ? fluidTransferPerStack(getFluidEjectorCount()) : 0;
    }

    /** Pull limit for each side, independent of the other sides. The six sides do not share one limit. */
    default int getFluidPullingTransferLimit(Direction side) {
        var directions = getFluidPullingDirections();
        return directions.isEmpty() || directions.contains(side) ? fluidTransferPerStack(getFluidPullingCount()) : 0;
    }
}
