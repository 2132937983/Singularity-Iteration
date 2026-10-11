package com.miophas.singularity_iteration.common.block.reactor;

import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pressure vessel frame block of the fluid nuclear reactor.
 *
 * Function:
 * - It is the frame block of the fluid nuclear reactor multiblock structure.
 * - It forms the reactor shell and gives structural support.
 * - It has high blast resistance and can survive a nuclear reactor explosion.
 * - It has no special function and does not link to a block entity.
 */
@SuppressWarnings("null")
public class mio_icif_Block_reactorvessel extends Block {

    public mio_icif_Block_reactorvessel(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            // Tell the multiblock structure manager to check the structure.
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // Tell the multiblock structure manager to check the structure.
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


