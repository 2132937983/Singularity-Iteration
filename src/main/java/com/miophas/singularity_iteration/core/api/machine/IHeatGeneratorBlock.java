package com.miophas.singularity_iteration.core.api.machine;

/**
 * HU heat generator interface.
 * <p>
 * It gives one runtime API for HU heat generators.
 * Blocks that implement this interface can supply heat to adjacent machines through the heat system.
 * <p>
 * Note: This interface is independent of {@link IEnergyBlock} (the EU system),
 * because HU heat is an independent energy system parallel to EU electricity.
 */
public interface IHeatGeneratorBlock {

    /**
     * Gets the current heat output rate (HU/tick).
     *
     * @return the current heat output rate. 0 means no generation.
     */
    int getHeatOutput();

    /**
     * Checks if the generator generates now (is burning).
     *
     * @return true if it generates now
     */
    boolean isGenerating();

    /**
     * Gets the remaining burn time of the fuel (ticks).
     *
     * @return the remaining burn time
     */
    int getBurnTime();

    /**
     * Gets the total burn time of the current fuel (ticks).
     *
     * @return the total burn time
     */
    int getBurnDuration();

    /**
     * Gets the heat generation rate (HU/tick).
     *
     * @return the heat generation rate
     */
    int getHeatGenerationRate();
}
