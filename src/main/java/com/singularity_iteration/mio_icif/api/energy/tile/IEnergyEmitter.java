package com.singularity_iteration.mio_icif.api.energy.tile;

import net.minecraft.core.Direction;

/**
 * 能量发射者接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergyEmitter}。
 * 实现此接口的方块可以向电网输出能量。
 * <p>
 * 此接口继承自 {@link com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker}，
 * 因此实现此接口的方块实体可以通过能量事件注册到电网。
 */
public interface IEnergyEmitter extends com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent.IEnergyTileMarker {

    /**
     * 检查是否向指定方向的接收者输出能量。
     *
     * @param acceptor 能量接收者
     * @param to 能量输出方向
     * @return 如果向此方向输出能量则返回 true
     */
    boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction to);
}
