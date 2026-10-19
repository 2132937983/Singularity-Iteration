// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime;

import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 0.1.10.10 (D1): the save mark of a tick. {@code setChanged()} marks the chunk for saving and then
 * updates the six neighbour blocks (it calls {@code onNeighborChange} of each neighbour, which wakes
 * blocks of other mods, and reads the block behind each solid neighbour for comparators). Many
 * tickers called it every tick only to keep their counters saved. {@link #saved} keeps the save mark
 * and drops the neighbour updates.
 *
 * <p>Use it only for a block whose comparator output does not change in that tick (or that has none)
 * and when no neighbour has to react; a state change that neighbours must see still calls
 * {@code setChanged()}.
 */
public final class TickMarks {
    private TickMarks() { }

    /** Marks the chunk of {@code tile} for saving, without the neighbour and comparator updates. */
    public static void saved(BlockEntity tile) {
        ContainerToTank.markUnsaved(tile);
        if (tile.getLevel() != null && !tile.getLevel().isClientSide) PerfCounters.add(PerfCounters.Kind.SAVE_MARKS);
    }
}
