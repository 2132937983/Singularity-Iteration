package com.singularity_iteration.mio_icif.energy.grid;

import java.util.List;

/**
 * A tile that delegates to multiple sub-tiles (e.g. multi-block machines).
 * Each sub-tile has its own position and can connect to the grid independently.
 */
public interface IMetaDelegate extends IEnergyTile {
    /**
     * @return All sub-tiles that form this multi-block.
     */
    List<IEnergyTile> getSubTiles();
}


