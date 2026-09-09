package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 反应堆多方块 API
 *
 * <p>提供反应堆多方块结构的查询和管理功能。
 * 从 IMachineAPI 拆分而来，遵循单一职责原则。
 *
 * <p>注意：此接口专注于多方块结构管理，与 {@link com.singularity_iteration.mio_icif.api.reactor.IReactorAPI}
 * （反应堆元件系统）职责不同。
 */
public interface IMultiblockReactorAPI {

    /**
     * 获取反应堆多方块信息
     *
     * @return 多方块信息的 Optional 包装，如果不是反应堆则返回空 Optional
     */
    Optional<IMachineAPI.IMultiblockInfo> getReactorInfo(Level world, BlockPos pos);

    /**
     * 检查位置是否是反应堆控制器
     */
    boolean isReactorController(Level world, BlockPos pos);

    /**
     * 获取反应堆温度
     */
    double getReactorTemperature(Level world, BlockPos pos);

    /**
     * 获取反应堆冷却水平
     */
    double getReactorCoolantLevel(Level world, BlockPos pos);
}