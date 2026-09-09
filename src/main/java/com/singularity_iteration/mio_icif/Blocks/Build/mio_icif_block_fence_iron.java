package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 自定义铁栅栏方块
 * 使用木栅栏的连接逻辑和碰撞箱，但具有铁栅栏的材质和属性
 */
@SuppressWarnings("null")
public class mio_icif_block_fence_iron extends FenceBlock {

    public mio_icif_block_fence_iron(Properties properties) {
        super(properties);
    }

    /**
     * 连接逻辑：检测是否可以连接到相邻方块
     * 铁栅栏会连接到其他栅栏、固体方块等
     */
    @Override
    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        // 连接到其他栅栏（包括同类型的铁栅栏和原版铁栅栏）
        if (state.getBlock() instanceof FenceBlock) {
            return true;
        }
        // 连接到固体方块
        if (isSideSolid) {
            return true;
        }
        return false;
    }

    /**
     * 获取碰撞箱 - 使用木栅栏的碰撞箱
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 使用父类（FenceBlock）的碰撞箱计算
        return super.getShape(state, level, pos, context);
    }

    /**
     * 获取交互形状 - 使用木栅栏的形状
     */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return super.getInteractionShape(state, level, pos);
    }
}