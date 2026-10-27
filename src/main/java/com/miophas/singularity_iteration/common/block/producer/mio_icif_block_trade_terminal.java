// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeTerminalBlockEntity;
import com.miophas.singularity_iteration.common.exchange.TerminalRules;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.27 (X2): the Trade Terminal block ({@link TradeTerminalBlockEntity}). The placer owns it; players without access
 * cannot open it; when it is broken its buffers go to the owner's exchange mailbox.
 */
@SuppressWarnings("null")
public class mio_icif_block_trade_terminal extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final MapCodec<mio_icif_block_trade_terminal> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_trade_terminal::new));

    public mio_icif_block_trade_terminal(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TradeTerminalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TradeTerminalBlockEntity terminal) terminal.setOwnerFromPlacer(placer);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TradeTerminalBlockEntity terminal) {
            if (TerminalRules.may(terminal.levelOf(serverPlayer), TerminalRules.Action.OPEN)) serverPlayer.openMenu(terminal, pos);
            else serverPlayer.displayClientMessage(Component.translatable("message.mio_icif.terminal.no_access", terminal.ownerName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TradeTerminalBlockEntity terminal) {
            terminal.emptyBuffers();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, mio_icif_block_entities.TRADE_TERMINAL_ENTITY_TYPE.get(),
            (lvl, pos, blockState, blockEntity) -> TradeTerminalBlockEntity.tick(lvl, pos, blockState, blockEntity));
    }
}
