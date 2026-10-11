package com.miophas.singularity_iteration.core.api.machine;

/**
 * KU kinetic generator interface.
 * <p>
 * It gives one runtime API for all KU kinetic generators.
 * A block that implements this interface can supply kinetic energy to adjacent machines through the kinetic system.
 * <p>
 * Note: this interface is independent of {@link IEnergyBlock} (EU system),
 * because KU kinetic energy is a separate energy system parallel to EU electricity.
 */
public interface IKineticGeneratorBlock {

    /**
     * Gets the current kinetic output rate (KU/tick).
     *
     * @return the current kinetic output rate, 0 if the generator does not generate
     */
    int getKineticOutput();

    /**
     * Checks if the generator is generating now (burning fuel).
     *
     * @return true if it is generating
     */
    boolean isGenerating();

    /**
     * Gets the remaining fuel burn time (ticks).
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
     * Gets the kinetic generation rate (KU/tick).
     *
     * @return the kinetic generation rate
     */
    int getKineticGenerationRate();

    /**
     * Gets the current rotor speed (RPM).
     *
     * @return the current rotation speed
     */
    int getRotorRPM();
}
