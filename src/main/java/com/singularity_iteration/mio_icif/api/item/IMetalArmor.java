package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 金属护甲接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IMetalArmor}。
 * 实现此接口的物品可以被识别为金属护甲，
 * 用于计算金属护甲的防护值和电力护甲的充电效率。
 */
public interface IMetalArmor {

    /**
     * 检查此物品是否是金属护甲。
     *
     * @param stack 物品堆
     * @param player 穿戴此护甲的玩家
     * @return 如果是金属护甲则返回 true
     */
    boolean isMetalArmor(ItemStack stack, Player player);
}
