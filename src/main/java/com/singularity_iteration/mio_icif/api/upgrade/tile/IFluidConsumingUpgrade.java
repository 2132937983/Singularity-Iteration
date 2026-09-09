package com.singularity_iteration.mio_icif.api.upgrade.tile;

/**
 * 流体消耗升级接口（标记接口）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IFluidConsumingUpgrade}。
 * 实现此接口的升级表示该升级会消耗流体作为工作材料。
 * <p>
 * 例如：某些升级可能需要消耗冷却液或特殊流体来提供额外功能。
 */
public interface IFluidConsumingUpgrade extends IUpgradeItem {
}
