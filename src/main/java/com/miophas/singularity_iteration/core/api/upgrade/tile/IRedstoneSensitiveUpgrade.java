package com.miophas.singularity_iteration.core.api.upgrade.tile;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for redstone-sensitive upgrades.
 * <p>
 * Matches {@code IRedstoneSensitiveUpgrade} of IC2 1.12.2.
 * An upgrade that implements this interface can change how the machine responds to redstone signals.
 * Example: a redstone inverter upgrade makes the machine stop when it has a redstone signal.
 */
public interface IRedstoneSensitiveUpgrade extends IUpgradeItem {

    /**
     * Checks if this upgrade changes the redstone input.
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @return true if the upgrade changes the redstone input
     */
    boolean modifiesRedstoneInput(ItemStack stack, IUpgradableBlock block);

    /**
     * Gets the changed redstone input value.
     * <p>
     * Return values:
     * <ul>
     *   <li>0 = no redstone signal</li>
     *   <li>1-15 = the redstone signal strength</li>
     * </ul>
     *
     * @param stack the upgrade item stack
     * @param block the machine that contains this upgrade
     * @param currentInput the current redstone signal strength (0-15)
     * @return the changed redstone signal strength (0-15)
     */
    int getRedstoneInput(ItemStack stack, IUpgradableBlock block, int currentInput);
}
