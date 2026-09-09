package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import java.util.List;

public class GridChange {
    public enum Type {
        ADDITION,
        REMOVAL
    }

    public final Type type;
    public final BlockPos pos;
    public final IEnergyTile ioTile;
    public List<IEnergyTile> subTiles;

    public GridChange(Type type, BlockPos pos, IEnergyTile ioTile) {
        this.type = type;
        this.pos = pos;
        this.ioTile = ioTile;
    }
}

