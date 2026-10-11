// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileLoadEvent;
import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileUnloadEvent;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;

import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Existing SI lifecycle events feed the independent engine, never a parallel grid.
 *
 * <p>An energy tile posts {@link EnergyTileLoadEvent} when it joins the world and
 * {@link EnergyTileUnloadEvent} when it leaves. This class forwards both events to
 * {@link EnergyNetGlobal}, which passes them to the engine of the server. The conductor topology
 * itself comes from chunk and block events in {@code PlatformTopology}, not from these events.
 */
public final class GridEventHandler {
    /** True after the first {@link #init()}. The listeners are global, so they are added only once per JVM. */
    private static boolean installed;
    private GridEventHandler() { }
    /**
     * Installs the engine hooks and the two tile event listeners. A second call has no effect.
     *
     * <p>A load event uses the block position of a {@link BlockEntity} tile, or the position of an
     * {@link ILocatableTile}. A tile that is neither is ignored.
     */
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
