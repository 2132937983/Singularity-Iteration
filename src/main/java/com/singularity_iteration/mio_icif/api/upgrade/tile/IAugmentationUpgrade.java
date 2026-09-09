package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 增强升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IAugmentationUpgrade}。
 * 实现此接口的升级可以为机器提供额外的增强效果。
 * 这是一个通用的增强接口，具体的增强效果由实现类定义。
 */
public interface IAugmentationUpgrade extends IUpgradeItem {

    /**
     * 获取此升级提供的增强值。
     * <p>
     * 增强值的含义取决于具体实现：
     * <ul>
     *   <li>对于超频升级：增强值表示额外的处理速度</li>
     *   <li>对于储能升级：增强值表示额外的能量容量</li>
     *   <li>对于其他升级：增强值由实现者自定义</li>
     * </ul>
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 增强值
     */
    int getAugmentation(ItemStack stack, IUpgradableBlock block);
}
