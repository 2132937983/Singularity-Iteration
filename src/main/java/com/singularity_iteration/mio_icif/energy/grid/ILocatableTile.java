package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Extension of {@link IEnergyTile} for tiles that are not BlockEntities
 * but still need to be located in the world for grid registration.
 */
public interface ILocatableTile extends IEnergyTile {
    Level getWorld();
    BlockPos getPos();
}
