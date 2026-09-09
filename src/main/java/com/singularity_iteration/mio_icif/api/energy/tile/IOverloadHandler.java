package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

/**
 * 过载处理接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IOverloadHandler}。
 * 实现此接口的能量方块在受到超过承受能力的电压冲击时会被调用。
 */
public interface IOverloadHandler extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 处理过载事件。
     * <p>
     * 当注入的能量超过导体的承受阈值时调用此方法。
     * 实现者应在此处理方块的损坏逻辑（如冒烟、爆炸等）。
     *
     * @param tier 过载能量的电压等级
     * @return 如果方块在过载中存活则返回 true，否则返回 false
     */
    boolean onOverload(int tier);
}