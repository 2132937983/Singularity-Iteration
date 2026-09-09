package com.singularity_iteration.mio_icif.Blocks.reactor;

import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 流体核反应堆红石端口方块
 *
 * 功能量?
 * - 用于流体核反应堆多方块结构中接收红石信号
 * - 可以放置拉杆、按钮等红石控制元件来激�?关闭反应�?
 * - 根据接收到的红石信号强度控制反应堆的启停
 * - �?GUI，纯红石控制接口
 * - 方块状态会随红石信号变化（用于视觉反馈�?
 */
@SuppressWarnings("null")
public class mio_icif_block_reactor_redstone_port extends Block {

    public static final MapCodec<mio_icif_block_reactor_redstone_port> CODEC = simpleCodec(mio_icif_block_reactor_redstone_port::new);

    // 红石信号激活状态属性?
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    @Override
    public MapCodec<mio_icif_block_reactor_redstone_port> codec() {
        return CODEC;
    }

    public mio_icif_block_reactor_redstone_port(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        // 通知多方块结构管理器
        if (!context.getLevel().isClientSide()) {
            mio_icif_multiblock_manager.notifyBlockChanged(context.getLevel(), context.getClickedPos());
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean isPowered = state.getValue(POWERED);
            boolean hasSignal = level.hasNeighborSignal(pos);

            if (isPowered != hasSignal) {
                level.setBlock(pos, state.setValue(POWERED, hasSignal), 3);
                // 通知相邻的反应堆方块红石状态变化?
                notifyReactorRedstoneChange(level, pos, hasSignal);
            }
        }
    }

    /**
     * 通知反应堆红石信号状态变化?
     * 查找附近的核反应堆并触发更新
     */
    private void notifyReactorRedstoneChange(Level level, BlockPos pos, boolean powered) {
        // 查找附近的核反应堆（�?x5x5范围内）
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor) {
                        // 触发反应堆更新（通过标记更改�?
                        reactor.setChanged();
                    }
                }
            }
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return false;
    }

    /**
     * 获取当前红石信号强度
     */
    public int getRedstoneSignal(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(POWERED) ? 15 : 0;
    }

    /**
     * 检查是否接收到红石信号
     */
    public boolean isReceivingRedstoneSignal(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(POWERED);
    }
}


