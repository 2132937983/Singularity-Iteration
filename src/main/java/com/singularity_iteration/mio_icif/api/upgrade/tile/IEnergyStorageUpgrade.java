package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 能量存储升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergyStorageUpgrade}。
 * 实现此接口的升级可以增加机器的储能容量。
 */
public interface IEnergyStorageUpgrade extends IUpgradeItem {

    /**
     * 获取额外的储能容量（EU）。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 额外储能容量（EU）
     */
    long getExtraEnergyStorage(ItemStack stack, IUpgradableBlock block);

    /**
     * 获取储能容量乘数。
     * <p>
     * 1.0 表示无变化，2.0 表示容量翻倍。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 储能容量乘数
     */
    double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock block);
}