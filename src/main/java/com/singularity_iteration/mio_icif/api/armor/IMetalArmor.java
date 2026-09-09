package com.singularity_iteration.mio_icif.api.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * 金属装甲接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IMetalArmor}。
 * 实现此接口的物品是导电的金属装甲，在受到电击时会传导伤害。
 */
public interface IMetalArmor {

    /**
     * 检查此物品是否为金属装甲。
     * <p>
     * 金属装甲会传导电流，穿着者在接触带电方块时会受到电击伤害。
     *
     * @param stack  物品堆
     * @param entity 穿着实体
     * @return 如果是金属装甲则返回 true
     */
    boolean isMetalArmor(ItemStack stack, Entity entity);
}