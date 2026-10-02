package com.miophas.singularity_iteration.common.energy;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeProfile;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry;

/** Built-in balance and migration policies; the core engine has no knowledge of this content catalog. */
public final class BuiltinEnergyProfiles {
    private BuiltinEnergyProfiles() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    
    private static final ResourceLocation BATBOX = id("wiring/block_bat_box");
    private static final ResourceLocation GENERATOR = id("generator/block_thermal_generator");
    private static final ResourceLocation GEOTHERMAL = id("generator/block_geo_generator");
    private static final ResourceLocation RTG = id("generator/block_rt_generator");
    private static final ResourceLocation SOLAR = id("generator/block_solar_generator");
    private static final ResourceLocation STIRLING = id("generator/block_stirling_generator");
    // Its creative direct-output protocol deliberately does not debit the
    // small display buffer. Treating that buffer as an ordinary generator's
    // entire offer would silently cap the existing creative supply at 100k EU.
    private static final ResourceLocation CREATIVE_GENERATOR = id("generator/block_unlimit_generator");
    private static final Set<ResourceLocation> AMBIENT_GENERATORS = Set.of(
        id("generator/block_wind_generator"), id("generator/block_water_generator"));
    // Candidate limits retained from the allowed SI constructors; fuel behavior awaits grouped reference validation.
    private static final Map<ResourceLocation, Long> FUEL_GENERATOR_PACKETS = Map.of(
        id("generator/block_semifluid_generator"), 16L,
        id("generator/block_advanced_semifluid_generator"), 128L,
        id("generator/block_diesel_generator"), 512L);
    private static final Map<ResourceLocation, Long> WORLD_FUEL_GENERATOR_PACKETS = Map.of(
        id("generator/block_drop_generator"), 128L,
        id("generator/block_advanced_drop_generator"), 512L,
        id("generator/block_experience_generator"), 512L,
        id("generator/block_advanced_experience_generator"), 2048L);
    private static final Map<ResourceLocation, Long> GENERATOR_PACKETS = generatorPackets();
    private static final Set<ResourceLocation> GENERATORS = GENERATOR_PACKETS.keySet();
    private static Map<ResourceLocation, Long> generatorPackets() {
        var packets = new HashMap<ResourceLocation, Long>();
        for (var type : List.of(GENERATOR, GEOTHERMAL, RTG, SOLAR)) packets.put(type, 32L);
        // The kinetic conversion family is registered as dynamic-tier (see
        // DYNAMIC_TIER_GENERATORS), so the engine derives its packet from the live
        // source tier each refresh. These entries are only the static fallback and
        // equal each machine's maximum dynamic tier (never the stale 4096 that
        // overrode the tier and melted EV wires while leaving IV intact).
        packets.put(id("generator/block_kinetic_generator"), 512L);             // dynamic ceiling: tier 3 (HV)
        packets.put(id("generator/block_turbo_kinetic_generator"), 2048L);      // dynamic ceiling: tier 4 (EV)
        packets.put(id("generator/block_twin_turbo_kinetic_generator"), 8192L); // dynamic ceiling: tier 5 (IV)
        packets.put(id("generator/block_nuclear_reactor_generator"), 8192L); // SI public extraction ceiling; actual per-tick offer is demand quoted.
        packets.put(STIRLING, 128L); // Retained public SI packet setting; reference maximum remains open.
        for (var type : AMBIENT_GENERATORS) packets.put(type, 32L);
        packets.putAll(FUEL_GENERATOR_PACKETS);
        packets.putAll(WORLD_FUEL_GENERATOR_PACKETS);
        for (var profile : SolarGeneratorProfile.all())
            packets.put(ResourceLocation.parse(profile.registryId()), profile.outputPacket());
        return Map.copyOf(packets);
    }
    private static final Set<ResourceLocation> TRANSFORMERS = Set.of(id("wiring/transformer_lv_mv"),
        id("wiring/transformer_mv_hv"), id("wiring/transformer_hv_ev"), id("wiring/transformer_ev_sc"),
        id("wiring/transformer_iv_luv"), id("wiring/transformer_luv_zpmv"));
    private static final Set<ResourceLocation> SPECIAL_CABLES = Set.of(id("wiring/block_eu_detector_cable"), id("wiring/block_eu_splitter_cable"));
    // IC2 TileEntityConversionGenerator semantics: a KU->EU conversion generator reports a
    // packet of min(live offer, getPowerFromTier(live source tier)), so an EV wire melts only
    // above its own rated voltage instead of at one fixed packet size.
    private static final Set<ResourceLocation> DYNAMIC_TIER_GENERATORS = Set.of(
        id("generator/block_kinetic_generator"),
        id("generator/block_turbo_kinetic_generator"),
        id("generator/block_twin_turbo_kinetic_generator"));
    private static final Set<ResourceLocation> EXTENDED_PROCESSORS = Set.of(id("producer/block_matter_elc"), id("hugenerator/block_heat_generator_elc"), id("producer/block_scanner_elc"),
        id("producer/block_replicator_elc"), id("producer/block_centrifuge_elc"),
        id("producer/block_washer_elc"), id("producer/block_molecular_transformer"),
        id("producer/block_compressor_advanced_elc"), id("producer/block_powder_advanced_elc"),
        id("producer/block_oil_refinery_elc"), id("producer/block_metal_former"),
        id("producer/block_metal_former_advanced"), id("producer/block_block_cutter"),
        id("producer/block_recycler_elc"), id("producer/block_induction_elc"),
        id("producer/block_miner_elc"), id("producer/block_advanced_miner_elc"), id("producer/block_electrolyzer_elc"), id("producer/block_condenser"), id("producer/block_pump_elc"), id("producer/block_pattern_storage"));
    // R130 reviewed ordinary consumers: one owned CustomEU balance, six input faces.
    // R134 also admits the three consumers after their internal payment repairs.
    private static final Set<ResourceLocation> ORDINARY_CONSUMERS = Set.of(
        id("producer/block_canner_elc"),
        id("producer/block_neutron_polymerizer"),
        id("producer/block_redstone_reactor_coolant_injector"),
        id("producer/block_lapis_reactor_coolant_injector"),
        id("producer/block_terra_elc"),
        id("producer/block_matron_elc"),
        id("producer/block_harvest_elc"),
        id("producer/block_teleporter_elc"),
        id("producer/block_sorter_elc"),
        id("producer/block_fluid_regulator_elc"),
        id("producer/block_batch_crafter"), id("producer/block_tesla"), id("producer/block_chunk_loader"),
        id("producer/block_laser_defense_tower"), id("producer/block_sky_patrol_laser_tower"),
        id("producer/block_blast_furnace_elc"), id("producer/block_magnetizer"), id("producer/block_future_elc"),
        id("producer/block_armory"));
    private static final Set<ResourceLocation> BASIC_PROCESSORS = Set.of(id("producer/block_furnace_elc"),
        id("producer/block_powder_elc"), id("producer/block_extractor_elc"), id("producer/block_compressor_elc"));
    // Packet sizes are explicit public-game observations, not old grid tiers.
    private static final Map<ResourceLocation, Long> STORAGE_PACKETS = Map.of(
        BATBOX, 32L, id("wiring/block_cesu"), 128L,
        id("wiring/block_mfe"), 512L, id("wiring/block_mfsu"), 2048L);
    private static final Set<ResourceLocation> ENDPOINTS = Set.of(BATBOX, GENERATOR, GEOTHERMAL, RTG, SOLAR, id("producer/block_furnace_elc"),
        id("wiring/block_cesu"), id("wiring/block_mfe"), id("wiring/block_mfsu"),
        id("producer/block_powder_elc"), id("producer/block_extractor_elc"), id("producer/block_compressor_elc"));
    private static final Map<ResourceLocation, Long> CONDUCTORS = Map.ofEntries(
        Map.entry(id("wiring/cable/block_cable"), 200L), Map.entry(id("wiring/cable/block_cable_o"), 200L),
        Map.entry(id("wiring/cable/block_tin_cable"), 200L), Map.entry(id("wiring/cable/block_tin_cable_1"), 200L),
        Map.entry(id("wiring/cable/block_gold_cable"), 400L), Map.entry(id("wiring/cable/block_gold_cable_1"), 400L),
        Map.entry(id("wiring/cable/block_gold_cable_2"), 400L),
        Map.entry(id("wiring/cable/block_iron_cable"), 800L), Map.entry(id("wiring/cable/block_iron_cable_1"), 800L),
        Map.entry(id("wiring/cable/block_iron_cable_2"), 800L), Map.entry(id("wiring/cable/block_iron_cable_3"), 800L),
        Map.entry(id("wiring/cable/block_glass_cable"), 25L),
        Map.entry(id("wiring/block_eu_detector_cable"), 500L), Map.entry(id("wiring/block_eu_splitter_cable"), 500L),
        // The wireless node also passes native energy through its position (IC2 dual-node
        // layout), so a cable run can be continued by swapping one cable for a node.
        Map.entry(id("wiring/block_wireless_power_transmission_node"), 0L));
    // Only measured fuse thresholds are enabled. Iron/glass ultimate limits
    // remain open, including extended-solar candidate packets up to 8192 EU.
    private static final Map<ResourceLocation, Long> FUSE_LIMITS = Map.of(
        id("wiring/cable/block_tin_cable"), 33L, id("wiring/cable/block_tin_cable_1"), 33L,
        id("wiring/cable/block_cable"), 129L, id("wiring/cable/block_cable_o"), 129L,
        id("wiring/cable/block_gold_cable"), 513L, id("wiring/cable/block_gold_cable_1"), 513L);
    private static final Set<ResourceLocation> MEASURED_BLAST_CABLES = Set.of(
        id("wiring/cable/block_tin_cable_1"), id("wiring/cable/block_cable"),
        id("wiring/cable/block_gold_cable_1"), id("wiring/cable/block_glass_cable"));

