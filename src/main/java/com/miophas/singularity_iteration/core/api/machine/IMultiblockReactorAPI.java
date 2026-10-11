package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Reactor multiblock API.
 *
 * <p>Use it to query and manage reactor multiblock structures.
 * It is split from IMachineAPI to follow the single responsibility principle.
 *
 * <p>Note: this interface is only for multiblock structure management.
 * {@link com.miophas.singularity_iteration.core.api.reactor.IReactorAPI} (the reactor component system)
 * has a different responsibility.
 */
public interface IMultiblockReactorAPI {

    /**
     * Gets the reactor multiblock information.
     *
     * @return the multiblock information in an Optional, or an empty Optional if no reactor is at the position
     */
    Optional<IMachineAPI.IMultiblockInfo> getReactorInfo(Level world, BlockPos pos);

    /**
     * Checks if the position has a reactor controller.
     */
    boolean isReactorController(Level world, BlockPos pos);

    /**
     * Gets the reactor temperature.
     */
    double getReactorTemperature(Level world, BlockPos pos);

    /**
     * Gets the reactor coolant level.
     */
    double getReactorCoolantLevel(Level world, BlockPos pos);
}