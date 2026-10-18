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
        // 0.1.10.5: the cost after Energy Saving, as shoot() takes it
        return getEnergy(stack) >= EnergySaving.maxCost(stack, getEnergyPerShot());
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

    /**
     * Fires one shot for a shooter that is not a player (0.1.8.4; for example an Armory Wingman or
     * a turret). The weapon pays the shot from its own charge, spawns its projectile from the
     * shooter's eye along {@code direction}, and plays its sound and effects. Server side only.
     *
     * <p>The default does nothing: weapons that cannot fire without a player return -1.
     *
     * @param stack     the weapon stack held by the shooter
     * @param shooter   the shooter (owner of the projectile)
     * @param direction aim direction (any length; normalised by the weapon)
     * @param mainHand  true if the weapon is in the main hand
     * @return ticks until the weapon can fire again, or -1 if it did not fire (no energy, no
     *         ammunition, not supported)
     */
    default int fireFrom(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter, net.minecraft.world.phys.Vec3 direction, boolean mainHand) {
        return -1;
    }

    /**
     * Distance band (blocks) in which an AI shooter should keep its target, as {min, max}
     * (0.1.8.4). The default is derived from {@link #getRange()}: 40 % to 80 % of it, at least 6
     * to 10 blocks and at most 18 to 28 blocks.
     *
     * @return {min, max} in blocks
     */
    default double[] engagementRange() {
        double r = Math.max(10, getRange());
        return new double[]{Math.max(6, Math.min(18, r * 0.4)), Math.max(10, Math.min(28, r * 0.8))};
    }

    /**
     * Projectile speed in blocks per tick for aim prediction by an AI shooter (0.1.8.4). 0 means
     * hitscan or unknown: aim at the current position.
     *
     * @return speed in blocks per tick
     */
    default double projectileSpeed() {
        return 0;
    }
}
