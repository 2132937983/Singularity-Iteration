package com.singularity_iteration.mio_icif.api.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电钻工具接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IMiningDrill}。
 * 实现此接口的物品是电力驱动的采矿工具。
 */
public interface IMiningDrill {

    /**
     * 获取挖掘指定方块所需的能量消耗。
     *
     * @param stack 工具物品堆
     * @param world 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @return 能量消耗（EU）
     */
    long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);

    /**
     * 获取挖掘指定方块所需的 tick 数。
     *
     * @param stack 工具物品堆
     * @param world 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @return 所需 tick 数
     */
    int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);

    /**
     * 实际挖掘方块。
     * <p>
     * 此方法在能量充足时调用，执行实际的方块破坏逻辑。
     *
     * @param stack 工具物品堆
     * @param world 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @return 如果方块成功被破坏则返回 true
     */
    boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state);
}
