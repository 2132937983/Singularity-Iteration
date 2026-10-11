package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for solar helmets.
 *
 * <p>It extends {@link IBatteryItem} and gives one API for solar helmets and armor.
 * A solar helmet automatically charges itself or other equipment during the day (or when the light conditions are met).
 */
public interface ISolarHelmetItem extends IBatteryItem {

    /**
     * Returns the solar generation rate.
     *
     * @return the energy generated each tick (EU)
     */
    long getGenerationRate();

    /**
     * Returns true if the helmet needs a view of the sky (no blocks above it).
     *
     * @return true if a view of the sky is necessary
     */
    boolean requiresSky();

    /**
     * Returns true if the helmet works only during the day.
     *
     * @return true if it generates power only during the day
     */
    boolean isDayOnly();

    /**
     * Returns the minimum light level that is necessary (0-15).
     *
     * @return the minimum light level
     */
    default int getMinLightLevel() {
        return 0;
    }

    /**
     * Returns true if the current conditions permit power generation.
     *
     * @param stack     the item stack
     * @param skyLight  the sky light level (0-15)
     * @param blockLight the block light level (0-15)
     * @param canSeeSky true if the sky is visible
     * @param isDaytime true if it is day
     * @return true if the conditions for generation are met
     */
    default boolean canGenerate(ItemStack stack, int skyLight, int blockLight, boolean canSeeSky, boolean isDaytime) {
        if (requiresSky() && !canSeeSky) return false;
        if (isDayOnly() && !isDaytime) return false;
        int light = requiresSky() ? skyLight : blockLight;
        return light >= getMinLightLevel();
    }

    /**
     * Charges a target item with solar energy for the player who wears this helmet.
     * The amount is at most {@link #getGenerationRate()}.
     *
     * @param stack           the item stack
     * @param chargeTarget    the item stack to charge
     * @param simulate        if true, only simulate
     * @return the energy that was added (EU)
     */
    default long chargeTarget(ItemStack stack, ItemStack chargeTarget, boolean simulate) {
        if (chargeTarget.isEmpty()) return 0;
        if (chargeTarget.getItem() instanceof IBatteryItem battery) {
            long rate = getGenerationRate();
            if (simulate) {
                long current = battery.getEnergy(chargeTarget);
                long max = battery.getMaxEnergy();
                return Math.min(rate, max - current);
            }
            return battery.addEnergy(chargeTarget, rate);
        }
        return 0;
    }
}
