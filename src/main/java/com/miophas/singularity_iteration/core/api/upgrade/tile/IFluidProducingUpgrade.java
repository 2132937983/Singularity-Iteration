package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Fluid producing upgrade interface (marker interface).
 * <p>
 * This is the equivalent of {@code IFluidProducingUpgrade} in IC2 1.12.2.
 * An upgrade that implements this interface can produce fluid.
 * <p>
 * For example, some upgrades make a by-product fluid while the machine works.
 */
public interface IFluidProducingUpgrade extends IUpgradeItem {
}
