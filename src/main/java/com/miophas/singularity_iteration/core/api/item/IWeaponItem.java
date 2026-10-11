package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for weapon items.
 *
 * <p>It extends {@link IBatteryItem} and gives one API for electric weapons.
 * Use it for weapons that use energy, such as electric guns, laser weapons and plasma weapons.
 */
public interface IWeaponItem extends IBatteryItem {

    /**
     * Returns the base damage of the weapon.
     *
     * @return the damage
     */
    float getDamage();

    /**
     * Returns the effective range of the weapon (in blocks).
     *
     * @return the range
     */
    float getRange();

    /**
     * Returns the energy that each shot uses.
     *
     * @return the energy for each shot (EU)
     */
    long getEnergyPerShot();

    /**
     * Returns true if this is a ranged weapon.
     *
     * @return true if it is a ranged weapon
     */
    boolean isRanged();

    /**
     * Returns the fire rate of the weapon (ticks for each shot).
     *
     * @return the interval between shots
     */
    default int getFireRate() {
        return 20;
    }

    /**
     * Returns the accuracy of the weapon (0.0 - 1.0).
     *
     * @return the accuracy
     */
    default float getAccuracy() {
        return 1.0F;
    }

    /**
     * Returns true if the weapon has sufficient energy for a shot.
     *
     * @param stack the item stack
     * @return true if the energy is sufficient
     */
    default boolean canShoot(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerShot();
    }

    /**
     * Uses the energy for one shot.
     *
     * @param stack the item stack
     * @return true if the energy was used
     */
    default boolean shoot(ItemStack stack) {
        if (canShoot(stack)) {
            extractEnergy(stack, EnergySaving.apply(stack, getEnergyPerShot()));
            return true;
        }
        return false;
    }
}
