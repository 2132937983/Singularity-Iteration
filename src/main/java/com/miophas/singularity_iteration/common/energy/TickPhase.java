// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 0.1.11.22: periodic work spread over the period by position, so that machines on the same "every N ticks" timer do
 * not all do it on the same tick.
 */
public final class TickPhase {
    private TickPhase() {}

    /** The tick's offset for a position in a period of {@code period} ticks. */
    public static int of(BlockPos pos, int period) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        return (int) Math.floorMod(h ^ (h >>> 29), (long) period);
    }

    /** True once every {@code period} ticks, on a tick that depends on the position. */
    public static boolean every(Level level, BlockPos pos, int period) {
        return Math.floorMod(level.getGameTime() + of(pos, period), (long) period) == 0;
    }
}
