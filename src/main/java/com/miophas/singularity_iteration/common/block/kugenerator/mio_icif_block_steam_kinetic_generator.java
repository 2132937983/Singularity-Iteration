package com.miophas.singularity_iteration.common.block.kugenerator;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_steam_kinetic_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.core.prefab.fluid.FluidContainerInteraction;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
 * 蒸汽动能发生机方块类
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_kinetic_generator extends mio_icif_entity_block {

    // 六面朝向属性（上、下、北、南、东、西）
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_steam_kinetic_generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_steam_kinetic_generator::new));

    public mio_icif_block_steam_kinetic_generator(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 六面 FACING 与 LIT（不再使用父类的水平朝向）
        builder.add(FACING, LIT);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向玩家，支持六个方向放置
        return this.defaultBlockState()
            .setValue(FACING, context.getNearestLookingDirection().getOpposite())
            .setValue(LIT, false);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_steam_kinetic_generator generator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 对齐 IC2：手持蒸汽单元 / 空单元右击即与机器储罐双向传输（蒸汽槽只进、蒸馏水槽双向）。
        if (FluidContainerInteraction.interact(player, hand, generator.getCombinedFluidHandler())) {
            return ItemInteractionResult.SUCCESS;
        }

        openGui(level, pos, player, generator);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                openGui(level, pos, player, generator);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void openGui(Level level, BlockPos pos, Player player, mio_icif_steam_kinetic_generator generator) {
        MenuProvider menuProvider = new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new com.miophas.singularity_iteration.common.menu.generator.SteamKineticGeneratorMenu(containerId, playerInventory, generator),
            Component.translatable("container.mio_icif.steam_kinetic_generator")
        );
        player.openMenu(menuProvider);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.isClientSide()) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                for (int i = 0; i < generator.getItemHandler().getSlots(); i++) {
                    ItemStack stack = generator.getItemHandler().getStackInSlot(i);
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
        return new mio_icif_steam_kinetic_generator(pos, state, mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                mio_icif_steam_kinetic_generator.tick(lvl, pos, blockState, generator);
            }
        };
    }
}
