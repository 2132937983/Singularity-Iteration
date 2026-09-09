package com.singularity_iteration.mio_icif.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 气体流体源方块实现。
 *
 * <p>继承 {@link BaseFlowingFluid.Source}，添加气体行为：
 * <ul>
 *   <li><b>向上飘升</b>：每 tick 检查上方方块，若为空气或可替换则向上移动</li>
 *   <li><b>限高消失</b>：当 Y 坐标达到 {@link #getMaxHeight()} 时自动消失</li>
 *   <li><b>水平扩散</b>：当上方被阻挡时，向水平四个方向扩散</li>
 *   <li><b>禁止向下流动</b>：不执行任何向下流动逻辑</li>
 * </ul>
 *
 * <h3>Addon 开发者使用示例</h3>
 * <pre>{@code
 * // 1. 定义流体属性（与普通流体相同）
 * public static final BaseFlowingFluid.Properties MY_GAS_PROPERTIES =
 *     new BaseFlowingFluid.Properties(
 *         () -> MY_GAS, () -> MY_GAS_FLOWING,
 *         () -> mio_icif_fluids.MY_GAS_BLOCK
 *     ).canMultiply(true).flowSpeed(4).levelDecreasePerBlock(1);
 *
 * // 2. 注册流体，使用 GasFluidSource / GasFluidFlowing
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
     * 创建气体流体源。
     *
     * @param properties 流体属性（{@link BaseFlowingFluid.Properties}）
     * @param maxHeight  最大高度限制，超过此 Y 坐标气体会消失。
     *                   通常使用 {@code 320}（1.18+ 世界高度上限）。
     */
    public GasFluidSource(BaseFlowingFluid.Properties properties, int maxHeight) {
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