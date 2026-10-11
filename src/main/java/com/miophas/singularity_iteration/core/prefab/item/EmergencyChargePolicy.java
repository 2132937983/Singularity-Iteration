package com.miophas.singularity_iteration.core.prefab.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Policy for the vanilla item that a discharge slot can use as a single-use energy source.
 *
 * <p>The default is redstone as emergency fuel. This is the same as the behavior before the migration.
 * The host can replace it during setup, so the slot and charge logic of core does not hard-code one item.
 * All methods are synchronized and are safe to call from any thread.
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

    /**
     * The discharge slot allows a larger stack size for the emergency fuel.
     * Other discharge items keep the default limit.
     */
    public static synchronized int stackLimitFor(ItemStack stack, int defaultLimit) {
        return applies(stack) ? stackLimit : defaultLimit;
    }
}
