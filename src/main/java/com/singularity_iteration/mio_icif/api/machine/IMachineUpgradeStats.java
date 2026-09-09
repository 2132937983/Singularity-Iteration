package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * API 级机器升级统计接口
 * 使附属模组开发者与内部 MachineUpgradeStats 实现解耦
 */
public interface IMachineUpgradeStats {
    
    /**
     * 获取处理时间倍率
     * @return 处理时间倍率（越低越快）
     */
    double getProcessTimeMultiplier();
    
    /**
     * 获取能耗倍率
     * @return 能耗倍率
     */
    double getEnergyUsageMultiplier();
    
    /**
     * 获取应用升级后的每 tick 能耗
     * @param baseEnergyPerTick 基础每 tick 能耗
     * @return 实际每 tick 能耗
     */
    long getEnergyPerTick(long baseEnergyPerTick);
    
    /**
     * 获取应用升级后的最大进度
     * @param baseMaxProgress 基础最大进度
     * @return 实际最大进度
     */
    int getMaxProgress(int baseMaxProgress);
    
    /**
     * 获取应用升级后的处理时间
     * @param baseTicks 基础 tick 数
     * @return 实际处理时间
     */
    int getProcessTicks(int baseTicks);
    
    /**
     * 获取升级带来的能量容量加成
     * @return 能量容量加成
     */
    long getEnergyCapacityBonus();
    
    /**
     * 获取应用变压器升级后的有效电缆等级
     * @param baseTier 基础电缆等级
     * @return 有效电缆等级
     */
    ICableTier getEffectiveCableTier(ICableTier baseTier);
    
    /**
     * 获取超频升级数量
     * @return 超频升级数量
     */
    int getOverclockerCount();
    
    /**
     * 获取储能升级数量
     * @return 储能升级数量
     */
    int getEnergyStorageCount();
    
    /**
     * 获取变压器升级数量
     * @return 变压器升级数量
     */
    int getTransformerCount();
    
    /**
     * 获取弹射升级数量
     * @return 弹射升级数量
     */
    int getEjectorCount();
    
    /**
     * 获取吸取升级数量
     * @return 吸取升级数量
     */
    int getPullingCount();
    
    /**
     * 获取流体弹射升级数量
     * @return 流体弹射升级数量
     */
    int getFluidEjectorCount();
    
    /**
     * 获取流体吸取升级数量
     * @return 流体吸取升级数量
     */
    int getFluidPullingCount();
    
    /**
     * 检查红石信号是否反转
     * @return 如果反转则返回 true
     */
    boolean isRedstoneInverted();
    
    /**
     * 获取弹射方向
     * @return 方向列表
     */
    List<Direction> getEjectorDirections();
    
    /**
     * 获取吸取方向
     * @return 方向列表
     */
    List<Direction> getPullingDirections();
    
    /**
     * 获取流体弹射方向
     * @return 方向列表
     */
    List<Direction> getFluidEjectorDirections();
    
    /**
     * 获取流体吸取方向
     * @return 方向列表
     */
    List<Direction> getFluidPullingDirections();
}