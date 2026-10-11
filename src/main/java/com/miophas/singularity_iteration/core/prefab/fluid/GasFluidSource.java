package com.miophas.singularity_iteration.core.prefab.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Gas fluid source implementation.
 *
 * <p>This class extends {@link BaseFlowingFluid.Source} and adds gas behavior:
 * <ul>
 *   <li><b>Move up</b>: on each tick, it checks the block above. If that block is air or replaceable, the gas moves up.</li>
 *   <li><b>Disappear at the height limit</b>: when the Y coordinate reaches {@link #getMaxHeight()}, the gas disappears.</li>
 *   <li><b>Spread horizontally</b>: when the block above blocks it, the gas spreads in the four horizontal directions.</li>
 *   <li><b>No downward flow</b>: the gas never flows down.</li>
 * </ul>
 *
 * <h3>Example for addon developers</h3>
 * <pre>{@code
 * // 1. Define the fluid properties (the same as for a normal fluid)
 * public static final BaseFlowingFluid.Properties MY_GAS_PROPERTIES =
 *     new BaseFlowingFluid.Properties(
 *         () -> MY_GAS, () -> MY_GAS_FLOWING,
 *         () -> mio_icif_fluids.MY_GAS_BLOCK
 *     ).canMultiply(true).flowSpeed(4).levelDecreasePerBlock(1);
 *
 * // 2. Register the fluids with GasFluidSource / GasFluidFlowing
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS = FLUIDS.register("my_gas",
 *     () -> new GasFluidSource(MY_GAS_PROPERTIES, 320));
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS_FLOWING = FLUIDS.register("my_gas_flowing",
 *     () -> new GasFluidFlowing(MY_GAS_PROPERTIES, 320));
 * }</pre>
 *
 * @see GasFluidFlowing
 */
public class GasFluidSource extends BaseFlowingFluid.Source {

    private final int maxHeight;

    /**
     * Creates a gas fluid source.
     *
     * @param properties the fluid properties ({@link BaseFlowingFluid.Properties})
     * @param maxHeight  the maximum height. At this Y coordinate or higher, the gas disappears.
     *                   Usually {@code 320} (the world height limit in 1.18 and later).
     */
    public GasFluidSource(BaseFlowingFluid.Properties properties, int maxHeight) {
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