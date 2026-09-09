package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

/**
 * 充电槽接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IChargingSlot}。
 * 实现此接口的方块具有专门用于给物品充电的槽位。
 */
public interface IChargingSlot extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 向槽位中的物品充入能量。
     * <p>
     * 此方法由电网调用，将能量充入方块指定槽位中的物品。
     *
     * @param amount 要充入的能量值（EU）
     * @return 实际充入的能量值（EU）
     */
    long chargeItem(long amount);
}