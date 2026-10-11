package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for energy storage upgrades.
 * <p>
 * Matches {@code IEnergyStorageUpgrade} of IC2 1.12.2.
 * An upgrade that implements this interface can increase the energy capacity of the machine.
 */
public interface IEnergyStorageUpgrade extends IUpgradeItem {

    /**
     * Gets the extra energy capacity (EU).
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the extra energy capacity (EU)
     */
    long getExtraEnergyStorage(ItemStack stack, IUpgradableBlock block);

    /**
     * Gets the energy capacity multiplier.
     * <p>
     * 1.0 means no change. 2.0 doubles the capacity.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the energy capacity multiplier
     */
    double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock block);
}