package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 升级统计 API
 *
 * <p>提供机器升级相关的统计信息和有效参数查询功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IUpgradeStatsAPI {

    /**
     * 获取机器的升级统计信息
     */
    Optional<IMachineUpgradeStats> getUpgradeStats(Level world, BlockPos pos);

    /**
     * 获取升级后的实际处理时间倍率
     *
     * <p><b>注意</b>：此方法返回的是<b>处理时间倍率</b>，而非速度倍率。
     * 值越小表示处理越快（例如 0.5 表示处理时间减半，速度翻倍）。
     *
     * <p><b>命名说明</b>：方法名保留 "Speed" 以维持向后兼容，但语义实际为 "TimeMultiplier"。
     * 获取真正的速度倍率请使用 {@code 1.0 / getEffectiveProcessTimeMultiplier(world, pos)}。
     *
     * @return 处理时间倍率（值越小越快，1.0 为基准速度）
     * @deprecated 语义与方法名不符，请使用 {@link #getEffectiveProcessTimeMultiplier(Level, BlockPos)} 代替
     */
    @Deprecated
    double getEffectiveSpeedMultiplier(Level world, BlockPos pos);

    /**
     * 获取升级后的实际处理时间倍率（语义正确的方法名）
     *
     * <p>值越小表示处理越快（例如 0.5 表示处理时间减半，速度翻倍）。
     * 获取真正的速度倍率请使用 {@code 1.0 / getEffectiveProcessTimeMultiplier(world, pos)}。
     *
     * @return 处理时间倍率（值越小越快，1.0 为基准速度）
     */
    double getEffectiveProcessTimeMultiplier(Level world, BlockPos pos);

    /**
     * 获取升级后的实际能量消耗倍率
     *
     * @return 能量消耗倍率
     */
    double getEffectiveEnergyMultiplier(Level world, BlockPos pos);

    /**
     * 获取升级后的有效电缆等级
     *
     * @return 有效电缆等级
     */
    Optional<ICableTier> getEffectiveCableTier(Level world, BlockPos pos);

    /**
     * 获取升级后的能量容量加成
     *
     * @return 容量加成 (EU)
     */
    long getEnergyCapacityBonus(Level world, BlockPos pos);

    /**
     * 获取升级后的实际工作 tick
     *
     * @return 实际工作 tick
     */
    int getEffectiveProcessTicks(Level world, BlockPos pos);

    /**
     * 获取升级后的实际每 tick 能耗
     *
     * @return 实际每 tick 能耗 (EU)
     */
    long getEffectiveEnergyPerTick(Level world, BlockPos pos);
}