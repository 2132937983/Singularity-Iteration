package com.singularity_iteration.mio_icif.Blocks.HUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 热交换机方块
 * 通过热交换器将热冷却剂或岩浆转化为热�? */
@SuppressWarnings("null")
public class mio_icif_block_heat_source_fluid extends mio_icif_entity_block {

    // 热交换机需要支持所有六个方向（覆盖基类的水平方向FACING）
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_block_heat_source_fluid(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 不调用super，自己添加FACING和ACTIVE
        builder.add(FACING, ACTIVE);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_block_heat_source_fluid(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向玩家（支持所有六个方向）
        return this.defaultBlockState()
            .setValue(FACING, context.getNearestLookingDirection().getOpposite())
            .setValue(ACTIVE, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.HEAT_SOURCE_FLUID.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_heat_source_fluid heatSource) {
                    mio_icif_heat_source_fluid.tick(l, p, s, heatSource);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.HEAT_SOURCE_FLUID.get().create(pos, state);
    }
}