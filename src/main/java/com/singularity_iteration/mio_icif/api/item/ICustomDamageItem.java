package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 自定义耐久度物品接口
 * 实现此接口的物品可以使用自定义耐久度系统而非原版物品耐久度。
 * 参考 IC2 的 ICustomDamageItem
 */
public interface ICustomDamageItem {
    /**
     * 获取物品当前自定义耐久度值
     */
    int getCustomDamage(ItemStack stack);

    /**
     * 设置物品自定义耐久度值
     */
    void setCustomDamage(ItemStack stack, int damage);

    /**
     * 获取物品最大自定义耐久度
     */
    int getMaxCustomDamage(ItemStack stack);

    /**
     * 对物品应用自定义耐久度伤害。
     * <p>
     * 对应 IC2 1.12.2 的 {@code applyCustomDamage}。
     * 此方法在物品被使用或受到伤害时调用。
     *
     * @param stack 物品堆
     * @param damage 要应用的伤害值
     * @param entity 使用物品的实体（可能为 null）
     * @return 如果伤害被成功应用则返回 true
     */
    default boolean applyCustomDamage(ItemStack stack, int damage, net.minecraft.world.entity.LivingEntity entity) {
        setCustomDamage(stack, getCustomDamage(stack) + damage);
        return true;
    }
}