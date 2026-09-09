package com.singularity_iteration.mio_icif.api.util;

import net.minecraft.world.item.ItemStack;

/**
 * 工具箱存储接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IBoxable}。
 * 实现此接口的物品可以被存放在工具箱中。
 */
public interface IBoxable {

    /**
     * 检查此物品是否可以存放在工具箱中。
     *
     * @param stack 物品堆
     * @return 如果可以存放则返回 true
     */
    boolean canBeStoredInToolbox(ItemStack stack);
}