    public static void install() {
        GENERATOR_PACKETS.forEach((id, packet) -> EnergyNodeRegistry.register(id,
            EnergyNodeProfile.generator(packet, DYNAMIC_TIER_GENERATORS.contains(id))));
        STORAGE_PACKETS.forEach((id, packet) -> EnergyNodeRegistry.register(id, EnergyNodeProfile.storage(packet)));
        CONDUCTORS.forEach((id, loss) -> EnergyNodeRegistry.register(id, new EnergyNodeProfile(
            EnergyNodeProfile.Role.CONDUCTOR, 0, loss, FUSE_LIMITS.getOrDefault(id, -1L), false,
            SPECIAL_CABLES.contains(id), MEASURED_BLAST_CABLES.contains(id), false)));
        BASIC_PROCESSORS.forEach(id -> EnergyNodeRegistry.register(id, new EnergyNodeProfile(
            EnergyNodeProfile.Role.CONSUMER, 0, -1, -1, true, false, false, false)));
        EnergyNodeRegistry.registerAdmissionPolicy(state -> {
            var type = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return "mio_icif".equals(type.getNamespace())
                && !type.equals(id("producer/block_fluid_distributor_elc"))
                && !type.equals(id("producer/block_weighted_fluid_distributor_elc"));
        });
    }
}
