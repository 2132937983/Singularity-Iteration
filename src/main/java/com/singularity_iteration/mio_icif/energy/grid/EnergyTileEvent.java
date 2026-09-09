package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Base event for energy tile load/unload.
 */
@SuppressWarnings("null")
public class EnergyTileEvent extends Event {
    public final IEnergyTile tile;
    public final Level world;

    public EnergyTileEvent(IEnergyTile tile, Level world) {
        this.tile = tile;
        this.world = world;
    }
}


