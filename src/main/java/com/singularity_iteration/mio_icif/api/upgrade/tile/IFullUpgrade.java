package com.singularity_iteration.mio_icif.api.upgrade.tile;

/**
 * 全能升级接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IFullUpgrade}。
 * 实现此接口的升级同时具备所有升级类型的功能。
 * 这是一个便捷接口，继承了所有具体的升级子接口。
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
