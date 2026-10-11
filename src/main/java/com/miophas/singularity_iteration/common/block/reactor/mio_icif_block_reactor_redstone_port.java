package com.miophas.singularity_iteration.common.block.reactor;

import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;
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
 * Function:
 * - It receives redstone signals for the fluid nuclear reactor multiblock structure.
 * - Players can place levers, buttons, or other redstone controls on it to start or stop the reactor.
 * - The received redstone signal controls whether the reactor runs.
 * - It has no GUI. It is a redstone-only control interface.
 * - The block state changes with the redstone signal to give visual feedback.
 */
@SuppressWarnings("null")
public class mio_icif_block_reactor_redstone_port extends Block {

    public static final MapCodec<mio_icif_block_reactor_redstone_port> CODEC = simpleCodec(mio_icif_block_reactor_redstone_port::new);

    // Property that shows whether a redstone signal is active.
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
                // Tell the adjacent reactor blocks that the redstone state changed.
                notifyReactorRedstoneChange(level, pos, hasSignal);
            }
        }
    }

    /**
     * Tells the reactor that the redstone signal state changed.
     * Finds the nearby nuclear reactor and starts an update.
     */
    private void notifyReactorRedstoneChange(Level level, BlockPos pos, boolean powered) {
        // Find nuclear reactors nearby (in a 5x5x5 area).
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor) {
                        // Mark the reactor as changed to trigger an update.
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


