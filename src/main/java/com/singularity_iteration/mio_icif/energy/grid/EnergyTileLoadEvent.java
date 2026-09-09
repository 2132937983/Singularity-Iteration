package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.world.level.Level;

/**
 * Posted when an energy tile is loaded/placed in the world.
 */
@SuppressWarnings("null")
public class EnergyTileLoadEvent extends EnergyTileEvent {
    public EnergyTileLoadEvent(IEnergyTile tile, Level world) {
        super(tile, world);
    }
}


