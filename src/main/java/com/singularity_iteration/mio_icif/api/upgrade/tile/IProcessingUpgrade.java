package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 处理升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IProcessingUpgrade}。
 * 实现此接口的升级可以改变机器的处理速度和能量消耗。
 */
public interface IProcessingUpgrade extends IUpgradeItem {

    /**
     * 获取额外的处理时间（tick）。
     * <p>
     * 返回正值会延长处理时间，负值会缩短处理时间。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 额外处理时间（tick）
     */
    int getExtraProcessTime(ItemStack stack, IUpgradableBlock block);

    /**
     * 获取处理时间乘数。
     * <p>
     * 1.0 表示无变化，0.5 表示速度翻倍，2.0 表示速度减半。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 处理时间乘数
     */
    double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock block);

    /**
     * 获取额外的每 tick 能量需求（EU）。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 额外能量需求（EU/tick）
     */
    long getExtraEnergyDemand(ItemStack stack, IUpgradableBlock block);

    /**
     * 获取能量需求乘数。
     * <p>
     * 1.0 表示无变化，2.0 表示能耗翻倍。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 能量需求乘数
     */
    double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock block);
}