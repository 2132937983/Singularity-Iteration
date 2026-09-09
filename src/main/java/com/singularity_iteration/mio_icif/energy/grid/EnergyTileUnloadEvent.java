package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.world.level.Level;

public class EnergyTileUnloadEvent extends EnergyTileEvent {
    public EnergyTileUnloadEvent(IEnergyTile tile, Level world) {
        super(tile, world);
    }
}

