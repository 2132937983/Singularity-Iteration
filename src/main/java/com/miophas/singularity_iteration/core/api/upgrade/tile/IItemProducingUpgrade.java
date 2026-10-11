package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Marker interface for upgrades that produce items.
 * <p>
 * Equivalent to {@code IItemProducingUpgrade} in IC2 1.12.2.
 * An upgrade that implements this interface can produce items.
 * <p>
 * Example: some upgrades produce an extra by-product while the machine operates.
 */
public interface IItemProducingUpgrade extends IUpgradeItem {
}
