// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySource;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyTile;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;

import java.util.Set;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Compatibility entrypoints backed by the independently authored active engine. */
public final class EnergyNetGlobal {
    private EnergyNetGlobal() { }
    private static IndependentSiEnergy engine(Level level) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return null;
        return IndependentSiEnergy.current(server.getServer());
    }
    public static void addTile(IEnergyTile tile, Level level, BlockPos pos) {
        var engine = engine(level);
        if (engine != null) engine.registerTile((ServerLevel)level, pos, tile);
        if (tile instanceof FECompatTile compat) compat.setRegistered(engine != null);
    }
    public static void removeTile(IEnergyTile tile) {
        Level level = tile instanceof BlockEntity block ? block.getLevel()
            : tile instanceof ILocatableTile located ? located.getWorld() : null;
        var engine = engine(level);
        if (engine != null) engine.unregisterTile(tile);
        if (tile instanceof FECompatTile compat) compat.setRegistered(false);
    }
    public static IEnergyTile getTile(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.tileAt((ServerLevel)level, pos);
    }
    public static NodeStats getNodeStats(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.nodeStats((ServerLevel)level, pos);
    }

    /** For instruments sampled at ServerTickEvent.Post, after all level transfers. */
    public static NodeStats getCurrentTickNodeStats(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.currentTickNodeStats((ServerLevel)level, pos);
    }
    public static NodeStats getNodeStats(IEnergyTile tile) {
        Level level = tile instanceof BlockEntity block ? block.getLevel()
            : tile instanceof ILocatableTile located ? located.getWorld() : null;
        BlockPos pos = tile instanceof BlockEntity block ? block.getBlockPos()
            : tile instanceof ILocatableTile located ? located.getPos() : null;
        return pos == null ? null : getNodeStats(level, pos);
    }
    public static Set<Direction> getConnections(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? Set.of() : engine.connections((ServerLevel)level, pos);
    }
    public static boolean sourceHasReachableSinks(IEnergySource source) {
        Level level = source instanceof BlockEntity block ? block.getLevel()
            : source instanceof ILocatableTile located ? located.getWorld() : null;
        BlockPos pos = source instanceof BlockEntity block ? block.getBlockPos()
            : source instanceof ILocatableTile located ? located.getPos() : null;
        var engine = engine(level);
        return engine != null && pos != null && engine.hasReachableSink((ServerLevel)level, pos);
    }
    public static int cableTierToSourceTier(CableTier tier) { return tier.getTier() + 1; }
    public static double getPowerFromTier(int tier) {
        if (tier < 0) return 0;
        return Math.scalb(8.0, Math.min(60, tier) * 2);
    }
    public static int getTierFromPower(double power) {
        if (!Double.isFinite(power) || power < 0) throw new IllegalArgumentException("Invalid source power");
        int tier = 0;
        while (tier < 60 && getPowerFromTier(tier) < power) tier++;
        return tier;
    }
}
