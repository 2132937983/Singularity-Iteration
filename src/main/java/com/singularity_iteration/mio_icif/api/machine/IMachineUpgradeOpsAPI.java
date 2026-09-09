package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * 机器升级操作 API
 *
 * <p>提供升级插件的安装、移除和查询功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IMachineUpgradeOpsAPI {

    /**
     * 检查机器是否支持升级插件
     */
    boolean supportsUpgrades(Level world, BlockPos pos);

    /**
     * 获取机器当前安装的升级插件
     */
    Collection<ItemStack> getInstalledUpgrades(Level world, BlockPos pos);

    /**
     * 安装升级插件
     */
    boolean installUpgrade(Level world, BlockPos pos, ItemStack upgrade);

    /**
     * 移除升级插件
     */
    ItemStack removeUpgrade(Level world, BlockPos pos, int slot);
}
