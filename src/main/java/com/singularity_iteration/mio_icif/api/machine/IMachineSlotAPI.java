package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 机器槽位 API
 *
 * <p>提供机器槽位布局查询和物品操作功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IMachineSlotAPI {

    /**
     * 获取机器的槽位布局信息
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 槽位布局，如果该位置不是机器或布局信息不可用则返回空 Optional
     */
    Optional<ISlotLayout> getMachineSlotLayout(Level world, BlockPos pos);

    /**
     * 获取机器指定槽位的物品
     *
     * @param world 世界
     * @param pos 方块位置
     * @param slot 槽位索引
     * @return 槽位中的物品，无效槽位返回空 ItemStack
     */
    ItemStack getMachineSlotItem(Level world, BlockPos pos, int slot);

    /**
     * 设置机器指定槽位的物品
     *
     * @param world 世界
     * @param pos 方块位置
     * @param slot 槽位索引
     * @param stack 要设置的物品
     * @return 是否成功设置
     */
    boolean setMachineSlotItem(Level world, BlockPos pos, int slot, ItemStack stack);

    /**
     * 获取机器的总槽位数
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 总槽位数
     */
    int getMachineSlotCount(Level world, BlockPos pos);

    /**
     * 获取机器的工作进度
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 当前进度 tick，非工作机器返回 0
     * @deprecated 请使用 {@link IMachineControlAPI#getProgress(Level, BlockPos)}
     */
    @Deprecated
    int getMachineProgress(Level world, BlockPos pos);

    /**
     * 获取机器完成工作所需的总进度
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 总进度 tick
     * @deprecated 请使用 {@link IMachineControlAPI#getMaxProgress(Level, BlockPos)}
     */
    @Deprecated
    int getMachineMaxProgress(Level world, BlockPos pos);

    /**
     * 获取机器每 tick 的能量消耗
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 每 tick 消耗的能量 (EU)
     * @deprecated 该方法语义上属于控制/查询接口，建议使用 {@link IProducerBlock#getEnergyPerTick()}
     */
    @Deprecated
    long getMachineEnergyPerTick(Level world, BlockPos pos);
}
