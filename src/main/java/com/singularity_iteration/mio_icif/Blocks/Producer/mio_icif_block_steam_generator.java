package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_generator;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidUtil;
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
 * 蒸汽机方块类
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_generator extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_steam_generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_steam_generator::new));

    public mio_icif_block_steam_generator(Properties properties) {
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
            .setValue(FACING, context.getNearestLookingDirection().getOpposite())
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
            openGui(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_steam_generator generator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 蹲下 + 含水物品 = 直接给蒸汽机补水
        if (player.isShiftKeyDown()) {
            FluidStack fluidToAdd = null;
            ItemStack returnStack = null;

            if (stack.is(Items.WATER_BUCKET)) {
                fluidToAdd = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
                returnStack = new ItemStack(Items.BUCKET);
            } else if (stack.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get())) {
                fluidToAdd = new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1000);
                returnStack = new ItemStack(Items.BUCKET);
            } else if (mio_icif_cells.isFluidCell(stack)) {
                FluidStack cellFluid = mio_icif_cells.getCellFluid(stack);
                if (cellFluid != null && !cellFluid.isEmpty()) {
                    fluidToAdd = cellFluid.copy();
                    returnStack = mio_icif_cells.getEmptyCellForStack(stack);
                    if (returnStack.isEmpty()) returnStack = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                }
            } else {
                var fluidOpt = FluidUtil.getFluidContained(stack);
                if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) {
                    fluidToAdd = fluidOpt.get().copy();
                    var handlerOpt = FluidUtil.getFluidHandler(stack);
                    if (handlerOpt.isPresent()) {
                        var handler = handlerOpt.get();
                        handler.drain(fluidToAdd, IFluidHandler.FluidAction.EXECUTE);
                        returnStack = handler.getContainer();
                    }
                }
            }

            if (fluidToAdd != null && returnStack != null) {
                int filled = generator.getWaterTank().fill(fluidToAdd, IFluidHandler.FluidAction.SIMULATE);
                if (filled >= fluidToAdd.getAmount()) {
                    generator.getWaterTank().fill(fluidToAdd, IFluidHandler.FluidAction.EXECUTE);
                    stack.shrink(1);
                    if (!player.getInventory().add(returnStack)) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), returnStack);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.CONSUME;
            }
        }

        openGui(level, pos, player);
        return ItemInteractionResult.SUCCESS;
    }

    private void openGui(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_steam_generator generator) {
            MenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu(containerId, playerInventory, generator),
                Component.translatable("container.mio_icif.steam_generator")
            );
            player.openMenu(menuProvider);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_steam_generator generator) {
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
        return new mio_icif_steam_generator(pos, state, mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_steam_generator generator) {
                mio_icif_steam_generator.tick(lvl, pos, blockState, generator);
            }
        };
    }
}