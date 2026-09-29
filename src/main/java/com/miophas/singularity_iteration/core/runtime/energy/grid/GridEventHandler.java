// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileLoadEvent;
import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileUnloadEvent;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;

import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;

/** Existing SI lifecycle events feed the independent engine, never a parallel grid. */
public final class GridEventHandler {
    private static boolean installed;
    private GridEventHandler() { }
    public static synchronized void init() {
        if (installed) return;
        installed = true;
        IndependentSiEnergy.install();
        NeoForge.EVENT_BUS.addListener((EnergyTileLoadEvent event) -> {
            var tile = event.getTile();
            if (tile instanceof BlockEntity block) EnergyNetGlobal.addTile(tile, event.getWorld(), block.getBlockPos());
            else if (tile instanceof ILocatableTile located) EnergyNetGlobal.addTile(tile, event.getWorld(), located.getPos());
        });
        NeoForge.EVENT_BUS.addListener((EnergyTileUnloadEvent event) -> EnergyNetGlobal.removeTile(event.getTile()));
    }
}
