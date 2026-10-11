package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Remote access upgrade interface.
 * <p>
 * Equivalent to {@code IRemoteAccessUpgrade} in IC2 1.12.2.
 * An upgrade that implements this interface lets a machine use remote access or a larger operation range.
 */
public interface IRemoteAccessUpgrade extends IUpgradeItem {

    /**
     * Gets the range increase.
     * <p>
     * The machine adds the return value to its base remote access range.
     * Example: a return value of 16 increases the remote access range by 16 blocks.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @param baseRange the base remote access range, in blocks
     * @return the increased range, in blocks
     */
    int getRangeAmplification(ItemStack stack, IUpgradableBlock block, int baseRange);
}
