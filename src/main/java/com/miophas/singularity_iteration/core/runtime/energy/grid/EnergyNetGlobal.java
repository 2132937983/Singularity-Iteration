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

/**
 * Compatibility entrypoints backed by the independently authored active engine.
 *
 * <p>This class gives a static, IC2-style energy net API. Each method finds the
 * {@link IndependentSiEnergy} engine of the level and delegates to it.
 *
 * <p><b>Rules for all methods:</b>
 * <ul>
 *   <li>The engine works only on a {@link ServerLevel}, on the server thread.
 *       On a client level, on another thread, or when no engine is attached,
 *       a method does nothing and returns its empty value ({@code null},
 *       {@code false} or an empty set).</li>
 *   <li>A method does not load chunks.</li>
 *   <li>Registration is a notification. Only loaded SI block entities with an
 *       owned storage take part in a native transfer.</li>
 * </ul>
 *
 * <p><b>Tiers.</b> A source tier maps to a packet size in EU:
 * {@code 8 * 4^tier}. Tier 0 is 8 EU, tier 1 is 32 EU, tier 2 is 128 EU,
 * tier 3 is 512 EU, tier 4 is 2048 EU.
 */
public final class EnergyNetGlobal {
    private EnergyNetGlobal() { }

    /**
     * Returns the engine of a level.
     *
     * @return the engine, or {@code null} when the level is not a server level,
     *         the caller is not on the server thread, or no engine is attached
     */
    private static IndependentSiEnergy engine(Level level) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return null;
        return IndependentSiEnergy.current(server.getServer());
    }

    /**
     * Registers a tile at a position. The engine then observes the position
     * again. An {@link FECompatTile} gets its registered flag set to
     * {@code true} only when an engine accepted the call.
     */
    public static void addTile(IEnergyTile tile, Level level, BlockPos pos) {
        var engine = engine(level);
        if (engine != null) engine.registerTile((ServerLevel)level, pos, tile);
        if (tile instanceof FECompatTile compat) compat.setRegistered(engine != null);
    }

    /**
     * Removes all registrations of a tile. The level comes from the block entity
     * or from {@link ILocatableTile#getWorld()}. An {@link FECompatTile} always
     * gets its registered flag cleared, also when no engine is found.
     */
    public static void removeTile(IEnergyTile tile) {
        Level level = tile instanceof BlockEntity block ? block.getLevel()
            : tile instanceof ILocatableTile located ? located.getWorld() : null;
        var engine = engine(level);
        if (engine != null) engine.unregisterTile(tile);
        if (tile instanceof FECompatTile compat) compat.setRegistered(false);
    }

    /**
     * Returns the energy tile at a loaded position.
     *
     * @return the tile, or {@code null} when there is none or no engine
     * @see IndependentSiEnergy#tileAt
     */
    public static IEnergyTile getTile(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.tileAt((ServerLevel)level, pos);
    }

    /**
     * Returns the meter sample of a position from the current or the previous
     * tick. Values: EU in, EU out, highest voltage (EU per packet).
     *
     * @return the sample, or {@code null} when the position moved no energy
     *         recently or there is no engine
     * @see IndependentSiEnergy#nodeStats
     */
    public static NodeStats getNodeStats(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.nodeStats((ServerLevel)level, pos);
    }

    /**
     * For instruments sampled at ServerTickEvent.Post, after all level transfers.
     *
     * <p>Unlike {@link #getNodeStats(Level, BlockPos)}, an idle node returns a
     * zero sample, never the value of an older tick.
     *
     * @return the sample of this tick, or {@code null} when there is no node or no engine
     */
    public static NodeStats getCurrentTickNodeStats(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? null : engine.currentTickNodeStats((ServerLevel)level, pos);
    }

    /**
     * Returns the meter sample of a tile. The level and the position come from
     * the block entity or from {@link ILocatableTile}.
     *
     * @return the sample, or {@code null} when the tile has no position
     */
    public static NodeStats getNodeStats(IEnergyTile tile) {
        Level level = tile instanceof BlockEntity block ? block.getLevel()
            : tile instanceof ILocatableTile located ? located.getWorld() : null;
        BlockPos pos = tile instanceof BlockEntity block ? block.getBlockPos()
            : tile instanceof ILocatableTile located ? located.getPos() : null;
        return pos == null ? null : getNodeStats(level, pos);
    }

    /**
     * Returns the faces of a position that have an electrical connection now.
     * This query is not cheap. Do not call it every tick for many positions.
     *
     * @return an immutable set, empty when there is no engine
     * @see IndependentSiEnergy#connections
     */
    public static Set<Direction> getConnections(Level level, BlockPos pos) {
        var engine = engine(level);
        return engine == null ? Set.of() : engine.connections((ServerLevel)level, pos);
    }

    /**
     * Tells whether a source can reach a sink that is not full, or a foreign
     * FE block that can receive. The test uses connected components only. It
     * does not test loss or packet size.
     *
     * @return {@code false} when the source has no position or there is no engine
     */
    public static boolean sourceHasReachableSinks(IEnergySource source) {
        Level level = source instanceof BlockEntity block ? block.getLevel()
            : source instanceof ILocatableTile located ? located.getWorld() : null;
        BlockPos pos = source instanceof BlockEntity block ? block.getBlockPos()
            : source instanceof ILocatableTile located ? located.getPos() : null;
        var engine = engine(level);
        return engine != null && pos != null && engine.hasReachableSink((ServerLevel)level, pos);
    }

    /**
     * Converts a cable tier to a source tier. Source tiers start one above the
     * cable tier index.
     */
    public static int cableTierToSourceTier(CableTier tier) { return tier.getTier() + 1; }

    /**
     * Returns the packet size in EU of a source tier: {@code 8 * 4^tier}.
     * A tier above 60 is treated as 60. A negative tier returns 0.
     */
    public static double getPowerFromTier(int tier) {
        if (tier < 0) return 0;
        // scalb(8, 2 * tier) = 8 * 2^(2 * tier) = 8 * 4^tier, without overflow for tier <= 60.
        return Math.scalb(8.0, Math.min(60, tier) * 2);
    }

    /**
     * Returns the lowest tier whose packet size is equal to or more than
     * {@code power}. The result is in {@code [0, 60]}.
     *
     * @param power a packet size in EU, finite and {@code >= 0}
     * @throws IllegalArgumentException when {@code power} is not finite or is negative
     */
    public static int getTierFromPower(double power) {
        if (!Double.isFinite(power) || power < 0) throw new IllegalArgumentException("Invalid source power");
        int tier = 0;
        // At most 61 steps. The loop stops at tier 60 for a very large power.
        while (tier < 60 && getPowerFromTier(tier) < power) tier++;
        return tier;
    }
}
