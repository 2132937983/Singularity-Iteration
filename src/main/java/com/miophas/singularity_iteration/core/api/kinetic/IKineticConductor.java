package com.miophas.singularity_iteration.core.api.kinetic;

import net.minecraft.core.Direction;

/**
 * Conductor of KU (Kinetic Units).
 * Blocks that transmit kinetic energy implement this interface. Examples: drive shafts, gearboxes.
 *
 * Properties of a conductor:
 * - It can receive kinetic energy from a kinetic source.
 * - It can transmit kinetic energy to adjacent blocks.
 * - It can lose kinetic energy during transmission (friction).
 * - It can have direction limits.
 */
public interface IKineticConductor {
    
    /**
     * Receives kinetic energy from the specified direction.
     *
     * @param fromSide the receive direction
     * @param amount the quantity of kinetic energy to receive
     * @param simulate if true, only simulate the operation
     * @return the quantity of kinetic energy that was received
     */
    int receiveKineticFrom(Direction fromSide, int amount, boolean simulate);
    
    /**
     * Outputs kinetic energy to the specified direction.
     *
     * @param toSide the output direction
     * @param amount the quantity of kinetic energy to output
     * @param simulate if true, only simulate the operation
     * @return the quantity of kinetic energy that was output
     */
    int outputKineticTo(Direction toSide, int amount, boolean simulate);
    
    /**
     * Checks if the conductor can receive kinetic energy from the specified direction.
     *
     * @param fromSide the receive direction
     * @return true if the conductor can receive
     */
    boolean canReceiveFrom(Direction fromSide);
    
    /**
     * Checks if the conductor can output kinetic energy to the specified direction.
     *
     * @param toSide the output direction
     * @return true if the conductor can output
     */
    boolean canOutputTo(Direction toSide);
    
    /**
     * Gets the conduction efficiency (0.0 - 1.0).
     * The conductor loses energy during transmission.
     */
    float getConductionEfficiency();
    
    /**
 * Gets the kinetic energy that is currently stored in the buffer.
 * A conductor usually has a small buffer.
 */
    int getBufferedKinetic();
    
    /**
 * Gets the maximum buffer capacity.
 */
    int getMaxBufferCapacity();
    
    /**
     * Gets the current rotation speed in RPM.
     */
    int getCurrentRPM();
}


