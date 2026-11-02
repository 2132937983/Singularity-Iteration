// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.TradeBoxBlockEntity;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
 * 0.1.10.42: the Basic and the Advanced Trade Box ({@link TradeBoxBlockEntity}). The placer owns it; only the owner and
 * operators open it; when it is broken its contents go to the owner's exchange mailbox. The upgrade kit, used by the owner
 * (or an operator) on a basic box, makes it an advanced box with its contents and settings.
 */
@SuppressWarnings("null")
public class mio_icif_block_trade_box extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final MapCodec<mio_icif_block_trade_box> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec(), Codec.BOOL.fieldOf("advanced").forGetter(mio_icif_block_trade_box::advanced))
            .apply(instance, mio_icif_block_trade_box::new));

    private final boolean advanced;

    public mio_icif_block_trade_box(Properties properties, boolean advanced) {
        super(properties);
        this.advanced = advanced;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    /** The advanced box (54 slots, return slots, filter, instant sales, bounty deliveries). */
    public boolean advanced() { return advanced; }

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
        return new TradeBoxBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TradeBoxBlockEntity box) box.setOwnerFromPlacer(placer);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TradeBoxBlockEntity box) {
            if (box.mayOpen(serverPlayer)) box.open(serverPlayer);
            else serverPlayer.displayClientMessage(Component.translatable("message.mio_icif.trade_box.no_access", box.ownerName()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The upgrade kit on a basic box: it becomes an advanced box with its contents, owner and settings. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (advanced || !stack.is(mio_icif_normal.TRADE_BOX_UPGRADE.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TradeBoxBlockEntity box)) return ItemInteractionResult.FAIL;
        if (!box.mayOpen(player)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.trade_box.no_access", box.ownerName()), true);
            return ItemInteractionResult.FAIL;
        }
        CompoundTag tag = box.upgradeTag(level.registryAccess());
        box.upgrading = true;
        BlockState next = mio_icif_blocks.TRADE_BOX_ADVANCED.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TradeBoxBlockEntity upgraded) {
            upgraded.loadWithComponents(tag, level.registryAccess());
            upgraded.setChanged();
        }
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.mio_icif.trade_box.upgraded"), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TradeBoxBlockEntity box) {
            box.emptyContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, mio_icif_block_entities.TRADE_BOX_ENTITY_TYPE.get(),
            (lvl, pos, blockState, blockEntity) -> TradeBoxBlockEntity.tick(lvl, pos, blockState, blockEntity));
    }
}
