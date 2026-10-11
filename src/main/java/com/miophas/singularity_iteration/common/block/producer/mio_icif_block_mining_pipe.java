package com.miophas.singularity_iteration.common.block.producer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mining pipe block.
 * A vertical pipe with a 4x4x16 shape. */
@SuppressWarnings("null")
public class mio_icif_block_mining_pipe extends Block {

    // 4x4x16 collision box: from x=6, z=6 to x=10, z=10, and from y=0 to y=16.
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public mio_icif_block_mining_pipe(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}


