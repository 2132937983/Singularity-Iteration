package com.singularity_iteration.mio_icif.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 气体流体流动方块实现。
 *
 * <p>继承 {@link BaseFlowingFluid.Flowing}，与 {@link GasFluidSource} 配对使用，
 * 实现相同的向上飘升、限高消失和水平扩散行为。
 *
 * <p>使用方式请参见 {@link GasFluidSource} 的文档。
 *
 * @see GasFluidSource
 */
public class GasFluidFlowing extends BaseFlowingFluid.Flowing {

    private final int maxHeight;

    /**
     * 创建气体流体流动变体。
     *
     * @param properties 流体属性（必须与对应的 {@link GasFluidSource} 使用相同的 Properties）
     * @param maxHeight  最大高度限制，必须与对应的 {@link GasFluidSource} 一致
     */
    public GasFluidFlowing(BaseFlowingFluid.Properties properties, int maxHeight) {
        super(properties);
        this.maxHeight = maxHeight;
    }

    /**
     * 获取气体消失的最大高度。
     *
     * @return 最大 Y 坐标
     */
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void tick(Level level, BlockPos pos, FluidState fluidState) {
        if (level.isClientSide) return;

        if (pos.getY() >= maxHeight) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        FluidState aboveFluid = aboveState.getFluidState();

        if (aboveState.isAir() || (aboveFluid.isEmpty() && aboveState.canBeReplaced(this))) {
            level.setBlockAndUpdate(above, fluidState.createLegacyBlock());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        if (!aboveFluid.isEmpty() && aboveFluid.getType() == this) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}