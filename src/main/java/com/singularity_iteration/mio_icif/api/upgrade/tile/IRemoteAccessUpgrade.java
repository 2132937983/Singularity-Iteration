package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 远程访问升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IRemoteAccessUpgrade}。
 * 实现此接口的升级可以让机器支持远程访问或扩展操作范围。
 */
public interface IRemoteAccessUpgrade extends IUpgradeItem {

    /**
     * 获取范围放大倍数。
     * <p>
     * 返回值加到机器的基础远程访问范围上。
     * 例如：返回 16 表示远程访问范围增加 16 格。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @param baseRange 基础远程访问范围
     * @return 放大后的范围
     */
    int getRangeAmplification(ItemStack stack, IUpgradableBlock block, int baseRange);
}
