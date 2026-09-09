package com.singularity_iteration.mio_icif.Blocks.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Manual_KineticU_Generator;
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
 * 手动动能发电机方法?
 * 玩家右键点击来产生动作?
 *
 * 特点�?
 * - 不需要燃烧?
 * - 玩家右键点击产生动能
 * - 每次点击消耗饥饿�?
 * - 饥饿度低�?时无法使�?
 * - 适合早期游戏使用
 */
@SuppressWarnings("null")
public class mio_icif_Block_Manual_Kinetic_Generator extends mio_icif_entity_block {

    // 定义方块状态：是否正在工作（被点击后）
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_Block_Manual_Kinetic_Generator(Properties properties) {
        super(properties);
        // 注册默认状态?
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
        return simpleCodec(properties -> new mio_icif_Block_Manual_Kinetic_Generator(properties));
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
        return createTicker(level, state, type, mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 创建方块实体
        return mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_Manual_KineticU_Generator generator) {
            // 检查玩家是否有足够的饥饿�?
            if (!generator.hasEnoughHunger(player)) {
                // 饥饿度不足，不发送消息，直接返回
                return InteractionResult.CONSUME;
            }

            // 处理玩家点击
            boolean success = generator.onPlayerClick(player);

            if (success) {
                // 更新方块状态为活跃
                level.setBlock(pos, state.setValue(ACTIVE, true), 3);

                // 播放声音效果（可选）
                // level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.5F);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    /**
     * 获取方块实体�?ticker
     * 用于�?Level 中注册?tick 更新
     */
    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level, BlockState state, BlockEntityType<T> type, BlockEntityType<mio_icif_Manual_KineticU_Generator> entityType) {
        return type == entityType ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_Manual_KineticU_Generator generator) {
                    mio_icif_Manual_KineticU_Generator.tick(l, p, s, generator);
                }
            } : null;
    }
}


