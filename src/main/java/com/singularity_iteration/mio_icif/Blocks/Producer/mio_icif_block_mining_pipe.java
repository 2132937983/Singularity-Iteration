package com.singularity_iteration.mio_icif.Blocks.Producer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 采矿管道方块
 * 4x4x16 的竖向管理? */
@SuppressWarnings("null")
public class mio_icif_block_mining_pipe extends Block {

    // 4x4x16 的碰撞箱 (�?x=6, z=6 �?x=10, z=10，高度从 y=0 �?y=16)
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


