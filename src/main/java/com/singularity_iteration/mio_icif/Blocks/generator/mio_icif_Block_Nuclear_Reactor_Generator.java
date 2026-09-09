package com.singularity_iteration.mio_icif.Blocks.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 核反应堆发电机方法? *
 * 特点�? * - 自身不直接发电，依靠内部放置的燃料棒进行发电
 * - 拥有54个槽位用于放置燃料棒、散热片等物�? * - 拥有热量存储系统，最大热量存储为10000HU
 */
@SuppressWarnings("null")
public class mio_icif_Block_Nuclear_Reactor_Generator extends mio_icif_entity_block {

    // 定义方块状态：是否正在运行
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_Block_Nuclear_Reactor_Generator(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // FACING 由父类添加，只需要添�?ACTIVE
        builder.add(ACTIVE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_Nuclear_Reactor_Generator(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ACTIVE, false);

        // 通知多方块结构管理器
        if (!context.getLevel().isClientSide()) {
            context.getLevel().scheduleTick(context.getClickedPos(), this, 1);
        }

        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            // 延迟一 tick 通知，确保方块完全放弃
        level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // 检查是否是因为堆温过高导致的爆�
        BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_nuclear_reactor_generator reactor) {
                int currentHeat = (int) reactor.getCurrentHeat();
                int maxHeat = (int) reactor.getMaxHeat();
                double heatPercentage = (double) currentHeat / maxHeat;

                // 如果堆温超过 20%，触发核�
            if (heatPercentage >= 0.20) {
                    // 触发核爆
                    triggerExplosion(level, pos, heatPercentage);
                    // 不调用父类的 onRemove，因为爆炸已经处理了方块移除
                    return;
                }
            }

            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 触发核爆�
 * @param level 世界
     * @param pos 爆炸位置
     * @param heatPercentage 堆温百分配
 */
    private void triggerExplosion(Level level, BlockPos pos, double heatPercentage) {
        if (level.isClientSide()) return;

        // 基础爆炸半径
        float baseRadius = 1.5f;
        float explosionRadius = baseRadius + (float)(heatPercentage * 2.5f);

        // 产生爆炸
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            explosionRadius, Level.ExplosionInteraction.BLOCK);

        // 如果堆温超过 50%，额外产生火�
    if (heatPercentage >= 0.50) {
            // 在周围生成火
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos firePos = pos.offset(x, y, z);
                        if (level.getBlockState(firePos).isAir() && level.getBlockState(firePos.below()).isSolidRender(level, firePos.below())) {
                            level.setBlock(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // 如果堆温超过 80%，产生辐射效果
    if (heatPercentage >= 0.80) {
            // 对周围生物造成辐射伤害
            var entities = level.getEntities(null,
                new net.minecraft.world.phys.AABB(
                    pos.getX() - 15, pos.getY() - 15, pos.getZ() - 15,
                    pos.getX() + 16, pos.getY() + 16, pos.getZ() + 16
                )
            );

            for (var entity : entities) {
                if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                    // 造成辐射伤害
                    livingEntity.hurt(level.damageSources().magic(), 15.0f);
                    // 添加辐射效果
                    livingEntity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.singularity_iteration.mio_icif.effect.mio_icif_effects.RADIATION, 300, 2));
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
                    mio_icif_nuclear_reactor_generator.tick(l, p, s, reactor);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.generator.gui_open_failed", Component.translatable("block.mio_icif.generator.block_nuclear_reactor_generator")));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

