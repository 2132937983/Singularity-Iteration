package com.singularity_iteration.mio_icif.Blocks.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Water_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
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
 * 水力动能发生机方法? * 通过水流产生动能（KU），需要放置转子才能工�? *
 * 特点�? * - 只能在河流或海洋生物群系的水中工�? * - 只能使用天然水源，用桶洒出的水源无效
 * - 无法放置木转�? * - 转子耐久消耗速率为风动机器?�? * - 海洋生物群系中实测平均发电量高于风力
 */
@SuppressWarnings("null")
public class mio_icif_Block_Water_Kinetic_Generator extends mio_icif_entity_block {

    // 定义方块状态：是否正在工作
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_Block_Water_Kinetic_Generator(Properties properties) {
        super(properties);
        // 注册默认状态
    this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // FACING 由父类添加，只需要添�?ACTIVE
        builder.add(ACTIVE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_Water_Kinetic_Generator(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ACTIVE, false);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 创建并返回?ticker 用于更新方块实体
        return createTickerHelper(type, mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (lvl, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof mio_icif_Water_Kinetic_Generator generator) {
                    mio_icif_Water_Kinetic_Generator.tick(lvl, pos, blockState, generator);
                }
            });
    }

    /**
     * 创建并返回?ticker 用于更新方块实体
     */
    @SuppressWarnings("unchecked")
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> entityType, BlockEntityTicker<? super E> ticker) {
        return type == entityType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 创建方块实体
        return mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 打开GUI
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_Water_Kinetic_Generator generator) {
            player.openMenu(generator);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}

