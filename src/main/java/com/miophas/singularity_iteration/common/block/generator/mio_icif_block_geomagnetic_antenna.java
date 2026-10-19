package com.miophas.singularity_iteration.common.block.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_block_geomagnetic_antenna extends Block {
    public mio_icif_block_geomagnetic_antenna(Properties properties) {
        super(properties);
    }

    /** 0.1.10.7: the generators next to it check their structure at once (they no longer check every tick). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()))
            com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_geomagnetic_generator.structureChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!newState.is(state.getBlock()))
            com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_geomagnetic_generator.structureChanged(level, pos);
    }
}