package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import net.minecraft.core.Direction;

/**
 * 能量接收者接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergyAcceptor}。
 * 实现此接口的方块可以接收来自电网的能量。
 * <p>
 * 此接口继承自 {@link EnergyTileEvent.IEnergyTileMarker}，
 * 因此实现此接口的方块实体可以通过 {@link EnergyTileEvent} 注册到电网。
 */
public interface IEnergyAcceptor extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 检查是否接受来自指定发射者的能量。
     *
     * @param emitter 能量发射者
     * @param from 能量来源方向
     * @return 如果接受此方向的能量则返回 true
     */
    boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction from);
}
