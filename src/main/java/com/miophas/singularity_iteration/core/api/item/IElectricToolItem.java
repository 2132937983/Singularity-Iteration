package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for electric tool functions.
 * All tools that use electricity to work should implement this interface.
 */
public interface IElectricToolItem extends IBatteryItem {

    /**
     * Gets the energy that one use consumes, in EU.
     */
    long getEnergyPerUse();

    /**
     * Gets the tool tier.
     */
    int getToolTier();

    /**
     * Checks if the stack has sufficient energy for one use.
     */
    default boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerUse();
    }
}
