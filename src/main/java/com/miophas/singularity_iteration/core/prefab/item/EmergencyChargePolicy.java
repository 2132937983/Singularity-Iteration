package com.miophas.singularity_iteration.core.prefab.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 放电槽里可以当作一次性能源使用的原版物品策略。
 *
 * <p>默认把红石当作应急燃料，与迁移前的行为一致；宿主可在装配阶段替换，
 * 使 core 的槽位/充放电逻辑不写死某一种物品。
 */
public final class EmergencyChargePolicy {

    public static final long DEFAULT_ENERGY_VALUE = 800L;
    public static final int DEFAULT_STACK_LIMIT = 64;

    private static Item item = Items.REDSTONE;
    private static long energyValue = DEFAULT_ENERGY_VALUE;
    private static int stackLimit = DEFAULT_STACK_LIMIT;

    private EmergencyChargePolicy() {}

    public static synchronized void configure(Item emergencyItem, long euPerItem, int maxStack) {
        if (emergencyItem == null) throw new IllegalArgumentException("emergency item");
        if (euPerItem <= 0 || maxStack <= 0) throw new IllegalArgumentException("emergency charge settings");
        item = emergencyItem;
        energyValue = euPerItem;
        stackLimit = maxStack;
    }

    public static synchronized void reset() {
        item = Items.REDSTONE;
        energyValue = DEFAULT_ENERGY_VALUE;
        stackLimit = DEFAULT_STACK_LIMIT;
    }

    public static synchronized boolean applies(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == item;
    }

    public static synchronized long energyPerItem() {
        return energyValue;
    }

    public static synchronized int stackLimit() {
        return stackLimit;
    }

    /** 放电槽对这种应急燃料放宽堆叠上限，其余放电物品保持单件限制。 */
    public static synchronized int stackLimitFor(ItemStack stack, int defaultLimit) {
        return applies(stack) ? stackLimit : defaultLimit;
    }
}
