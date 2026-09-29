package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
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
     * 获取应用升级后的能量容量加成
     * @return 能量容量加成
     */
    long getEnergyCapacityBonus();

    /**
     * 获取应用储能升级后的 HU 容量
     *
     * @param baseCapacity 基础 HU 容量
     * @return 实际 HU 容量
     */
    long getHeatCapacity(long baseCapacity);
    
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

    /** IC2 每一叠流体升级、每个相邻面的 mB/t：50、200、800、3200、12800（封顶）。 */
    static int fluidTransferPerStack(int count) {
        return count <= 0 ? 0 : 50 << (2 * Math.min(4, count - 1));
    }

    /** 每个面的独立弹出额度。实现可保留不同升级槽的堆叠数量与方向。 */
    default int getFluidEjectorTransferLimit(Direction side) {
        var directions = getFluidEjectorDirections();
        return directions.isEmpty() || directions.contains(side) ? fluidTransferPerStack(getFluidEjectorCount()) : 0;
    }

    /** 每个面的独立抽入额度；不是六个面共用的额度。 */
    default int getFluidPullingTransferLimit(Direction side) {
        var directions = getFluidPullingDirections();
        return directions.isEmpty() || directions.contains(side) ? fluidTransferPerStack(getFluidPullingCount()) : 0;
    }
}
