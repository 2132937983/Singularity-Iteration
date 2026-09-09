package com.singularity_iteration.mio_icif.api.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 物品 HUD 信息接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IItemHudInfo}。
 * 实现此接口的物品可以在 HUD 上显示额外信息。
 */
public interface IItemHudInfo {

    /**
     * 获取此物品的 HUD 信息行。
     *
     * @param stack    物品堆
     * @param extended 是否显示扩展信息
     * @return HUD 信息行列表
     */
    List<String> getHudInfo(ItemStack stack, boolean extended);

    /**
     * HUD 能量条提供者接口。
     * <p>
     * 对应 IC2 1.12.2 的 {@code IItemHudProvider.IItemHudBarProvider}。
     * 实现此接口的物品可以在 HUD 上显示能量条。
     */
    interface IItemHudBarProvider {
        /**
         * 获取能量条百分比（0-100）。
         *
         * @param stack 物品堆
         * @return 能量条百分比
         */
        int getBarPercent(ItemStack stack);
    }
}
