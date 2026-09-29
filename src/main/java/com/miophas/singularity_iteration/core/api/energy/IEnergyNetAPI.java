// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import java.util.Set;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Public queries used by admitted SI machine builders and addon examples. */
public interface IEnergyNetAPI {
    ICableTier getCableTier(String name);
    ICableTier getDefaultCableTier();
    int cableTierToSourceTier(ICableTier tier);
    boolean hasEnergyTile(Level level, BlockPos pos);
    NodeStats getNodeStats(Level level, BlockPos pos);
    Set<Direction> getConnections(Level level, BlockPos pos);
}
