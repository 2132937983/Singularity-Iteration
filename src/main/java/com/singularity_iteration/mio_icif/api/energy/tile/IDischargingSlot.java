package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

/**
 * 放电槽接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IDischargingSlot}。
 * 实现此接口的方块具有专门用于从物品抽取能量的槽位。
 */
public interface IDischargingSlot extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 从槽位中的物品抽取能量。
     * <p>
     * 此方法由电网调用，从方块指定槽位中的物品抽取能量。
     *
     * @param amount 要抽取的能量值（EU）
     * @param remove 如果为 true，实际从物品中移除能量；如果为 false，仅模拟
     * @return 实际抽取的能量值（EU）
     */
    long dischargeItem(long amount, boolean remove);
}