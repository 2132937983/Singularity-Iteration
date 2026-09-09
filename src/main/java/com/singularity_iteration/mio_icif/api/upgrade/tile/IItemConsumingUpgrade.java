package com.singularity_iteration.mio_icif.api.upgrade.tile;

/**
 * 物品消耗升级接口（标记接口）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IItemConsumingUpgrade}。
 * 实现此接口的升级表示该升级会消耗物品作为工作材料。
 * <p>
 * 例如：某些特殊升级可能需要消耗特定的电路板或组件来提供额外功能。
 */
public interface IItemConsumingUpgrade extends IUpgradeItem {
}
