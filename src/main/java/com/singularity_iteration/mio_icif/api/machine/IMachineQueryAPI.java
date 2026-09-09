package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * 机器查询 API
 *
 * <p>提供机器检测、信息查询和批量检索功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IMachineQueryAPI {

    /**
     * 检查指定位置是否有机器
     */
    boolean isMachine(Level world, BlockPos pos);

    /**
     * 获取机器信息
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 机器信息的 Optional 包装，如果该位置不是机器或机器信息不可用则返回空 Optional
     */
    Optional<IMachineInfo> getMachineInfo(Level world, BlockPos pos);

    /**
     * 获取指定类型的所有机器
     */
    Collection<BlockPos> getMachinesByType(Level world, IMachineAPI.MachineType type);

    /**
     * 获取指定世界的所有机器
     */
    Collection<BlockPos> getAllMachines(Level world);

    /**
     * 获取机器工作速度倍率
     *
     * @deprecated 速度倍率由升级插件系统控制，请使用
     * {@link IUpgradeStatsAPI#getEffectiveSpeedMultiplier} 获取准确值。
     */
    @Deprecated
    double getSpeedMultiplier(Level world, BlockPos pos);

    /**
     * 获取机器能量消耗倍率
     *
     * @deprecated 能量倍率由升级插件系统控制，请使用
     * {@link IUpgradeStatsAPI#getEffectiveEnergyMultiplier} 获取准确值。
     */
    @Deprecated
    double getEnergyMultiplier(Level world, BlockPos pos);

    /**
     * 获取机器的电缆等级
     *
     * @deprecated 电缆等级由升级插件系统控制，请使用
     * {@link IUpgradeStatsAPI#getEffectiveCableTier} 获取准确值。
     */
    @Deprecated
    com.singularity_iteration.mio_icif.api.energy.ICableTier getCableTier(Level world, BlockPos pos);
}
