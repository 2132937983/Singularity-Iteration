package com.miophas.singularity_iteration.core.api.upgrade.tile;

import java.util.Set;

/**
 * Upgradable block interface.
 * <p>
 * It is equivalent to {@code IUpgradableBlock} in IC2 1.12.2.
 * Upgrade items can improve machine blocks that implement this interface.
 */
public interface IUpgradableBlock {

    /**
     * Gets the energy that the block stores now (EU).
     *
     * @return the stored energy now (EU)
     */
    long getEnergy();

    /**
     * Uses the given quantity of energy from the block.
     *
     * @param amount the energy to use (EU)
     * @return true if the energy was used
     */
    boolean useEnergy(long amount);

    /**
     * Gets the set of upgrade properties that this machine supports.
     * <p>
     * An upgrade item uses {@link IUpgradeItem#isSuitableFor} to check if its type
     * is in this set. This decides if the upgrade is applicable to this machine.
     *
     * @return the set of supported upgrade properties
     */
    Set<UpgradableProperty> getUpgradableProperties();
}
