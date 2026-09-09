package com.singularity_iteration.mio_icif.api.generator;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 发电机信息接口
 *
 * <p>提供发电机运行时状态的只读视图，用于查询发电速率、能量存储、效率等信息。
 */
public interface IGeneratorInfo {

    /**
     * 获取发电机所在的世界
     */
    Level getWorld();

    /**
     * 获取发电机方块位置
     */
    BlockPos getPos();

    /**
     * 获取发电机类型 ID
     */
    String getTypeId();

    /**
     * 获取发电机发电速率（EU/tick）
     */
    long getGenerationRate();

    /**
     * 获取发电机当前存储的能量
     */
    long getStoredEnergy();

    /**
     * 获取发电机最大能量容量
     */
    long getMaxEnergy();

    /**
     * 获取发电机的电缆等级
     */
    ICableTier getCableTier();

    /**
     * 检查发电机是否正在发电
     */
    boolean isActive();

    /**
     * 获取发电机能量输出速率
     */
    long getOutputRate();

    /**
     * 获取发电机效率（0-100）
     * 由于升级插件、燃料质量等因素，实际效率可能低于基础效率。
     */
    double getEfficiency();

    /**
     * 获取本次运行的持续时间（tick）
     */
    long getRuntimeTicks();

    /**
     * 获取发电机总发电量
     */
    long getTotalGenerated();
}