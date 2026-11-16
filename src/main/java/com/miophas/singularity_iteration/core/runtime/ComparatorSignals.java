// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime;

import com.miophas.singularity_iteration.core.api.machine.IComparatorSource;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 0.1.11.13: sends a comparator source's signal to its neighbours when it changes. Called from {@link TickMarks#saved}
 * (the per-tick save mark of most machines) and by tickers that do not use it. Server thread only.
 */
public final class ComparatorSignals {
    private ComparatorSignals() { }

    private static final Map<BlockEntity, Integer> LAST = new WeakHashMap<>();

    public static void refresh(BlockEntity tile) {
        if (!(tile instanceof IComparatorSource source) || !(tile.getLevel() instanceof ServerLevel level) || tile.isRemoved()) return;
        int signal = source.comparatorSignal();
        Integer last = LAST.put(tile, signal);
        if (last == null || last != signal) level.updateNeighbourForOutputSignal(tile.getBlockPos(), tile.getBlockState().getBlock());
    }
}
