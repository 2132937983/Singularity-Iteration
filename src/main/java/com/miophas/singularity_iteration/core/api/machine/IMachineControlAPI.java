package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Machine control API.
 *
 * <p>Gives basic control functions: start and stop a machine, and read its state.
 * This interface comes from IMachineAPI. It has one responsibility only.
 */
public interface IMachineControlAPI {

    /**
     * Starts the machine at the given position.
     */
    boolean startMachine(Level world, BlockPos pos);

    /**
     * Stops the machine at the given position.
     */
    boolean stopMachine(Level world, BlockPos pos);

    /**
     * Gets the current work progress of the machine.
     */
    int getProgress(Level world, BlockPos pos);

    /**
     * Gets the maximum work progress of the machine.
     */
    int getMaxProgress(Level world, BlockPos pos);

    /**
     * Checks if the machine is working now.
     */
    boolean isWorking(Level world, BlockPos pos);

    /**
     * Gets the machine type.
     *
     * @return the machine type in an Optional, or an empty Optional if the position does not contain a machine
     */
    Optional<IMachineAPI.MachineType> getMachineType(Level world, BlockPos pos);

    /**
     * Gets the machine configuration.
     *
     * @return the machine configuration in an Optional, or an empty Optional if the position does not contain a machine
     */
    Optional<IMachineAPI.IMachineConfiguration> getMachineConfiguration(Level world, BlockPos pos);

    /**
     * Starts the machine by force.
     */
    boolean forceStart(Level world, BlockPos pos);

    /**
     * Stops the machine by force.
     */
    boolean forceStop(Level world, BlockPos pos);
}