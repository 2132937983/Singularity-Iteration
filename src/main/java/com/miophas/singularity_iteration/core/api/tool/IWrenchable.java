package com.miophas.singularity_iteration.core.api.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collection;
import java.util.List;

/**
 * Wrench interaction interface.
 * <p>
 * It is equivalent to {@code IWrenchable} in IC2 1.12.2.
 * Players can use a wrench on blocks that implement this interface. The wrench can rotate and remove the block.
 * <p>
 * This interface gives general default implementations for all methods. They use the block properties {@code FACING}/{@code HORIZONTAL_FACING}.
 * A machine block only has to declare {@code implements IWrenchable}. Then the API manages it, and the block
 * does not need its own rotate or remove logic. Blocks that need special behavior can override the related methods.
 */
public interface IWrenchable {

    /**
     * Gets the current facing of the block.
     *
     * @param world the world
     * @param pos   the block position
     * @return the current facing, or NORTH if the block has no facing property
     */
    default Direction getFacing(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Sets the facing of the block.
     *
     * @param world  the world
     * @param pos    the block position
     * @param facing the new facing
     * @param player the player who does the operation
     * @return true if the facing changed
     */
    default boolean setFacing(Level world, BlockPos pos, Direction facing, Player player) {
        if (!canSetFacing(world, pos, facing)) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        BlockState newState = null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            if (state.getValue(BlockStateProperties.FACING) == facing) {
                return false;
            }
            newState = state.setValue(BlockStateProperties.FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (!facing.getAxis().isHorizontal()) {
                return false;
            }
            if (state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                return false;
            }
            newState = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (newState != null) {
            world.setBlock(pos, newState, 3);
            return true;
        }
        return false;
    }

    /**
     * Checks if the given facing can be set.
     *
     * @param world  the world
     * @param pos    the block position
     * @param facing the target facing
     * @return true if the facing can be set
     */
    default boolean canSetFacing(Level world, BlockPos pos, Direction facing) {
        return true;
    }

    /**
     * Checks if the wrench can remove this block.
     *
     * @param world  the world
     * @param pos    the block position
     * @param player the player who does the operation
     * @return true if the block can be removed
     */
    default boolean wrenchCanRemove(Level world, BlockPos pos, Player player) {
        return true;
    }

    /**
     * Gets the items that drop when a wrench removes this block.
     *
     * @param world the world
     * @param pos   the block position
     * @param state the block state
     * @return the list of dropped items
     */
    default Collection<ItemStack> getWrenchDrops(Level world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (world instanceof ServerLevel serverLevel) {
            return Block.getDrops(state, serverLevel, pos, blockEntity);
        }
        return List.of(new ItemStack(state.getBlock()));
    }
}
