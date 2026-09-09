package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 流体核反应堆流体端口方块
 *
 * 功能量? * - 用于流体核反应堆多方块结构中作为流体访问接口
 * - 提供 GUI 用于管理流体输入输出
 * - 包含一个空闲槽用于放置插件物品（未来扩展）
 * - 防爆性较强，能够抵御核反应堆爆炸
 */
@SuppressWarnings("null")
public class mio_icif_Block_Reactor_Fluid_Port extends BaseEntityBlock {

    public static final MapCodec<mio_icif_Block_Reactor_Fluid_Port> CODEC = simpleCodec(mio_icif_Block_Reactor_Fluid_Port::new);

    @Override
    public MapCodec<mio_icif_Block_Reactor_Fluid_Port> codec() {
        return CODEC;
    }

    public mio_icif_Block_Reactor_Fluid_Port(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_reactor_fluid_port fluidPort) {
                    mio_icif_reactor_fluid_port.tick(l, p, s, fluidPort);
                }
            } : null;
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_reactor_fluid_port fluidPort) {
                // 掉落槽位中的物品
                for (int i = 0; i < fluidPort.getItemHandler().getSlots(); i++) {
                    ItemStack stack = fluidPort.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Block.popResource(level, pos, stack);
                    }
                }
            }
            // 通知多方块结构管理器
            if (!level.isClientSide()) {
                mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


