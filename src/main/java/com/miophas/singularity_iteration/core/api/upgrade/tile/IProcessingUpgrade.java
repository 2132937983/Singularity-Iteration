package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Processing upgrade interface.
 * <p>
 * Equivalent to {@code IProcessingUpgrade} in IC2 1.12.2.
 * An upgrade that implements this interface can change the processing speed and energy use of the machine.
 */
public interface IProcessingUpgrade extends IUpgradeItem {

    /**
     * Gets the additional processing time (ticks).
     * <p>
     * A positive value makes the processing time longer. A negative value makes it shorter.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the additional processing time (ticks)
     */
    int getExtraProcessTime(ItemStack stack, IUpgradableBlock block);

    /**
     * Gets the processing time multiplier.
     * <p>
     * 1.0 means no change. 0.5 means double speed. 2.0 means half speed.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the processing time multiplier
     */
    double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock block);

    /**
     * Gets the additional energy demand per tick (EU).
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the additional energy demand (EU/tick)
     */
    long getExtraEnergyDemand(ItemStack stack, IUpgradableBlock block);

    /**
     * Gets the energy demand multiplier.
     * <p>
     * 1.0 means no change. 2.0 means double energy use.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the energy demand multiplier
     */
    double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock block);
}