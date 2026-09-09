package com.singularity_iteration.mio_icif.api.upgrade.tile;

import java.util.Set;

/**
 * 可升级方块接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IUpgradableBlock}。
 * 实现此接口的机器方块可以被升级物品增强。
 */
public interface IUpgradableBlock {

    /**
     * 获取方块当前的储能（EU）。
     *
     * @return 当前储能
     */
    long getEnergy();

    /**
     * 从方块中消耗指定量的能量。
     *
     * @param amount 要消耗的能量值（EU）
     * @return 如果成功消耗则返回 true
     */
    boolean useEnergy(long amount);

    /**
     * 获取此机器支持的升级属性集合。
     * <p>
     * 升级物品通过 {@link IUpgradeItem#isSuitableFor} 检查自己的类型
     * 是否在此集合中，以决定是否适用于此机器。
     *
     * @return 支持的升级属性集合
     */
    Set<UpgradableProperty> getUpgradableProperties();
}
