package com.singularity_iteration.mio_icif.Blocks.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_unlimit_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 无限发电机方法? * SC级发电机，可以无限输出能量而不消耗自身存�? */
@SuppressWarnings("null")
public class mio_icif_block_unlimit_generator extends mio_icif_entity_block {

    // 定义方块状态：是否正在发电（无限发电机始终处于活跃状态）
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_block_unlimit_generator(Properties properties) {
        super(properties);
        // 设置默认状态为活跃
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // FACING 由父类添加，只需要添�?ACTIVE
        builder.add(ACTIVE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_block_unlimit_generator(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ACTIVE, true);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_unlimit_generator generator) {
                    mio_icif_unlimit_generator.tick(l, p, s, generator);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 无限发电机始终发生
 */
    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 15; // 最大亮�
}
}

