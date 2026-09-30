// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyNetAPI;
import com.miophas.singularity_iteration.core.api.energy.EnergyDiagnostics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Energy API requires the server thread");
    }

    @Override public void conductorColorChanged(BlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level))
            throw new IllegalStateException("Energy color notifications require a server level");
        requireServerThread(level);
        if (!tile.isRemoved()) IndependentSiEnergy.conductorColorChanged(tile);
    }

    @Override public EnergyDiagnostics diagnostics(ServerLevel level) {
        requireServerThread(level);
        var engine = IndependentSiEnergy.current(level.getServer());
        if (engine == null) return EnergyDiagnostics.unavailable();
        var routing = engine.routingMetrics(level);
        var recovery = engine.topologyRecoveryMetrics();
        var timing = engine.performanceMetrics(level);
        return new EnergyDiagnostics(true,
            new EnergyDiagnostics.Routing(routing.topologyGenerations(), routing.plansBuilt(), routing.plansResumed(), routing.budgetYields()),
            new EnergyDiagnostics.Recovery(recovery.rebuilds(), recovery.isolatedFailures(), recovery.recoveringLevels(), recovery.quarantinedChunks(), recovery.lastReason()),
            new EnergyDiagnostics.Timing(timing.samples(), timing.p50Nanos(), timing.p95Nanos(), timing.p99Nanos(), timing.maxNanos()));
    }
}
