package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * Machine upgrade operations API.
 *
 * <p>Gives methods to install, remove and query upgrade modules.
 * It was split from IMachineAPI to follow the single responsibility principle.
 */
public interface IMachineUpgradeOpsAPI {

    /**
     * Checks if the machine accepts upgrade modules.
     */
    boolean supportsUpgrades(Level world, BlockPos pos);

    /**
     * Gets the upgrade modules that are installed in the machine now.
     */
    Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos);

    /**
     * Installs an upgrade module.
     *
     * @return true if the upgrade was installed
     */
    boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * Removes the upgrade module from an upgrade slot. Server side only.
     *
     * @param slot the index inside the upgrade slots (0 = the first upgrade slot), not the container slot index
     * @return the removed upgrade; an empty stack if nothing was removed or on the client
     */
    ItemStack removeUpgrade(Level world, BlockPos pos, int slot);
}
