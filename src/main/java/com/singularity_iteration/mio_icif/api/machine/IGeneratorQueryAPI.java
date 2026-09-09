package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 发电机查询 API
 *
 * <p>提供发电机燃料、燃烧状态和能量生成的查询功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IGeneratorQueryAPI {

    /**
     * 检查发电机是否正在燃烧
     */
    boolean isGeneratorBurning(Level world, BlockPos pos);

    /**
     * 获取发电机当前燃料的剩余燃烧时间
     *
     * @return 剩余燃烧时间 (tick)
     */
    int getGeneratorBurnTime(Level world, BlockPos pos);

    /**
     * 获取发电机当前燃料的总燃烧时间
     *
     * @return 总燃烧时间 (tick)
     */
    int getGeneratorBurnDuration(Level world, BlockPos pos);

    /**
     * 获取发电机的能量生成速率
     *
     * @return 生成速率 (EU/tick)
     */
    long getGeneratorEnergyRate(Level world, BlockPos pos);

    /**
     * 获取发电机燃料槽的物品
     */
    ItemStack getGeneratorFuelSlot(Level world, BlockPos pos);

    /**
     * 设置发电机燃料槽的物品
     *
     * @return 是否成功设置
     */
    boolean setGeneratorFuelSlot(Level world, BlockPos pos, ItemStack stack);
}
