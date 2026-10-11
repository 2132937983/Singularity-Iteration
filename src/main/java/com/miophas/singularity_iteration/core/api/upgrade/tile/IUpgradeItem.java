package com.miophas.singularity_iteration.core.api.upgrade.tile;

import java.util.Collection;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for upgrade items.
 * <p>
 * This is the equivalent of {@code IUpgradeItem} in IC2 for 1.12.2.
 * An item that implements this interface can be a machine upgrade module.
 * It gives the machine special functions and behavior.
 * <p>
 * An upgrade item has three main callbacks:
 * <ul>
 *   <li>{@link #isSuitableFor} - checks if the upgrade applies to a given machine</li>
 *   <li>{@link #onTick} - runs each tick and can apply continuous effects</li>
 *   <li>{@link #onProcessEnd} - runs after a recipe is complete and can change the output</li>
 * </ul>
 */
public interface IUpgradeItem {

    /**
     * Checks if this upgrade item applies to a machine with the given properties.
     *
     * @param stack the upgrade item stack
     * @param properties the set of upgradable properties of the machine
     * @return true if the upgrade applies
     */
    boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties);

    /**
     * The machine calls this method each tick.
     * <p>
     * The upgrade can apply continuous effects here, for example auto-eject of items or redstone control.
     *
     * @param stack the upgrade item stack
     * @param block the machine that has this upgrade installed
     * @return true if the upgrade uses this tick (the machine then does not do its normal tick)
     */
    boolean onTick(ItemStack stack, IUpgradableBlock block);

    /**
     * The machine calls this method after a recipe is complete.
     * <p>
     * The upgrade can change the output items here, for example to increase the yield or add by-products.
     *
     * @param stack the upgrade item stack
     * @param block the machine that has this upgrade installed
     * @param outputs the current output items (you can change this collection)
     * @return the changed output items
     */
    Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock block, Collection<ItemStack> outputs);

    /**
     * Gets the name of the upgrade type.
     */
    default String getUpgradeTypeName() {
        return "unknown";
    }

    /**
     * Gets the API-level upgrade type.
     */
    default com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.UpgradeType getApiUpgradeType() {
        return com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.UpgradeType.CUSTOM;
    }

    /**
     * Gets the upgrade type. This is a shortcut. It is the same as {@link #getApiUpgradeType()}.
     *
     * <p>Subclasses should override this method to return the correct upgrade type.
     * The API layer uses it to identify the upgrade kind without loading the implementation class.
     *
     * @return the upgrade type; the default is {@code CUSTOM}
     */
    default com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.UpgradeType getUpgradeType() {
        return getApiUpgradeType();
    }

    /**
     * Gets the upgrade effect object.
     *
     * <p>This method puts all effect parameters of this upgrade (speed, energy use, capacity and others) into an
     * {@link com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.IUpgradeEffect}.
     * The API layer uses it to calculate the combined effect in one way for all upgrades.
     *
     * <p>The default implementation makes an effect object from the {@code getXxxBonus()} methods of this interface.
     * Subclasses can override it to give a more precise effect calculation.
     *
     * @param stack the upgrade item stack
     * @return the upgrade effect object; never null
     */
    default com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.IUpgradeEffect getUpgradeEffect(ItemStack stack) {
        // Each method calls the outer IUpgradeItem method with "IUpgradeItem.this". A plain call
        // resolves to the anonymous class itself and recurses until StackOverflowError
        // (fixed in 0.1.7.36).
        return new com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.IUpgradeEffect() {
            @Override
            public double getSpeedMultiplier() {
                return 1.0 / getSpeedBonus();
            }

            @Override
            public double getEnergyMultiplier() {
                return getEnergyBonus();
            }

            @Override
            public long getExtraCapacity() {
                return IUpgradeItem.this.getExtraCapacity();
            }

            @Override
            public int getExtraOutputSlots() {
                return IUpgradeItem.this.getExtraOutputSlots();
            }

            @Override
            public boolean hasAutoEject() {
                return IUpgradeItem.this.hasAutoEject();
            }

            @Override
            public boolean hasAutoImport() {
                return IUpgradeItem.this.hasAutoImport();
            }

            @Override
            public boolean hasRedstoneControl() {
                return IUpgradeItem.this.hasRedstoneControl();
            }

            @Override
            public int getTierUpgrade() {
                return IUpgradeItem.this.getTierUpgrade();
            }
        };
    }

    /**
     * Returns true if the upgrade has the auto-eject function.
     */
    default boolean hasAutoEject() {
        return false;
    }

    /**
     * Returns true if the upgrade has the auto-import function.
     */
    default boolean hasAutoImport() {
        return false;
    }

    /**
     * Returns true if the upgrade has the redstone control function.
     */
    default boolean hasRedstoneControl() {
        return false;
    }

    /**
     * Gets the additional energy capacity bonus in EU.
     */
    default long getExtraCapacity() {
        return 0;
    }

    /**
     * Gets the number of additional output slots.
     */
    default int getExtraOutputSlots() {
        return 0;
    }

    /**
     * Gets the tier increase that the upgrade gives.
     */
    default int getTierUpgrade() {
        return 0;
    }

    /**
     * Checks if this upgrade is directional.
     */
    default boolean isDirectional() {
        return false;
    }

    /**
     * Gets the speed multiplier. A smaller value means a faster machine.
     */
    default float getSpeedBonus() {
        return 1.0f;
    }

    /**
     * Gets the energy consumption multiplier.
     */
    default float getEnergyBonus() {
        return 1.0f;
    }

    /**
     * Gets the upgrade tier. The range is 1 to 4 (LV, MV, HV, EV).
     */
    default int getUpgradeTier(ItemStack stack) {
        return 1;
    }
}
