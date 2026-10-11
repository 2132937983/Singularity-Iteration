package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Interface for upgrades that consume fluid (marker interface).
 * <p>
 * This is the equivalent of {@code IFluidConsumingUpgrade} in IC2 for 1.12.2.
 * An upgrade that implements this interface uses fluid as a work material.
 * <p>
 * Example: some upgrades can use coolant or a special fluid to give additional functions.
 */
public interface IFluidConsumingUpgrade extends IUpgradeItem {
}
