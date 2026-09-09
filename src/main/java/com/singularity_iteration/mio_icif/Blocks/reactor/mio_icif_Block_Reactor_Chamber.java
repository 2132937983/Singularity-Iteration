package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 核反应仓方块
 *
 * 功能量? * - 用于辅助核反应堆，增加核反应堆的槽位
 * - 每有一个面接触核反应堆，就从左往右多添加一列反应槽位? 个槽位）
 * - 六个面全部都有核反应仓时，核反应堆拥有完整的 54 个反应槽
 * - 核反应仓自身没有 GUI，右键显示所连接的核反应堆的 GUI
 * - 当核反应仓同时接触两个核反应堆时会爆�? */
@SuppressWarnings("null")
public class mio_icif_Block_Reactor_Chamber extends BaseEntityBlock {

    public mio_icif_Block_Reactor_Chamber(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_Reactor_Chamber(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            // 检查是否同时接触两个核反应�
        checkMultipleReactorsAndExplode(level, pos);

            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    /**
     * 检查是否同时接触多个核反应堆，如果是则爆炸
     */
    private void checkMultipleReactorsAndExplode(Level level, BlockPos pos) {
        int reactorCount = 0;

        // 检查六个面
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator) {
                reactorCount++;
            }
        }

        // 如果连接了多个核反应堆，立即爆炸
        if (reactorCount > 1) {
            // 产生爆炸
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1.5f, Level.ExplosionInteraction.BLOCK);

            // 移除方块（不触发掉落�
        level.removeBlock(pos, false);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // 检查是否是因为堆温过高导致的爆�
        BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_reactor_chamber chamber) {
                // 检查连接的核反应堆堆温
                var reactor = chamber.getConnectedReactor();
                if (reactor != null) {
                    int currentHeat = (int) reactor.getCurrentHeat();
                    int maxHeat = (int) reactor.getMaxHeat();
                    double heatPercentage = (double) currentHeat / maxHeat;

                    // 如果堆温超过 20%，触发核�
                if (heatPercentage >= 0.20) {
                        // 触发核爆
                        triggerExplosion(level, pos, reactor);
                        // 不调用父类的 onRemove，因为爆炸已经处理了方块移除
                        return;
                    }
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
     * @param reactor 连接的核反应�
 */
    private void triggerExplosion(Level level, BlockPos pos, com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor) {
        if (level.isClientSide()) return;

        // 计算爆炸威力（基于反应堆堆温百分比）
        int currentHeat = (int) reactor.getCurrentHeat();
        int maxHeat = (int) reactor.getMaxHeat();
        double heatPercentage = (double) currentHeat / maxHeat;

        // 基础爆炸半径
        float baseRadius = 1.5f;
        float explosionRadius = baseRadius + (float)(heatPercentage * 2.0f);

        // 产生爆炸
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            explosionRadius, Level.ExplosionInteraction.BLOCK);

        // 如果堆温超过 50%，额外产生火�
    if (heatPercentage >= 0.50) {
            // 在周围生成火
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
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
                    pos.getX() - 10, pos.getY() - 10, pos.getZ() - 10,
                    pos.getX() + 11, pos.getY() + 11, pos.getZ() + 11
                )
            );

            for (var entity : entities) {
                if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                    // 造成辐射伤害
                    livingEntity.hurt(level.damageSources().magic(), 10.0f);
                    // 添加辐射效果
                    livingEntity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.singularity_iteration.mio_icif.effect.mio_icif_effects.RADIATION, 200, 1));
                }
            }
        }
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_reactor_chamber chamber) {
                    mio_icif_reactor_chamber.tick(l, p, s, chamber);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}