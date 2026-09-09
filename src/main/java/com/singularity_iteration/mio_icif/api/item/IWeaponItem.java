package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 武器物品接口。
 *
 * <p>扩展 {@link IBatteryItem}，为电动武器提供统一的 API。
 * 适用于电枪、激光武器、等离子武器等消耗能量的武器。
 */
public interface IWeaponItem extends IBatteryItem {

    /**
     * 获取武器基础伤害值
     * @return 伤害值
     */
    float getDamage();

    /**
     * 获取武器有效射程（方块数）
     * @return 射程
     */
    float getRange();

    /**
     * 获取每次射击消耗的能量
     * @return 每次射击的能耗（EU）
     */
    long getEnergyPerShot();

    /**
     * 检查是否为远程武器
     * @return true 如果是远程武器
     */
    boolean isRanged();

    /**
     * 获取武器射速（tick/发）
     * @return 射速间隔
     */
    default int getFireRate() {
        return 20;
    }

    /**
     * 获取武器精准度（0.0 - 1.0）
     * @return 精准度
     */
    default float getAccuracy() {
        return 1.0F;
    }

    /**
     * 检查武器是否有足够能量射击
     * @param stack 物品堆
     * @return true 如果能量足够
     */
    default boolean canShoot(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerShot();
    }

    /**
     * 消耗射击能量
     * @param stack 物品堆
     * @return true 如果成功消耗
     */
    default boolean shoot(ItemStack stack) {
        if (canShoot(stack)) {
            extractEnergy(stack, getEnergyPerShot());
            return true;
        }
        return false;
    }
}
