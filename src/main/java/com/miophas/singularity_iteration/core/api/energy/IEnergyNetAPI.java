// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import java.util.Set;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Public queries used by admitted SI machine builders and addon examples. */
public interface IEnergyNetAPI {
    ICableTier getCableTier(String name);
    ICableTier getDefaultCableTier();
    int cableTierToSourceTier(ICableTier tier);
    boolean hasEnergyTile(Level level, BlockPos pos);
    NodeStats getNodeStats(Level level, BlockPos pos);
    Set<Direction> getConnections(Level level, BlockPos pos);

    /** Re-sample this tile and its neighbours after changing an electrical color. Server thread only. */
    default void conductorColorChanged(BlockEntity tile) {
        throw new UnsupportedOperationException("Energy color notifications are not supported by this provider");
    }

    /** Immutable diagnostics; does not create an engine, load chunks or renew energy budgets. Server thread only. */
    default EnergyDiagnostics diagnostics(ServerLevel level) {
        throw new UnsupportedOperationException("Energy diagnostics are not supported by this provider");
    }
}
