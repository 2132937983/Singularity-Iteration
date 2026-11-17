// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * 0.1.11.20: the RTG's cold side. A thermoelectric generator makes power from the difference between its hot junctions
 * (the pellets) and its cold side. The pellet table (1, 2, 4, 8, 16, 32 EU/t) is the output with the air of a temperate
 * biome on the cold side; the best of the six faces sets the cold side: ice, packed ice, blue ice, snow or powder snow
 * x{@link #ICE}, water x{@link #WATER}; otherwise the air: a hot biome (35 °C or more) x{@link #HOT}, the Nether
 * x{@link #NETHER}. Lava touching any face costs a further x{@link #LAVA}. The temperatures are for the screen.
 */
public final class RtgPhysics {
    private RtgPhysics() {}

    public static final double ICE = 1.25, WATER = 1.15, HOT = 0.9, NETHER = 0.7, LAVA = 0.6, HOT_AIR = 35;
    public static final double ICE_C = -5, WATER_C = 12, NETHER_C = 80, JUNCTION_RISE = 40, PER_PELLET = 90;
    public static final int AIR = 0, HOT_BIOME = 1, NETHER_AIR = 2, WATER_SIDE = 3, ICE_SIDE = 4;

    /** The cold side: the factor, its temperature, its kind and whether lava touches the machine. */
    public record ColdSide(double factor, double celsius, int kind, boolean lava) {}

    public static ColdSide coldSide(Level level, BlockPos pos) {
        boolean ice = false, water = false, lava = false;
        for (Direction d : Direction.values()) {
            BlockPos at = pos.relative(d);
            if (!level.isLoaded(at)) continue;
            var state = level.getBlockState(at);
            if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))
                ice = true;
            var fluid = level.getFluidState(at);
            if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) water = true;
            if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) lava = true;
        }
        double factor, celsius;
        int kind;
        if (ice) { factor = ICE; celsius = ICE_C; kind = ICE_SIDE; }
        else if (water) { factor = WATER; celsius = WATER_C; kind = WATER_SIDE; }
        else if (level.dimension() == Level.NETHER) { factor = NETHER; celsius = NETHER_C; kind = NETHER_AIR; }
        else {
            celsius = SolarPhysics.ambient(level.getBiome(pos).value().getBaseTemperature(), level.isRainingAt(pos.above()));
            boolean hot = celsius >= HOT_AIR;
            factor = hot ? HOT : 1;
            kind = hot ? HOT_BIOME : AIR;
        }
        if (lava) factor *= LAVA;
        return new ColdSide(factor, celsius, kind, lava);
    }

    /** The hot junctions' temperature for the screen. */
    public static double hot(double cold, int pellets) {
        return pellets <= 0 ? cold : cold + JUNCTION_RISE + PER_PELLET * pellets;
    }
}
