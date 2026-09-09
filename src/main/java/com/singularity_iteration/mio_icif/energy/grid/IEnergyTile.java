package com.singularity_iteration.mio_icif.energy.grid;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

/**
 * Base marker interface for all energy net participants.
 *
 * <p>此接口继承 {@link EnergyTileEvent.IEnergyTileMarker}（API 标记接口），
 * 使内部电网参与者自动满足 API 层的标记要求。
 */
public interface IEnergyTile extends EnergyTileEvent.IEnergyTileMarker {
    /** Called when the tile's network connections change. */
    default void onConnectionChange() {}
}

