// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/**
 * 0.1.11.19: the geothermal generator's lava around it. Lava within {@link #RANGE} blocks heats it: a block touching one
 * of its faces {@link #FACE} EU/t, any other {@link #NEAR}; flowing lava counts half. The deeper the hotter the rock, so
 * the sum is multiplied by {@code 1 + (64 - Y) / 128} (1 at Y 64 and above, 2 at Y -64); in the Nether by
 * {@link #NETHER}. At most {@link #CAP} EU/t.
 */
public final class GeoPhysics {
    private GeoPhysics() {}

    public static final int RANGE = 2;
    public static final double FACE = 1.0, NEAR = 0.5, FLOWING = 0.5, NETHER = 1.5, CAP = 12;

    public static double depth(int y) {
        return 1 + Math.max(0, Math.min(1, (64 - y) / 128.0));
    }

    public static double factor(Level level, BlockPos pos) {
        return level.dimension() == Level.NETHER ? NETHER : depth(pos.getY());
    }

    /** Lava around a position: the sources and flowing blocks, the weighted sum before the factor, the factor and the EU/t. */
    public record Scan(int sources, int flowing, double raw, double factor, double euPerTick) {}

    public static Scan scan(Level level, BlockPos pos) {
        int sources = 0, flowing = 0;
        double raw = 0;
        for (int dx = -RANGE; dx <= RANGE; dx++)
            for (int dy = -RANGE; dy <= RANGE; dy++)
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos at = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(at)) continue;
                    var fluid = level.getFluidState(at);
                    boolean source = fluid.is(Fluids.LAVA), flow = fluid.is(Fluids.FLOWING_LAVA);
                    if (!source && !flow) continue;
                    boolean face = Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1;
                    raw += (face ? FACE : NEAR) * (flow ? FLOWING : 1);
                    if (source) sources++;
                    else flowing++;
                }
        double f = factor(level, pos);
        return new Scan(sources, flowing, raw, f, Math.min(CAP, raw * f));
    }
}
