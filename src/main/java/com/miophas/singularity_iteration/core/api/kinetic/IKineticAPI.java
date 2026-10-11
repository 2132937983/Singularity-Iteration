package com.miophas.singularity_iteration.core.api.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Kinetic energy (KU) API.
 *
 * <p>Gives access to the mio_icif kinetic system. It lets you:
 * <ul>
 *   <li>Query the kinetic state of a block</li>
 *   <li>Get kinetic storage data</li>
 *   <li>Insert or extract kinetic energy</li>
 *   <li>Query kinetic connections</li>
 * </ul>
 */
public interface IKineticAPI {

    /**
     * Gets the kinetic storage at a position.
     *
     * @param world the level
     * @param pos the block position
     * @return the kinetic storage access; an empty Optional if the position has no kinetic block or the storage is not available
     */
    Optional<IKineticStorageAccess> getKineticStorage(Level world, BlockPos pos);

    /**
     * Checks if a position has a kinetic block.
     *
     * @param world the level
     * @param pos the block position
     * @return true if the position has a kinetic block
     */
    boolean hasKineticTile(Level world, BlockPos pos);

    /**
     * Gets the kinetic connection directions at a position.
     *
     * @param world the level
     * @param pos the block position
     * @return the collection of connected directions
     */
    Collection<Direction> getKineticConnections(Level world, BlockPos pos);

    /**
     * Puts kinetic energy into the kinetic storage at a position.
     *
     * @param world the level
     * @param pos the block position
     * @param amount the kinetic energy to insert (KU); must be ≥ 0
     * @param simulate if true, only simulate; nothing changes
     * @return the kinetic energy that was inserted (KU); 0 if the position has no kinetic block
     */
    default long insertKinetic(Level world, BlockPos pos, long amount, boolean simulate) {
        return getKineticStorage(world, pos).map(s -> s.insertKinetic(amount, simulate)).orElse(0L);
    }

    /**
     * Takes kinetic energy from the kinetic storage at a position.
     *
     * @param world the level
     * @param pos the block position
     * @param amount the kinetic energy to extract (KU); must be ≥ 0
     * @param simulate if true, only simulate; nothing changes
     * @return the kinetic energy that was extracted (KU); 0 if the position has no kinetic block
     */
    default long extractKinetic(Level world, BlockPos pos, long amount, boolean simulate) {
        return getKineticStorage(world, pos).map(s -> s.extractKinetic(amount, simulate)).orElse(0L);
    }
}
