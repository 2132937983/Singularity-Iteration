package com.miophas.singularity_iteration.core.prefab.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Gas fluid flowing implementation.
 *
 * <p>This class extends {@link BaseFlowingFluid.Flowing}. Use it together with {@link GasFluidSource}.
 * It has the same behavior: it moves up, disappears at the height limit, and spreads horizontally.
 *
 * <p>For usage, see the documentation of {@link GasFluidSource}.
 *
 * @see GasFluidSource
 */
public class GasFluidFlowing extends BaseFlowingFluid.Flowing {

    private final int maxHeight;

    /**
     * Creates the flowing variant of a gas fluid.
     *
     * @param properties the fluid properties (must be the same Properties as the related {@link GasFluidSource})
     * @param maxHeight  the maximum height. It must be the same as in the related {@link GasFluidSource}.
     */
    public GasFluidFlowing(BaseFlowingFluid.Properties properties, int maxHeight) {
        super(properties);
        this.maxHeight = maxHeight;
    }

    /**
     * Gets the maximum height at which the gas disappears.
     *
     * @return the maximum Y coordinate
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