// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

import java.util.Objects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/** Lifecycle notification only; registering a listener must not imply transfer support. */
public final class EnergyTileLoadEvent extends Event {
    private final IEnergyTile tile;
    private final Level world;

    public EnergyTileLoadEvent(IEnergyTile tile, Level world) {
        this.tile = Objects.requireNonNull(tile, "tile");
        this.world = Objects.requireNonNull(world, "world");
    }

    public IEnergyTile getTile() { return tile; }
    public Level getWorld() { return world; }
}
