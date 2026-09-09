package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 电动工具功能接口。
 * 所有消耗电力运行的工具都应实现此接口。
 */
public interface IElectricToolItem extends IBatteryItem {

    /**
     * 获取每次使用的能量消耗 (EU)
     */
    long getEnergyPerUse();

    /**
     * 获取工具等级
     */
    int getToolTier();

    /**
     * 检查是否有足够能量使用一次
     */
    default boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerUse();
    }
}
