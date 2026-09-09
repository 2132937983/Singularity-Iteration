package com.singularity_iteration.mio_icif.Blocks.Build;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iridium_tank_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_iridium_tank extends mio_icif_entity_block {

    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");

    public static final MapCodec<mio_icif_iridium_tank> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_iridium_tank::new));

    public mio_icif_iridium_tank(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED_UP, false)
            .setValue(CONNECTED_DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED_UP, CONNECTED_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean up = level.getBlockState(pos.above()).getBlock() instanceof mio_icif_iridium_tank;
        boolean down = level.getBlockState(pos.below()).getBlock() instanceof mio_icif_iridium_tank;
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(CONNECTED_UP, up)
            .setValue(CONNECTED_DOWN, down);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            state = state.setValue(CONNECTED_UP, neighborState.getBlock() instanceof mio_icif_iridium_tank);
        } else if (direction == Direction.DOWN) {
            state = state.setValue(CONNECTED_DOWN, neighborState.getBlock() instanceof mio_icif_iridium_tank);
        }
        return state;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            BlockPos above = pos.above();
            BlockPos below = pos.below();
            BlockState stateAbove = level.getBlockState(above);
            BlockState stateBelow = level.getBlockState(below);
            if (stateAbove.getBlock() instanceof mio_icif_iridium_tank) {
                level.setBlock(above, stateAbove.setValue(CONNECTED_DOWN, true), Block.UPDATE_ALL);
            }
            if (stateBelow.getBlock() instanceof mio_icif_iridium_tank) {
                level.setBlock(below, stateBelow.setValue(CONNECTED_UP, true), Block.UPDATE_ALL);
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof mio_icif_iridium_tank_entity tank) {
                tank.onBlockPlaced(level);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos above = pos.above();
            BlockPos below = pos.below();
            BlockState stateAbove = level.getBlockState(above);
            BlockState stateBelow = level.getBlockState(below);
            if (stateAbove.getBlock() instanceof mio_icif_iridium_tank) {
                level.setBlock(above, stateAbove.setValue(CONNECTED_DOWN, false), Block.UPDATE_ALL);
            }
            if (stateBelow.getBlock() instanceof mio_icif_iridium_tank) {
                level.setBlock(below, stateBelow.setValue(CONNECTED_UP, false), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_iridium_tank_entity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        ItemStack dropStack = new ItemStack(this);
        BlockEntity blockEntity = params.getParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof mio_icif_iridium_tank_entity tankEntity) {
            net.minecraft.core.HolderLookup.Provider registries = tankEntity.getLevel() != null ? tankEntity.getLevel().registryAccess() : null;
            if (registries != null) {
                CompoundTag blockEntityTag = tankEntity.saveWithId(registries);
                dropStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
            }
        }
        drops.add(dropStack);
        return drops;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("FluidTank")) {
                CompoundTag fluidTag = tag.getCompound("FluidTank");
                FluidStack fluidStack = FluidStack.parseOptional(context.registries(), fluidTag);
                if (!fluidStack.isEmpty()) {
                    tooltip.add(Component.translatable("tooltip.mio_icif.tank.fluid",
                            fluidStack.getHoverName(), fluidStack.getAmount(), mio_icif_iridium_tank_entity.TANK_CAPACITY)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                } else {
                    tooltip.add(Component.translatable("tooltip.mio_icif.tank.empty")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        }
        tooltip.add(Component.translatable("tooltip.mio_icif.tank.capacity", mio_icif_iridium_tank_entity.TANK_CAPACITY)
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, mio_icif_block_entities.IRIDIUM_TANK.get(), mio_icif_iridium_tank_entity::tick);
    }
}