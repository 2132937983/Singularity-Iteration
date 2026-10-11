package com.miophas.singularity_iteration.common.block.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke;
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
 * Nuke block.
 *
 * Functions:
 * - It has 8 industrial TNT slots and 1 nuclear material slot.
 * - Right-click opens the GUI to load the nuke.
 * - It calculates the explosion power from the loaded items.
 * - It uses a ray algorithm to calculate the explosion efficiently.
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
        // Both the client and the server tick the block. The client shows particles. The server runs the logic.
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


