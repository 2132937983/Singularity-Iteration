package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Interface for full upgrades.
 * <p>
 * It is the equivalent of {@code IFullUpgrade} in IC2 1.12.2.
 * Upgrades that implement this interface have the functions of all upgrade types.
 * This is a convenience interface. It extends all specific upgrade sub-interfaces.
 */
public interface IFullUpgrade extends
        IAugmentationUpgrade,
        IEnergyStorageUpgrade,
        IFluidConsumingUpgrade,
        IFluidProducingUpgrade,
        IItemConsumingUpgrade,
        IItemProducingUpgrade,
        IProcessingUpgrade,
        IRedstoneSensitiveUpgrade,
        ITransformerUpgrade,
        IRemoteAccessUpgrade {
}
