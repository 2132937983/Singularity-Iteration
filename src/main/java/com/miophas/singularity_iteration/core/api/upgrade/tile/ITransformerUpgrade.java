package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for transformer upgrades.
 * <p>
 * Matches {@code ITransformerUpgrade} of IC2 1.12.2.
 * An upgrade that implements this interface can increase the input voltage tier of the machine.
 */
public interface ITransformerUpgrade extends IUpgradeItem {

    /**
     * Gets the extra voltage tiers.
     * <p>
     * The return value is added to the base voltage tier of the machine.
     * Example: the machine base tier is 1 (LV) and the upgrade returns 1. Then the machine accepts MV (tier 2) input.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return the number of extra voltage tiers
     */
    int getExtraTier(ItemStack stack, IUpgradableBlock block);
}