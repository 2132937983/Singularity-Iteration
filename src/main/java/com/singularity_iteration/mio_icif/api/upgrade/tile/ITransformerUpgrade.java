package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 变压器升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code ITransformerUpgrade}。
 * 实现此接口的升级可以提高机器的输入电压等级。
 */
public interface ITransformerUpgrade extends IUpgradeItem {

    /**
     * 获取额外的电压等级。
     * <p>
     * 返回值加到机器的基础电压等级上。
     * 例如：机器基础等级为 1 (LV)，升级返回 1，则机器可以接受 MV (等级 2) 的输入。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 额外电压等级
     */
    int getExtraTier(ItemStack stack, IUpgradableBlock block);
}