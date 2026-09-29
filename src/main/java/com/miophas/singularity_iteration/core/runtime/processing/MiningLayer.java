// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Candidate ring order over coordinates only. World reads occur later under the machine's tick budget. */
public final class MiningLayer {
    public static final int MAX_RADIUS = 32;
    public static final int MAX_POSITIONS = (2 * MAX_RADIUS + 1) * (2 * MAX_RADIUS + 1);
    private MiningLayer() { }
    public static List<BlockPos> positions(BlockPos center, int radius) {
        if (radius < 0 || radius > MAX_RADIUS) throw new IllegalArgumentException("Invalid candidate mining radius");
        var positions = new ArrayList<BlockPos>((2 * radius + 1) * (2 * radius + 1));
        for (int r = 0; r <= radius; r++)
            for (int x = -r; x <= r; x++)
                for (int z = -r; z <= r; z++)
                    if (Math.abs(x) == r || Math.abs(z) == r) positions.add(center.offset(x, 0, z));
        return positions;
    }
    public static boolean contains(BlockPos center, BlockPos position) {
        return center.getY() == position.getY()
            && Math.abs((long) center.getX() - position.getX()) <= MAX_RADIUS
            && Math.abs((long) center.getZ() - position.getZ()) <= MAX_RADIUS;
    }
    public static int cycleBudget(int overclockers) {
        return (int) Math.min(MAX_POSITIONS, 5L * (Math.max(0, overclockers) + 1L));
    }
}
