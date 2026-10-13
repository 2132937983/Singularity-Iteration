// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Output multiplier of every EU generator (0.1.8.4, common config section {@code GeneratorOutput}).
 *
 * <p>One value per generator, keyed by its block entity id: 1.0 is the stock output, 2.0 doubles
 * the EU per tick, 0.5 halves it, 0 stops the generator. Fuel generators burn their fuel at the
 * same speed, so the multiplier is also the EU per unit of fuel. The value applies where each
 * generator decides its output (the rate it feeds to its energy planner), so fuel accounting and
 * storage room stay consistent. Fractions of an EU are carried to later ticks.
 *
 * <p>Kinetic (KU) and heat (HU) sources are not listed: they make no EU. Their EU comes out of the
 * kinetic, turbo kinetic and Stirling generators, which are listed.
 */
public final class GeneratorOutput {
    private GeneratorOutput() {}

    /** One configurable generator: block entity id and a short note on its stock output. */
    public record Entry(String id, String note) {}

    public static final List<Entry> GENERATORS = List.of(
        new Entry("solar_generator", "Solar Generator: 1 EU/t in daylight"),
        new Entry("advanced_solar_panel", "Advanced Solar Panel: 8 EU/t day, 1 EU/t night"),
        new Entry("hybrid_solar_panel", "Hybrid Solar Panel: 64 EU/t day, 8 EU/t night"),
        new Entry("ultimate_hybrid_solar_panel", "Ultimate Hybrid Solar Panel: 512 EU/t day, 64 EU/t night"),
        new Entry("quantum_solar_panel", "Quantum Solar Panel: 4096 EU/t day, 2048 EU/t night"),
        new Entry("mets_advanced_solar_generator", "Advanced Solar Generator (METS): up to 64 EU/t by sky light"),
        new Entry("photon_resonance_solar_generator", "Photon Resonance Solar Generator: up to 512 EU/t by sky light"),
        new Entry("ultimate_photon_resonance_solar_generator", "Ultimate Photon Resonance Solar Generator: up to 4096 EU/t by sky light"),
        new Entry("wind_generator", "Wind Mill: by height, obstruction and weather"),
        new Entry("water_generator", "Water Mill: by surrounding water and water buckets"),
        new Entry("geo_generator", "Geothermal Generator: 20 EU/t from lava, plus adjacent lava blocks"),
        new Entry("geomagnetic_generator", "Geomagnetic Generator: up to 30720 EU/t"),
        new Entry("nuclear_reactor_generator", "Nuclear Reactor (EU mode): by the reactor layout"),
        new Entry("rt_generator", "Radioisotope Thermoelectric Generator: 2 to 64 EU/t by pellets"),
        new Entry("kinetic_generator", "Kinetic Generator: 1 EU per 4 KU"),
        new Entry("turbo_kinetic_generator", "Turbo Kinetic Generator: 0.85 EU per KU"),
        new Entry("twin_turbo_kinetic_generator", "Twin Turbo Kinetic Generator: 1.2 EU per KU"),
        new Entry("semifluid_generator", "Semifluid Generator: by fuel"),
        new Entry("advanced_semifluid_generator", "Advanced Semifluid Generator: by fuel"),
        new Entry("diesel_generator", "Diesel Generator: 120 EU/t"),
        new Entry("stirling_generator", "Stirling Generator: EU from HU"),
        new Entry("advanced_stirling_generator", "Advanced Stirling Generator: EU from HU"),
        new Entry("thermal_generator", "Generator (furnace fuel): 10 EU/t"),
        new Entry("drop_generator", "Drop Generator: 20 EU/t"),
        new Entry("advanced_drop_generator", "Advanced Drop Generator: 40 EU/t"),
        new Entry("experience_generator", "Experience Generator: 160 EU/t"),
        new Entry("advanced_experience_generator", "Advanced Experience Generator: 320 EU/t"),
        new Entry("quantum_generator", "Quantum Generator: 512 EU/t (its own setting, then this multiplier)"));

    private static Map<String, ModConfigSpec.DoubleValue> values = Map.of();
    private static ModConfigSpec spec;
    private static final Map<BlockEntity, double[]> CARRY = new WeakHashMap<>();

    /** Defines the section entries on {@code builder} (called once while the common spec is built). */
    public static Map<String, ModConfigSpec.DoubleValue> define(ModConfigSpec.Builder builder) {
        Map<String, ModConfigSpec.DoubleValue> out = new LinkedHashMap<>();
        for (Entry e : GENERATORS) {
            out.put(e.id(), builder.comment(e.note() + ". Output multiplier: 1.0 = stock, 0 = off.")
                .translation("mio_icif.configuration.generator_output." + e.id())
                .defineInRange(e.id(), 1.0, 0.0, 100.0));
        }
        values = Map.copyOf(out);
        return values;
    }

    /** Called after the spec is built, so the values are read only when loaded. */
    public static void bind(ModConfigSpec built) {
        spec = built;
    }

    /** Multiplier of a generator block entity (1 for unknown ones or before the config loads). */
    public static double multiplier(BlockEntity be) {
        if (spec == null || !spec.isLoaded() || be == null) return 1.0;
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        if (id == null || !"mio_icif".equals(id.getNamespace())) return 1.0;
        ModConfigSpec.DoubleValue v = values.get(id.getPath());
        if (v == null) return 1.0;
        double m = v.get();
        return Double.isFinite(m) && m > 0 ? m : 0;
    }

    /** {@code eu} times the multiplier of {@code be}, in whole EU; the fraction is carried (server thread). */
    public static long eu(BlockEntity be, long eu) {
        double m = multiplier(be);
        if (m == 1.0 || eu <= 0) return m == 0 ? 0 : eu;
        double[] carry = CARRY.computeIfAbsent(be, k -> new double[1]);
        double v = eu * m + carry[0];
        long whole = (long) Math.floor(v);
        carry[0] = v - whole;
        return whole;
    }

    /** A per-unit or per-tick rate for an energy planner times the multiplier, rounded, at least 1 if the input was. */
    public static long rate(BlockEntity be, long rate) {
        double m = multiplier(be);
        if (m == 1.0 || rate <= 0) return m == 0 ? 0 : rate;
        return Math.max(1, Math.round(rate * m));
    }

    /** An exact amount (network-controlled storage) times the multiplier. */
    public static com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount exact(
            BlockEntity be, com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount amount) {
        double m = multiplier(be);
        return m == 1.0 ? amount : com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount.fromDouble(amount.toDouble() * m);
    }
}
