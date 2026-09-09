package com.singularity_iteration.mio_icif.Blocks.HUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_rt_heat_generator;
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
 * 放射性同位素温差加热机方法? * 通过RTG燃料靶丸产生热能，只在正面输出热�? */
@SuppressWarnings("null")
public class mio_icif_block_rt_heat_generator extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_block_rt_heat_generator(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_block_rt_heat_generator(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 支持六面放置�
    // 非潜行时：方块正面朝向玩家（像原版发射器那样�
    // 潜行时：根据点击的面放置
        Direction direction;
        // if (context.isSecondaryUseActive()) {
        //     // 潜行时：根据点击的面放置
        //     direction = context.getClickedFace();
        // } else {
            // 非潜行时：方块正面朝向玩�
        direction = context.getNearestLookingDirection().getOpposite();
        // }
        return this.defaultBlockState()
            .setValue(FACING, direction)
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
        return type == mio_icif_block_entities.RT_HEAT_GENERATOR.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_rt_heat_generator generator) {
                    mio_icif_rt_heat_generator.tick(l, p, s, generator);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.RT_HEAT_GENERATOR.get().create(pos, state);
    }
}

