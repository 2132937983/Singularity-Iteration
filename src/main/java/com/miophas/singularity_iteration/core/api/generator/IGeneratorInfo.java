package com.miophas.singularity_iteration.core.api.generator;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Generator information.
 *
 * <p>A read-only view of the runtime state of a generator.
 * Use it to query the generation rate, stored energy, efficiency and other information.
 */
public interface IGeneratorInfo {

    /**
     * Gets the world of the generator.
     */
    Level getWorld();

    /**
     * Gets the block position of the generator.
     */
    BlockPos getPos();

    /**
     * Gets the generator type ID.
     */
    String getTypeId();

    /**
     * Gets the generation rate of the generator, in EU/t.
     */
    long getGenerationRate();

    /**
     * Gets the energy that the generator stores now, in EU.
     */
    long getStoredEnergy();

    /**
     * Gets the maximum energy capacity of the generator, in EU.
     */
    long getMaxEnergy();

    /**
     * Gets the cable tier of the generator.
     */
    ICableTier getCableTier();

    /**
     * Checks if the generator is generating energy now.
     */
    boolean isActive();

    /**
     * Gets the energy output rate of the generator, in EU/t.
     */
    long getOutputRate();

    /**
     * Gets the efficiency of the generator (0 to 100).
     * Upgrades, fuel quality and other factors can make the real efficiency lower than the base efficiency.
     */
    double getEfficiency();

    /**
     * Gets the duration of the current run, in ticks.
     */
    long getRuntimeTicks();

    /**
     * Gets the total energy that the generator made, in EU.
     */
    long getTotalGenerated();
}