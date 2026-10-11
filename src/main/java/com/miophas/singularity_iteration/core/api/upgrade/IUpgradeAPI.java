package com.miophas.singularity_iteration.core.api.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * Upgrade API.
 *
 * <p>Gives an interface to manage machine upgrades. It lets you:
 * <ul>
 *   <li>Query upgrade types and effects</li>
 *   <li>Install and remove upgrades</li>
 *   <li>Calculate the properties after upgrades</li>
 * </ul>
 */
public interface IUpgradeAPI {

    /**
     * Upgrade type enumeration.
     */
    enum UpgradeType {
        OVERCLOCKER,        // Overclocker upgrade - increases speed, but also energy use
        TRANSFORMER,        // Transformer upgrade - increases the voltage tier
        ENERGY_STORAGE,     // Energy storage upgrade - increases energy capacity
        EJECTOR,            // Ejector upgrade - outputs items automatically
        IMPORT,             // Import upgrade - inputs items automatically
        FLUID_EJECTOR,      // Fluid ejector upgrade - outputs fluids automatically
        FLUID_IMPORT,       // Fluid import upgrade - inputs fluids automatically
        REDSTONE_SIGNAL,    // Redstone signal upgrade - redstone control
        ADVANCED_CIRCUIT,   // Advanced circuit upgrade - adds processing slots
        MOLECULAR_TRANSFORM, // Molecular transform upgrade - changes the recipe
        QUANTUM_CORE,       // Quantum core upgrade - increases performance very much
        CUSTOM              // Custom upgrade
    }

    /**
     * Upgrade effect interface.
     */
    interface IUpgradeEffect {
        /**
         * Gets the speed multiplier bonus.
         */
        double getSpeedMultiplier();

        /**
         * Gets the energy use multiplier.
         */
        double getEnergyMultiplier();

        /**
         * Gets the additional energy capacity.
         */
        long getExtraCapacity();

        /**
         * Gets the number of additional output slots.
         */
        int getExtraOutputSlots();

        /**
         * Checks if the upgrade enables automatic output.
         */
        boolean hasAutoEject();

        /**
         * Checks if the upgrade enables automatic input.
         */
        boolean hasAutoImport();

        /**
         * Checks if the upgrade enables redstone control.
         */
        boolean hasRedstoneControl();

        /**
         * Gets the number of voltage tiers to add.
         */
        int getTierUpgrade();
    }

    /**
     * Checks if the machine accepts upgrades.
     */
    boolean supportsUpgrades(Level world, BlockPos pos);

    /**
     * Gets the maximum number of upgrade slots of the machine.
     */
    int getMaxUpgradeSlots(Level world, BlockPos pos);

    /**
     * Gets the installed upgrades.
     */
    Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos);

    /**
     * Gets the upgrade type.
     */
    UpgradeType getUpgradeType(ItemStack upgrade);

    /**
     * Gets the upgrade effect.
     */
    IUpgradeEffect getUpgradeEffect(ItemStack upgrade);

    /**
     * Calculates the combined effect of more than one upgrade.
     */
    IUpgradeEffect calculateCombinedEffect(Collection<ItemStack> upgrades);

    /**
     * Installs an upgrade.
     */
    boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * Removes an upgrade.
     */
    ItemStack removeUpgrade(Level world, BlockPos pos, int slot);

    /**
     * Checks if this upgrade can be installed.
     */
    boolean canInstallUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * Gets the maximum number of upgrades of this type.
     */
    int getUpgradeLimit(UpgradeType type);
}