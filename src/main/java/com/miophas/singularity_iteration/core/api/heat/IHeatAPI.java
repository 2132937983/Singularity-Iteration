package com.miophas.singularity_iteration.core.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Heat (HU) API.
 *
 * <p>Use it to interact with the mio_icif heat system. It can:
 * <ul>
 *   <li>query the heat state of a block</li>
 *   <li>get heat storage information</li>
 *   <li>insert and extract heat</li>
 *   <li>query heat connections</li>
 * </ul>
 */
public interface IHeatAPI {

    /**
     * Gets the heat storage at the given position.
     *
     * @param world the world
     * @param pos the block position
     * @return the heat storage access, or an empty Optional if no heat block is at the position or the storage is not available
     */
    Optional<IHeatStorageAccess> getHeatStorage(Level world, BlockPos pos);

    /**
     * Checks if a heat block is at the given position.
     *
     * @param world the world
     * @param pos the block position
     * @return true if a heat block is at the position
     */
    boolean hasHeatTile(Level world, BlockPos pos);

    /**
     * Gets the heat connection directions at the given position.
     *
     * @param world the world
     * @param pos the block position
     * @return the connected directions
     */
    Collection<Direction> getHeatConnections(Level world, BlockPos pos);

    /**
     * Inserts heat into the heat storage at the given position.
     *
     * @param world the world
     * @param pos the block position
     * @param amount the heat to insert, in HU; must be 0 or more
     * @param simulate if true, only simulate; do not change the storage
     * @return the heat that was inserted, in HU; 0 if no heat block is at the position
     */
    default long insertHeat(Level world, BlockPos pos, long amount, boolean simulate) {
        return getHeatStorage(world, pos).map(s -> s.insertHeat(amount, simulate)).orElse(0L);
    }

    /**
     * Extracts heat from the heat storage at the given position.
     *
     * @param world the world
     * @param pos the block position
     * @param amount the heat to extract, in HU; must be 0 or more
     * @param simulate if true, only simulate; do not change the storage
     * @return the heat that was extracted, in HU; 0 if no heat block is at the position
     */
    default long extractHeat(Level world, BlockPos pos, long amount, boolean simulate) {
        return getHeatStorage(world, pos).map(s -> s.extractHeat(amount, simulate)).orElse(0L);
    }
}