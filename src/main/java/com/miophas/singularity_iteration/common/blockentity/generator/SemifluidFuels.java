package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 半流质燃料表，对齐原版 IC2 1.12.2（build 2.8.221）的燃料数据。
 *
 * <p>参照 IC2 源码：
 * <ul>
 *   <li>{@code TileEntitySemifluidGenerator.init()} 的
 *       {@code addFuel(name, energyPerMb, energyPerTick)}：oil 16/8、fuel 128/32、
 *       biomass 8/8、bio.ethanol 128/32、ic2biogas 32/16、ic2creosote 3/8。</li>
 *   <li>{@code TileEntityFluidHeatGenerator.init()} 的
 *       {@code addFuel(name, millibucketsPerCycle, heatPerTick)}：oil 10/32、fuel 5/768、
 *       biomass 20/16、bio.ethanol 10/32、ic2biogas 10/32。</li>
 * </ul>
 *
 * <p>本模组的流体命名与 IC2 不同，按语义一一对应：
 * {@code ic2biogas ≈ biogas}、{@code biomass = biomass}、
 * {@code oil ≈ crudeoil}、{@code fuel ≈ dieseloil}。
 * 因此半流质发电机与流体加热机同时支持沼气、生物质、原油与柴油四种半流质燃料，
 * 对应的燃烧值完全取自 IC2 的燃料表。
 */
public final class SemifluidFuels {

    private SemifluidFuels() {
    }

    /** 发电机侧参数：每 mB 产生的 EU 与每秒产生的 EU/tick。 */
    public record GeneratorValue(long energyPerMb, long energyPerTick) {
    }

    /** 加热机侧参数：每 20 tick 燃烧的 mB 数与每秒产生的 HU/tick。 */
    public record HeatValue(int millibucketsPerCycle, int heatPerTick) {
        /** 每 mB 产生的 HU。IC2: {@code heatPerTick * 20 / millibucketsPerCycle}。 */
        public long heatPerMb() {
            return (long) heatPerTick * 20L / Math.max(1, millibucketsPerCycle);
        }
    }

    private record Fuel(GeneratorValue generator, HeatValue heat) {
    }

    // 延迟构建：建表时才读取 DeferredHolder.get()，避免在流体注册前触发。
    private static volatile Map<Fluid, Fuel> table;

    private static Map<Fluid, Fuel> table() {
        Map<Fluid, Fuel> cache = table;
        if (cache != null) {
            return cache;
        }
        synchronized (SemifluidFuels.class) {
            if (table != null) {
                return table;
            }
            Map<Fluid, Fuel> map = new LinkedHashMap<>();
            // ic2biogas：半流质发电机 32 EU/mB · 16 EU/t；流体加热机 10 mB/20t · 32 HU/t
            map.put(mio_icif_fluids.BIOGAS.get(),
                new Fuel(new GeneratorValue(32, 16), new HeatValue(10, 32)));
            // biomass：8 EU/mB · 8 EU/t；20 mB/20t · 16 HU/t
            map.put(mio_icif_fluids.BIOMASS.get(),
                new Fuel(new GeneratorValue(8, 8), new HeatValue(20, 16)));
            // oil ≈ crudeoil：16 EU/mB · 8 EU/t；10 mB/20t · 32 HU/t
            map.put(mio_icif_fluids.CRUDEOIL.get(),
                new Fuel(new GeneratorValue(16, 8), new HeatValue(10, 32)));
            // fuel ≈ dieseloil：128 EU/mB · 32 EU/t；5 mB/20t · 768 HU/t
            map.put(mio_icif_fluids.DIESELOIL.get(),
                new Fuel(new GeneratorValue(128, 32), new HeatValue(5, 768)));
            table = Map.copyOf(map);
            return table;
        }
    }

    /** 该流体是否为半流质燃料。 */
    public static boolean accepts(@Nullable Fluid fluid) {
        return fluid != null && table().containsKey(fluid);
    }

    /** 半流质发电机燃烧参数；非燃料返回 null。 */
    @Nullable
    public static GeneratorValue generatorValue(@Nullable Fluid fluid) {
        Fuel fuel = fluid == null ? null : table().get(fluid);
        return fuel == null ? null : fuel.generator();
    }

    /** 流体加热机燃烧参数；非燃料返回 null。 */
    @Nullable
    public static HeatValue heatValue(@Nullable Fluid fluid) {
        Fuel fuel = fluid == null ? null : table().get(fluid);
        return fuel == null ? null : fuel.heat();
    }

    /** 所有被接受的半流质燃料。 */
    public static Set<Fluid> acceptedFluids() {
        return table().keySet();
    }
}
