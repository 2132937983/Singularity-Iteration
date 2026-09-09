package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 机器控制 API
 *
 * <p>提供机器启停、状态查询等基础控制功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 */
public interface IMachineControlAPI {

    /**
     * 启动机器
     */
    boolean startMachine(Level world, BlockPos pos);

    /**
     * 停止机器
     */
    boolean stopMachine(Level world, BlockPos pos);

    /**
     * 获取机器工作进度
     */
    int getProgress(Level world, BlockPos pos);

    /**
     * 获取机器最大进度
     */
    int getMaxProgress(Level world, BlockPos pos);

    /**
     * 检查机器是否正在工作
     */
    boolean isWorking(Level world, BlockPos pos);

    /**
     * 获取机器类型
     *
     * @return 机器类型的 Optional 包装，如果该位置不是机器则返回空 Optional
     */
    Optional<IMachineAPI.MachineType> getMachineType(Level world, BlockPos pos);

    /**
     * 获取机器配置
     *
     * @return 机器配置的 Optional 包装，如果该位置不是机器则返回空 Optional
     */
    Optional<IMachineAPI.IMachineConfiguration> getMachineConfiguration(Level world, BlockPos pos);

    /**
     * 强制启动机器
     */
    boolean forceStart(Level world, BlockPos pos);

    /**
     * 强制停止机器
     */
    boolean forceStop(Level world, BlockPos pos);
}