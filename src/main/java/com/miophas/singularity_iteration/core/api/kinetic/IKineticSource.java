package com.miophas.singularity_iteration.core.api.kinetic;

import net.minecraft.core.Direction;

/**
 * Source of KU (Kinetic Units).
 * Blocks or items that produce kinetic energy implement this interface.
 *
 * Properties of a kinetic source:
 * - It produces kinetic energy continuously. Examples: steam turbines, water mills, wind generators.
 * - It can output the kinetic energy to an adjacent kinetic storage or conductor.
 * - It can have start conditions. Examples: steam, water flow, wind.
 */
public interface IKineticSource {
    
    /**
     * Gets the current kinetic output in KU/tick.
     *
     * @param side the output direction
     * @return the kinetic energy produced this tick
     */
    int getKineticOutput(Direction side);
    
    /**
     * Gets the maximum kinetic output in KU/tick.
     *
     * @param side the output direction
     * @return the maximum possible kinetic output
     */
    int getMaxKineticOutput(Direction side);
    
    /**
     * Checks if the source is producing kinetic energy.
     *
     * @param side the output direction
     * @return true if the source is producing kinetic energy
     */
    boolean isProducingKinetic(Direction side);
    
    /**
     * Checks if the source can output kinetic energy to the specified direction.
     *
     * @param side the output direction
     * @return true if the source can output
     */
    boolean canOutputKinetic(Direction side);
    
    /**
     * Gets the current rotation speed in RPM.
     * The rotation speed of the source can change the output.
     */
    int getCurrentRPM();
    
    /**
     * Gets the maximum rotation speed in RPM.
     */
    int getMaxRPM();
}


