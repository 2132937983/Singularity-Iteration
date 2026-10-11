package com.miophas.singularity_iteration.core.api.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electric drill tool interface.
 * <p>
 * Equivalent to {@code IMiningDrill} in IC2 1.12.2.
 * An item that implements this interface is an electric mining tool.
 */
public interface IMiningDrill {

    /**
     * Gets the energy necessary to mine the given block.
     *
     * @param stack the tool item stack
     * @param world the world
     * @param pos   the block position
     * @param state the block state
     * @return the energy cost (EU)
     */
    long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);

    /**
     * Gets the number of ticks necessary to mine the given block.
     *
     * @param stack the tool item stack
     * @param world the world
     * @param pos   the block position
     * @param state the block state
     * @return the number of ticks necessary
     */
    int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);

    /**
     * Mines the block.
     * <p>
     * The caller calls this method when there is sufficient energy. It does the block break logic.
     *
     * @param stack the tool item stack
     * @param world the world
     * @param pos   the block position
     * @param state the block state
     * @return true if the block was broken
     */
    boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);
}
