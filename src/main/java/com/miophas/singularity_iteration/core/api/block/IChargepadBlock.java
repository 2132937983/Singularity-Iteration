package com.miophas.singularity_iteration.core.api.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Charge pad block interface.
 * All charge pad blocks must implement this interface.
 * The client particle system uses it to find charge pads and render the charge particle effect.
 *
 * <p>An addon developer only needs to implement this interface on a charge pad block to get the blue charge particle effect.
 * No change to client code is necessary.
 */
public interface IChargepadBlock {

    /**
     * Gets the current active (LIT) state of the charge pad.
     *
     * @param state the block state
     * @return true if the charge pad charges now (active state)
     */
    boolean isCharging(BlockState state);
}