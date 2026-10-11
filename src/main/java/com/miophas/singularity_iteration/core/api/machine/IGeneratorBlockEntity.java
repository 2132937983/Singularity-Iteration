package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;

/**
 * Interface for generator block entities.
 * All generator types (EU/HU/KU) must implement this interface.
 *
 * <p>This interface extends {@link com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock}.
 * It gives the cable tier query with the return type {@link CableTier}.
 * New code should use {@link com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock}.
 */
public interface IGeneratorBlockEntity extends com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock {

    /**
     * Gets the cable tier (as the internal type).
     *
     * @return the cable tier
     */
    @Override
    CableTier getCableTier();

    /**
     * Checks if the generator is working.
     * @return true if the generator makes energy now
     */
    @Override
    boolean isGenerating();

    /**
     * Gets the generation rate.
     * @return the amount generated per tick
     */
    @Override
    long getGenerationRate();

    /**
     * Gets the generator type ID.
     * @return the type ID
     */
    @Override
    String getGeneratorTypeId();

    /**
     * Gets the total energy that the generator made (EU).
     * @return the total generated energy
     */
    @Override
    long getTotalGenerated();
}
