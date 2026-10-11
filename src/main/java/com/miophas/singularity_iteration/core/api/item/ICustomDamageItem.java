package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for items with custom durability.
 * An item that implements this interface can use a custom durability system instead of vanilla item durability.
 * Based on ICustomDamageItem from IC2.
 */
public interface ICustomDamageItem {
    /**
     * Gets the current custom damage value of the item.
     */
    int getCustomDamage(ItemStack stack);

    /**
     * Sets the custom damage value of the item.
     */
    void setCustomDamage(ItemStack stack, int damage);

    /**
     * Gets the maximum custom damage of the item.
     */
    int getMaxCustomDamage(ItemStack stack);

    /**
     * Applies custom durability damage to the item.
     * <p>
     * This is the equivalent of {@code applyCustomDamage} in IC2 1.12.2.
     * The item calls this method when it is used or damaged.
     * The default implementation adds the damage to the current value and does not check the maximum.
     *
     * @param stack the item stack
     * @param damage the damage value to apply
     * @param entity the entity that uses the item (can be null)
     * @return true if the damage was applied
     */
    default boolean applyCustomDamage(ItemStack stack, int damage, net.minecraft.world.entity.LivingEntity entity) {
        setCustomDamage(stack, getCustomDamage(stack) + damage);
        return true;
    }
}