package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for augmentation upgrades.
 * <p>
 * It is the equivalent of {@code IAugmentationUpgrade} in IC2 1.12.2.
 * Upgrades that implement this interface give more augmentation effects to a machine.
 * This is a general augmentation interface. The implementation class defines the specific effect.
 */
public interface IAugmentationUpgrade extends IUpgradeItem {

    /**
     * Returns the augmentation value of this upgrade.
     * <p>
     * The meaning of the value depends on the implementation:
     * <ul>
     *   <li>for an overclocker upgrade: the value is the additional process speed</li>
     *   <li>for an energy storage upgrade: the value is the additional energy capacity</li>
     *   <li>for other upgrades: the implementer defines the value</li>
     * </ul>
     *
     * @param stack the upgrade item stack
     * @param block the machine that has this upgrade installed
     * @return the augmentation value
     */
    int getAugmentation(ItemStack stack, IUpgradableBlock block);
}
