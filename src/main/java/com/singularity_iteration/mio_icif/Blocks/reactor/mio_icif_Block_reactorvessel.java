package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 流体核反应堆压力容器框架方块
 *
 * 功能量?
 * - 用于构建流体核反应堆多方块结构的框架方块
 * - 作为反应堆的外壳结构，提供结构支�?
 * - 防爆性较强，能够抵御核反应堆爆炸
 * - 无特殊功能，不链接方块实体?
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
            // 通知多方块结构管理器检查结果?
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // 通知多方块结构管理器检查结果?
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


