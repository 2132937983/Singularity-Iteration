// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyNetAPI;

import java.util.Set;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class EnergyNetAPIImpl implements IEnergyNetAPI {
    @Override public ICableTier getDefaultCableTier() { return CableTier.LV; }
    @Override public int cableTierToSourceTier(ICableTier tier) { return tier.getTier() + 1; }
    @Override public ICableTier getCableTier(String name) { return CableTier.getTier(name); }
    @Override public boolean hasEnergyTile(Level level, BlockPos pos) { return EnergyNetGlobal.getTile(level, pos) != null; }
    @Override public NodeStats getNodeStats(Level level, BlockPos pos) { return EnergyNetGlobal.getNodeStats(level, pos); }
    @Override public Set<Direction> getConnections(Level level, BlockPos pos) { return EnergyNetGlobal.getConnections(level, pos); }
}
