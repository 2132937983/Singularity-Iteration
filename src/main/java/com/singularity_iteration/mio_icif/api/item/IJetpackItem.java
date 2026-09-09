package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 喷气背包物品接口。
 *
 * <p>扩展 {@link IBatteryItem}，为喷气背包提供统一的 API。
 * 喷气背包允许玩家飞行，消耗能量维持飞行状态。
 */
public interface IJetpackItem extends IBatteryItem {

    /**
     * 喷气背包模式枚举
     */
    enum JetpackMode {
        /** 关闭 */
        OFF,
        /** 普通模式（仅提升跳跃） */
        NORMAL,
        /** 悬浮模式（按住跳跃键悬浮） */
        HOVER,
        /** 飞行模式（自由飞行） */
        FLIGHT
    }

    /**
     * 获取喷气背包推力
     * @return 推力值
     */
    float getThrust();

    /**
     * 获取每 tick 飞行消耗的能量
     * @return 每 tick 能耗（EU）
     */
    long getEnergyPerTickFlying();

    /**
     * 获取当前喷气背包模式
     * @param stack 物品堆
     * @return 当前模式
     */
    JetpackMode getMode(ItemStack stack);

    /**
     * 设置喷气背包模式
     * @param stack 物品堆
     * @param mode  目标模式
     */
    void setMode(ItemStack stack, JetpackMode mode);

    /**
     * 获取喷气背包的最大飞行高度（方块数）
     * 返回 -1.0F 表示无高度限制（可飞至世界顶端）
     * @return 最大高度，-1 表示无限制
     */
    default float getMaxHeight() {
        return -1.0F;
    }

    /**
     * 是否有飞行高度限制
     * 量子胸甲等高级装备应覆盖此方法返回 false 以取消高度限制
     * @return true 如果有高度限制
     */
    default boolean hasHeightLimit() {
        return getMaxHeight() > 0;
    }

    /**
     * 获取悬浮模式下的目标高度偏移
     * @return 高度偏移
     */
    default float getHoverHeightOffset() {
        return 1.0F;
    }

    /**
     * 检查是否有足够能量飞行一 tick
     * @param stack 物品堆
     * @return true 如果能量足够
     */
    default boolean canFlyTick(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerTickFlying();
    }

    /**
     * 消耗一 tick 的飞行能量
     * @param stack 物品堆
     * @return true 如果成功消耗
     */
    default boolean consumeFlyTick(ItemStack stack) {
        if (canFlyTick(stack)) {
            extractEnergy(stack, getEnergyPerTickFlying());
            return true;
        }
        return false;
    }
}