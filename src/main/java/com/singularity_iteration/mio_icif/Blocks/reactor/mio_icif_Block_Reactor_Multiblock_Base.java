package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 反应堆多方块结构基础方块
 *
 * 所有参与流体反应堆多方块结构的方块都应该继承此�? * 自动处理结构验证通知
 */
@SuppressWarnings("null")
public class mio_icif_Block_Reactor_Multiblock_Base extends Block {

    public mio_icif_Block_Reactor_Multiblock_Base(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            // 通知多方块结构管理器检查结果
        mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // 通知多方块结构管理器检查结果
        mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


