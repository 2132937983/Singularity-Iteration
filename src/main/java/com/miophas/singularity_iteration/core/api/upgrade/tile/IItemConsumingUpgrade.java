package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Interface for item-consuming upgrades (marker interface).
 * <p>
 * Matches {@code IItemConsumingUpgrade} of IC2 1.12.2.
 * An upgrade that implements this interface uses up items as work material.
 * <p>
 * Example: some special upgrades can use up specific circuit boards or components to give extra functions.
 */
public interface IItemConsumingUpgrade extends IUpgradeItem {
}
