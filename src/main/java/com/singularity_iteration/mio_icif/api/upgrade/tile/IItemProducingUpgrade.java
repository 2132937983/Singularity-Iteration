package com.singularity_iteration.mio_icif.api.upgrade.tile;

/**
 * 物品产出升级接口（标记接口）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IItemProducingUpgrade}。
 * 实现此接口的升级表示该升级可以产出物品。
 * <p>
 * 例如：某些升级可以在机器工作时额外产出副产品。
 */
public interface IItemProducingUpgrade extends IUpgradeItem {
}
