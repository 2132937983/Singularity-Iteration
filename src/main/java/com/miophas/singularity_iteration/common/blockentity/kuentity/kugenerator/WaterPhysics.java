// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.18: the physics of the water machines (the water kinetic generator and the water generator), shared by the
 * machines, the screens and the tests.
 *
 * <ul>
 *   <li><b>Sources.</b> A river biome has a steady current (speed 1; rain {@code x cbrt 1.3}, thunder {@code x cbrt 1.6}, so
 *       the power rises by 30 % and 60 %). An ocean or beach has a tidal current: two floods and two ebbs a day,
 *       {@code |sin(2 pi t / 12000)|} with slack water at 0, 6000, 12000 and 18000 ticks, its power averaging the river's;
 *       the moon sets spring and neap tides, {@code 1 + 0.3 cos(phase pi / 2)} (full and new moon 1.3, quarters 0.7).
 *       Flowing water anywhere (a canal) moves at {@link #CANAL_SPEED} along its flow, counted along the rotor's axis;
 *       falling water at {@link #FALL_SPEED}. Still water outside rivers and oceans does not move.</li>
 *   <li><b>Power</b> of a water rotor is the power of the flow through its disc, {@code 1/2 rho A v^3}: the mean of
 *       {@code v^3} over the cells of the disc that are water (cells that are not water give nothing), times the rotor's
 *       mean output {@code (d - 3) x 25 + 100} (150 at sea) — the old mean, so a disc full of water in an average flow gives
 *       what it gave on average before.</li>
 *   <li><b>Wake.</b> A rotor in line in front of another (facing the same way, within 16 blocks) slows the water reaching
 *       it: Jensen's model with the slower recovery of water, {@code (2/3) / (1 + 0.04 x / r)^2}.</li>
 * </ul>
 */
public final class WaterPhysics {
    private WaterPhysics() {}

    public enum Source { NONE, RIVER, OCEAN, CANAL }

    public static final double CANAL_SPEED = 0.85, FALL_SPEED = 1.0;   // a waterfall is easy to build anywhere: no more than a river
    public static final double SPRING = 0.3, RAIN_POWER = 1.3, THUNDER_POWER = 1.6;
    /** Peak tidal speed: the mean of {@code |sin|^3} is {@code 4 / 3 pi}, so this makes the mean power 1. */
    public static final double TIDE_PEAK = Math.cbrt(3 * Math.PI / 4);
    public static final int TIDE_PERIOD = 12000, WAKE_RANGE = 16;

    public static double riverSpeed(boolean rain, boolean thunder) {
        return Math.cbrt(thunder ? THUNDER_POWER : rain ? RAIN_POWER : 1);
    }

    public static double spring(int moonPhase) {
        return 1 + SPRING * Math.cos(Math.floorMod(moonPhase, 8) * Math.PI / 2);
    }

    public static double tideSpeed(long dayTime, int moonPhase) {
        double phase = 2 * Math.PI * Math.floorMod(dayTime, TIDE_PERIOD) / TIDE_PERIOD;
        return TIDE_PEAK * Math.abs(Math.sin(phase)) * Math.cbrt(spring(moonPhase));
    }

    /** The biome's current: a river or an ocean (beaches count as ocean), else none. */
    public static Source biomeSource(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        String name = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (biome.is(BiomeTags.IS_RIVER) || name.contains("river")) return Source.RIVER;
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || name.contains("ocean") || name.contains("beach")) return Source.OCEAN;
        return Source.NONE;
    }

    /** The biome current's speed now. */
    public static double biomeSpeed(Level level, BlockPos pos, Source source) {
        return switch (source) {
            case RIVER -> riverSpeed(level.isRainingAt(pos.above()) || level.isRaining(), level.isThundering());
            case OCEAN -> tideSpeed(level.getDayTime(), level.getMoonPhase());
            default -> 0;
        };
    }

    public static boolean water(FluidState fluid) {
        return fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER);
    }

    /** Speed of the water in one cell along a rotor axis, given the biome current's speed. 0 when it is not water. */
    public static double cellSpeed(Level level, BlockPos pos, Direction axis, double biomeSpeed) {
        FluidState fluid = level.getFluidState(pos);
        if (!water(fluid)) return 0;
        double own = 0;
        if (!fluid.isSource()) {
            if (fluid.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING)) own = FALL_SPEED;
            else {
                Vec3 flow = fluid.getFlow(level, pos);
                own = CANAL_SPEED * Math.abs(flow.x * axis.getStepX() + flow.y * axis.getStepY() + flow.z * axis.getStepZ());
            }
        }
        return Math.max(own, biomeSpeed);
    }

    /** The rotor's output at the mean flow with its disc full of water. */
    public static int meanOutput(int diameter, Source source) {
        return Math.max(0, (diameter - 3) * 25 + (source == Source.OCEAN ? 150 : 100));
    }

    /** Jensen wake deficit of water at {@code distance} blocks behind a rotor of {@code diameter}. */
    public static double wakeDeficit(double distance, double diameter) {
        double r = Math.max(0.5, diameter / 2.0);
        double d = 1 + 0.04 * Math.max(0, distance) / r;
        return (2.0 / 3.0) / (d * d);
    }

    // ---- the disc -------------------------------------------------------------------------------------------------------

    /** Cell kinds of a disc for the screens. */
    public static final int OUTSIDE = 0, STILL = 1, MOVING = 2, AIR = 3, SOLID = 4;
    public static final int MAX_DIAMETER = 11;

    /** The two axes of the plane square to a rotor's facing. */
    public static Direction[] plane(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) return new Direction[]{Direction.EAST, Direction.SOUTH};
        return new Direction[]{facing.getClockWise(), Direction.DOWN};
    }

    public static boolean inDisc(int a, int b, int diameter) {
        double c = (diameter - 1) / 2.0;
        return (a - c) * (a - c) + (b - c) * (b - c) <= (c + 0.5) * (c + 0.5);
    }

    /** The result of scanning a disc: per cell kind (row by row, {@code d x d}), the sum of v^3 and the counts. */
    public record Disc(int diameter, int[] cells, double cubes, int waterCells, int discCells, double meanSpeed) {
        public double share() { return discCells == 0 ? 0 : waterCells / (double) discCells; }
        /** Mean v^3 over the whole disc (cells that are not water count as 0). */
        public double power() { return discCells == 0 ? 0 : cubes / discCells; }
    }

    /** Scans the disc of a rotor whose centre is {@code centre}, square to {@code facing}. */
    public static Disc scan(Level level, BlockPos centre, Direction facing, int diameter, double biomeSpeed) {
        Direction[] axes = plane(facing);
        int c = (diameter - 1) / 2, discCells = 0, waterCells = 0;
        int[] cells = new int[diameter * diameter];
        double cubes = 0, speeds = 0;
        for (int b = 0; b < diameter; b++) {
            for (int a = 0; a < diameter; a++) {
                if (!inDisc(a, b, diameter)) continue;
                discCells++;
                BlockPos at = centre.relative(axes[0], a - c).relative(axes[1], b - c);
                int kind;
                if (!level.isLoaded(at)) kind = SOLID;
                else if (water(level.getFluidState(at))) {
                    double v = cellSpeed(level, at, facing, biomeSpeed);
                    cubes += v * v * v;
                    speeds += v;
                    waterCells++;
                    kind = v > 0.05 ? MOVING : STILL;
                } else kind = level.getBlockState(at).isAir() || !level.getBlockState(at).blocksMotion() ? AIR : SOLID;
                cells[b * diameter + a] = kind;
            }
        }
        return new Disc(diameter, cells, cubes, waterCells, discCells, waterCells == 0 ? 0 : speeds / waterCells);
    }

    /** Packs cell kinds (4 bits each, 8 a value) into menu values. */
    public static int pack(int[] cells, int index) {
        int v = 0;
        for (int k = 0; k < 8; k++) {
            int i = index * 8 + k;
            if (i < cells.length) v |= (cells[i] & 0xF) << (k * 4);
        }
        return v;
    }

    public static int unpack(int value, int k) {
        return (value >>> (k * 4)) & 0xF;
    }

    /** Menu values holding a disc of {@link #MAX_DIAMETER}: 121 cells, 8 a value. */
    public static final int PACKED = (MAX_DIAMETER * MAX_DIAMETER + 7) / 8;
}
