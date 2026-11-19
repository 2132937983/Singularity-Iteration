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
 *
 * <p>A heat block is one whose block entity implements {@link IHeatStorage}, {@link IHeatSource} or
 * {@link IHeatConductor}, or one that exposes
 * {@link com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities#HEAT_STORAGE_BLOCK} on some face.
 * SI's own heat blocks (heat generators, Stirling generators, boilers, blast furnaces, reactors...) are of the second
 * kind; this API sees them since 0.1.11.87. Their storage can differ by face: the storage access moves HU through the
 * first face that takes or gives it. To move HU through one particular face, query the capability on that face.
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
     * @return the directions whose neighbour is a heat block; a neighbour that only exposes the capability counts when
     *         it does so on its face turned to {@code pos}
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