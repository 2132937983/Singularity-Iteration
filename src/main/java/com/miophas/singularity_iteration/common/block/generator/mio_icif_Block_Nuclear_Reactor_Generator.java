package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;
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
        // R123 normal heated command-removal controls: dismantling below capacity has no blast.
        // Always complete vanilla removal so caches and inventories cannot outlive the block.
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide() && !state.is(newState.getBlock()))
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
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

