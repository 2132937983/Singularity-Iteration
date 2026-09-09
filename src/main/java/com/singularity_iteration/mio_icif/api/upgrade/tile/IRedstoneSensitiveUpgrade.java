package com.singularity_iteration.mio_icif.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * 红石敏感升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IRedstoneSensitiveUpgrade}。
 * 实现此接口的升级可以修改机器对红石信号的响应行为。
 * 例如：红石反向器升级可以让机器在有红石信号时停止工作。
 */
public interface IRedstoneSensitiveUpgrade extends IUpgradeItem {

    /**
     * 检查此升级是否修改红石输入。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 如果修改红石输入则返回 true
     */
    boolean modifiesRedstoneInput(ItemStack stack, IUpgradableBlock block);

    /**
     * 获取修改后的红石输入值。
     * <p>
     * 返回值含义：
     * <ul>
     *   <li>0 = 无红石信号（等同于无信号）</li>
     *   <li>1-15 = 红石信号强度</li>
     * </ul>
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @param currentInput 当前红石信号强度
     * @return 修改后的红石信号强度
     */
    int getRedstoneInput(ItemStack stack, IUpgradableBlock block, int currentInput);
}
