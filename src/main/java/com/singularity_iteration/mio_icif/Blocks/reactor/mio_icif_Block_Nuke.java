package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_nuke;
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
 * 核弹方块
 *
 * 功能量? * - 拥有8个工业TNT槽位置?个核原料槽位
 * - 右键点击打开GUI进行装填
 * - 根据装填内容计算爆炸威力
 * - 使用射线算法进行高效爆炸计算
 */
@SuppressWarnings("null")
public class mio_icif_Block_Nuke extends Block implements EntityBlock {

    public mio_icif_Block_Nuke(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_reactor_nuke nuke) {
            MenuProvider menuProvider = nuke;
            player.openMenu(menuProvider);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_reactor_nuke(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // 客户端和服务端都需�?tick 来处理粒子效果和逻辑
        return createTickerHelper(blockEntityType,
            mio_icif_block_entities.NUKE_ENTITY_TYPE.get(),
            mio_icif_reactor_nuke::tick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_reactor_nuke nuke) {
                nuke.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


