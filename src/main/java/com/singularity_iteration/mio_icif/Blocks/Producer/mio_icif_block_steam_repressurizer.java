package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_repressurizer;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽再加压机方方块? */
@SuppressWarnings("null")
public class mio_icif_block_steam_repressurizer extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_steam_repressurizer> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_steam_repressurizer::new));

    public mio_icif_block_steam_repressurizer(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getClickedFace())
            .setValue(LIT, false);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu(containerId, playerInventory, repressurizer),
                    Component.translatable("container.mio_icif.steam_repressurizer")
                );
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                for (int i = 0; i < repressurizer.getItemHandler().getSlots(); i++) {
                    ItemStack stack = repressurizer.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_steam_repressurizer(pos, state, mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                mio_icif_steam_repressurizer.tick(lvl, pos, blockState, repressurizer);
            }
        };
    }
}

