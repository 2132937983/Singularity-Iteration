// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/**
 * 0.1.11.17: the Stirling engines (Stirling generator, advanced Stirling generator, Stirling kinetic generator).
 *
 * <p>The engine works between the heater head (hot) and the cooler (cold). Its efficiency is a fixed share of the Carnot
 * efficiency {@code 1 - Tc/Th}; the machines' old ratios (2 HU = 1 EU, 100 HU = 85 EU, 4 HU = 12 KU) are the ratios at
 * the design point, heater {@link #DESIGN_HOT} °C and cooler {@link #DESIGN_COLD} °C, and the ratio scales by the Carnot
 * efficiency against the design one ({@link #relative}), between {@link #MIN_RELATIVE} and {@link #MAX_RELATIVE}.
 *
 * <ul>
 *   <li>Heater: its temperature follows the heat flowing through it: {@code Tc + 610 * share^0.15} for a share of the
 *   rated heat flow (650 °C at the rated flow with a 40 °C cooler, about 530 °C at a quarter), with a time constant of
 *   {@link #HEATER_TAU} ticks (a cold engine warms up).</li>
 *   <li>Cooler: the ambient air of the biome ({@link SolarPhysics#ambient}) plus 22 K at the rated flow; with water beside
 *   it (a water block, or the kinetic generator's cooling water) at most 15 °C plus 8 K.</li>
 * </ul>
 */
public final class StirlingPhysics {
    private StirlingPhysics() {}

    public static final double DESIGN_HOT = 650, DESIGN_COLD = 40, HOT_RISE = 610;
    public static final double MIN_RELATIVE = 0.4, MAX_RELATIVE = 1.1;
    public static final double HEATER_TAU = 100, FLOW_TAU = 20;
    public static final double AIR_RISE = 22, WATER_TEMPERATURE = 15, WATER_RISE = 8;

    public static double carnot(double hot, double cold) {
        return Math.max(0, 1 - (cold + 273.15) / (hot + 273.15));
    }

    public static final double DESIGN_CARNOT = carnot(DESIGN_HOT, DESIGN_COLD);

    /** The ratio factor against the design point. */
    public static double relative(double hot, double cold) {
        return Math.max(MIN_RELATIVE, Math.min(MAX_RELATIVE, carnot(hot, cold) / DESIGN_CARNOT));
    }

    /** Heater temperature the heat flow drives it to. */
    public static double heaterTarget(double cold, double share) {
        return cold + HOT_RISE * Math.pow(Math.max(0, Math.min(1.5, share)), 0.15);
    }

    public static double coolerTemperature(double ambient, boolean water, double share) {
        double s = Math.max(0, Math.min(1.5, share));
        return water ? Math.min(ambient, WATER_TEMPERATURE) + WATER_RISE * s : ambient + AIR_RISE * s;
    }

    /** Biome air temperature at a position (°C). */
    public static double ambient(Level level, BlockPos pos) {
        return SolarPhysics.ambient(level.getBiome(pos).value().getBaseTemperature(), level.isRainingAt(pos.above()));
    }

    /** True when a water block (source or flowing, or waterlogged) touches the machine. */
    public static boolean waterBeside(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            var at = pos.relative(d);
            if (!level.isLoaded(at)) continue;
            if (level.getFluidState(at).is(Fluids.WATER) || level.getFluidState(at).is(Fluids.FLOWING_WATER)) return true;
        }
        return false;
    }

    /**
     * The heater and cooler of one engine: call {@link #tick} once a game tick with the heat that went through the engine
     * that tick; {@link #factor()} is the ratio factor to apply to that heat.
     */
    public static final class Head {
        private final double ratedHu;
        private double hot = Double.NaN, cold = Double.NaN, flow, ambient = 18;
        private boolean water;

        public Head(double ratedHu) { this.ratedHu = ratedHu; }

        /** @param hu heat through the engine this tick; {@code waterCooled} when cooled by water whatever is beside it */
        public void tick(Level level, BlockPos pos, double hu, boolean waterCooled) {
            if (level.getGameTime() % 20 == 0 || Double.isNaN(hot)) {
                ambient = ambient(level, pos);
                water = waterCooled || waterBeside(level, pos);
            }
            if (Double.isNaN(hot)) { hot = ambient; cold = ambient; }
            flow += (hu - flow) / FLOW_TAU;
            double share = flow / ratedHu;
            double coldTarget = coolerTemperature(ambient, water, share);
            cold += (coldTarget - cold) / FLOW_TAU;
            double target = share > 0.002 ? heaterTarget(cold, share) : ambient;
            hot += (target - hot) / HEATER_TAU;
        }

        public double factor() {
            return Double.isNaN(hot) ? MIN_RELATIVE : relative(hot, cold);
        }

        public double hot() { return Double.isNaN(hot) ? ambient : hot; }
        public double cold() { return Double.isNaN(cold) ? ambient : cold; }
        public double flow() { return flow; }
        public double share() { return flow / ratedHu; }
        public boolean water() { return water; }
        public double ambientTemperature() { return ambient; }

        /** Puts the head at its rated running state (the design point): for tests. */
        public void warm(double hu) { hot = DESIGN_HOT; cold = DESIGN_COLD; flow = hu; ambient = 18; }

        public void save(CompoundTag tag) {
            if (Double.isNaN(hot)) return;
            var t = new CompoundTag();
            t.putDouble("hot", hot);
            t.putDouble("cold", cold);
            t.putDouble("flow", flow);
            tag.put("scex_stirling_head", t);
        }

        public void load(CompoundTag tag) {
            if (!tag.contains("scex_stirling_head")) return;
            var t = tag.getCompound("scex_stirling_head");
            hot = t.getDouble("hot");
            cold = t.getDouble("cold");
            flow = t.getDouble("flow");
        }
    }
}
