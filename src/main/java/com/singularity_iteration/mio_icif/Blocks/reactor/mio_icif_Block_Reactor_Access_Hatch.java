package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_access_hatch;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 流体反应堆访问接口方法? *
 * 功能量? * - 用于访问流体反应堆多方块结构中的核反应堆
 * - 右键点击时显示连接的核反应堆�?GUI
 * - 只有在有效的流体反应堆结构下才能正常工作
 */
@SuppressWarnings("null")
public class mio_icif_Block_Reactor_Access_Hatch extends Block implements EntityBlock {

    public mio_icif_Block_Reactor_Access_Hatch(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_reactor_access_hatch accessHatch) {
            // 调试模式：玩家潜行时输出多方块结构诊断信息
        if (player.isShiftKeyDown()) {
                accessHatch.debugMultiblockStructure(level, pos, player);
                return InteractionResult.CONSUME;
            }

            // 检查是否已连接到有效的流体反应�
        if (accessHatch.isConnected()) {
                // 打开连接的核反应堆的 GUI
                MenuProvider menuProvider = accessHatch;
                player.openMenu(menuProvider);
                return InteractionResult.CONSUME;
            } else {
                // 未连接，输出调试信息
                accessHatch.debugMultiblockStructure(level, pos, player);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_reactor_access_hatch(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType,
            com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.REACTOR_ACCESS_HATCH_ENTITY_TYPE.get(),
            mio_icif_reactor_access_hatch::tick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}


