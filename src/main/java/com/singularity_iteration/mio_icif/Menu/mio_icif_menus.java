package com.singularity_iteration.mio_icif.Menu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Menu.Generator.*;
import com.singularity_iteration.mio_icif.Menu.HUEntity.*;
import com.singularity_iteration.mio_icif.Menu.KUEntity.*;
import com.singularity_iteration.mio_icif.Menu.Producer.*;
import com.singularity_iteration.mio_icif.Menu.OilRig.OilRigPanelMenu;
import com.singularity_iteration.mio_icif.Menu.Storage.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_menus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(BuiltInRegistries.MENU, Singularity_Iteration.MOD_ID);

    // ===== Generator =====
    public static final DeferredHolder<MenuType<?>, MenuType<ThermalGeneratorMenu>> THERMAL_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("thermal_generator_menu", () -> new MenuType<>(ThermalGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<GeoGeneratorMenu>> GEO_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("geo_generator_menu", () -> new MenuType<>(GeoGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarGeneratorMenu>> SOLAR_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("solar_generator_menu", () -> new MenuType<>(SolarGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<GeomagneticGeneratorMenu>> GEOMAGNETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("geomagnetic_generator_menu", () -> new MenuType<>(GeomagneticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<DieselGeneratorMenu>> DIESEL_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("diesel_generator_menu", () -> new MenuType<>(DieselGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WindGeneratorMenu>> WIND_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("wind_generator_menu", () -> new MenuType<>(WindGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WaterGeneratorMenu>> WATER_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("water_generator_menu", () -> new MenuType<>(WaterGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SemifluidGeneratorMenu>> SEMIFLUID_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("semifluid_generator_menu", () -> new MenuType<>(SemifluidGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StirlingGeneratorMenu>> STIRLING_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("stirling_generator_menu", () -> new MenuType<>(StirlingGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<KineticGeneratorMenu>> KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("kinetic_generator_menu", () -> new MenuType<>(KineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<TurboKineticGeneratorMenu>> TURBO_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("turbo_kinetic_generator_menu", () -> new MenuType<>(TurboKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<TwinTurboKineticGeneratorMenu>> TWIN_TURBO_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("twin_turbo_kinetic_generator_menu", () -> new MenuType<>(TwinTurboKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WindKineticGeneratorMenu>> WIND_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("wind_kinetic_generator_menu", () -> new MenuType<>(WindKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WaterKineticGeneratorMenu>> WATER_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("water_kinetic_generator_menu", () -> new MenuType<>(WaterKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StirlingKineticGeneratorMenu>> STIRLING_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("stirling_kinetic_generator_menu", () -> new MenuType<>(StirlingKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamKineticGeneratorMenu>> STEAM_KINETIC_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("steam_kinetic_generator_menu", () -> new MenuType<>(SteamKineticGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<NuclearReactorGeneratorMenu>> NUCLEAR_REACTOR_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("nuclear_reactor_generator_menu", () -> new MenuType<>(NuclearReactorGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorFluidPortMenu>> REACTOR_FLUID_PORT_MENU_TYPE =
        MENU_TYPES.register("reactor_fluid_port_menu", () -> new MenuType<>(ReactorFluidPortMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidReactorMenu>> FLUID_REACTOR_MENU_TYPE =
        MENU_TYPES.register("fluid_reactor_menu", () -> new MenuType<>(FluidReactorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<NukeMenu>> NUKE_MENU_TYPE =
        MENU_TYPES.register("nuke_menu", () -> new MenuType<>(NukeMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<RTGeneratorMenu>> RT_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("rt_generator_menu", () -> new MenuType<>(RTGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== Advanced Solar Panels =====
    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedSolarPanelMenu>> ADVANCED_SOLAR_PANEL_MENU_TYPE =
        MENU_TYPES.register("advanced_solar_panel_menu", () -> new MenuType<>(AdvancedSolarPanelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<HybridSolarPanelMenu>> HYBRID_SOLAR_PANEL_MENU_TYPE =
        MENU_TYPES.register("hybrid_solar_panel_menu", () -> new MenuType<>(HybridSolarPanelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<UltimateHybridSolarPanelMenu>> ULTIMATE_HYBRID_SOLAR_PANEL_MENU_TYPE =
        MENU_TYPES.register("ultimate_hybrid_solar_panel_menu", () -> new MenuType<>(UltimateHybridSolarPanelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<QuantumSolarPanelMenu>> QUANTUM_SOLAR_PANEL_MENU_TYPE =
        MENU_TYPES.register("quantum_solar_panel_menu", () -> new MenuType<>(QuantumSolarPanelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MetsAdvancedSolarGeneratorMenu>> METS_ADVANCED_SOLAR_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("mets_advanced_solar_generator_menu", () -> new MenuType<>(MetsAdvancedSolarGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PhotonResonanceSolarGeneratorMenu>> PHOTON_RESONANCE_SOLAR_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("photon_resonance_solar_generator_menu", () -> new MenuType<>(PhotonResonanceSolarGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<UltimatePhotonResonanceSolarGeneratorMenu>> ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("ultimate_photon_resonance_solar_generator_menu", () -> new MenuType<>(UltimatePhotonResonanceSolarGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<QuantumGeneratorMenu>> QUANTUM_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("quantum_generator_menu", () -> new MenuType<>(QuantumGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<DropGeneratorMenu>> DROP_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("drop_generator_menu", () -> new MenuType<>(DropGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedDropGeneratorMenu>> ADVANCED_DROP_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("advanced_drop_generator_menu", () -> new MenuType<>(AdvancedDropGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedStirlingGeneratorMenu>> ADVANCED_STIRLING_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("advanced_stirling_generator_menu", () -> new MenuType<>(AdvancedStirlingGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedSemifluidGeneratorMenu>> ADVANCED_SEMIFLUID_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("advanced_semifluid_generator_menu", () -> new MenuType<>(AdvancedSemifluidGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ExperienceGeneratorMenu>> EXPERIENCE_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("experience_generator_menu", () -> new MenuType<>(ExperienceGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedExperienceGeneratorMenu>> ADVANCED_EXPERIENCE_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("advanced_experience_generator_menu", () -> new MenuType<>(AdvancedExperienceGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== HUEntity =====
    public static final DeferredHolder<MenuType<?>, MenuType<HeatGeneratorElcMenu>> HEAT_GENERATOR_ELC_MENU_TYPE =
        MENU_TYPES.register("heat_generator_elc_menu", () -> new MenuType<>(HeatGeneratorElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SolidHeatGeneratorMenu>> SOLID_HEAT_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("solid_heat_generator_menu", () -> new MenuType<>(SolidHeatGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidHeatGeneratorMenu>> FLUID_HEAT_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("fluid_heat_generator_menu", () -> new MenuType<>(FluidHeatGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<HeatSourceFluidMenu>> HEAT_SOURCE_FLUID_MENU_TYPE =
        MENU_TYPES.register("heat_source_fluid_menu", () -> new MenuType<>(HeatSourceFluidMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<RTHeatGeneratorMenu>> RT_HEAT_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("rt_heat_generator_menu", () -> new MenuType<>(RTHeatGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== KUEntity =====
    public static final DeferredHolder<MenuType<?>, MenuType<KineticGeneratorElcMenu>> KINETIC_GENERATOR_ELC_MENU_TYPE =
        MENU_TYPES.register("kinetic_generator_elc_menu", () -> new MenuType<>(KineticGeneratorElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== Producer =====
    public static final DeferredHolder<MenuType<?>, MenuType<FurnaceElcMenu>> FURNACE_ELC_MENU_TYPE =
        MENU_TYPES.register("furnace_elc_menu", () -> new MenuType<>(FurnaceElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PowderElcMenu>> POWDER_ELC_MENU_TYPE =
        MENU_TYPES.register("powder_elc_menu", () -> new MenuType<>(PowderElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PowderAdvancedElcMenu>> POWDER_ADVANCED_ELC_MENU_TYPE =
        MENU_TYPES.register("powder_advanced_elc_menu", () -> new MenuType<>(PowderAdvancedElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WasherElcMenu>> WASHER_ELC_MENU_TYPE =
        MENU_TYPES.register("washer_elc_menu", () -> new MenuType<>(WasherElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FermenterElcMenu>> FERMENTER_ELC_MENU_TYPE =
        MENU_TYPES.register("fermenter_elc_menu", () -> new MenuType<>(FermenterElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<OilRefineryElcMenu>> OIL_REFINERY_ELC_MENU_TYPE =
        MENU_TYPES.register("oil_refinery_elc_menu", () -> new MenuType<>(OilRefineryElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU_TYPE =
        MENU_TYPES.register("blast_furnace_menu", () -> new MenuType<>(BlastFurnaceMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceElcMenu>> BLAST_FURNACE_ELC_MENU_TYPE =
        MENU_TYPES.register("blast_furnace_elc_menu", () -> new MenuType<>(BlastFurnaceElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceAdvancedMenu>> BLAST_FURNACE_ADVANCED_MENU_TYPE =
        MENU_TYPES.register("blast_furnace_advanced_menu", () -> new MenuType<>(BlastFurnaceAdvancedMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamGeneratorMenu>> STEAM_GENERATOR_MENU_TYPE =
        MENU_TYPES.register("steam_generator_menu", () -> new MenuType<>(SteamGeneratorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamRepressurizerMenu>> STEAM_REPRESSURIZER_MENU_TYPE =
        MENU_TYPES.register("steam_repressurizer_menu", () -> new MenuType<>(SteamRepressurizerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarDistillerMenu>> SOLAR_DISTILLER_MENU_TYPE =
        MENU_TYPES.register("solar_distiller_menu", () -> new MenuType<>(SolarDistillerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<CondenserMenu>> CONDENSER_MENU_TYPE =
        MENU_TYPES.register("condenser_menu", () -> new MenuType<>(CondenserMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<BlockCutterMenu>> BLOCK_CUTTER_MENU_TYPE =
        MENU_TYPES.register("block_cutter_menu", () -> new MenuType<>(BlockCutterMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<LatheMenu>> LATHE_MENU_TYPE =
        MENU_TYPES.register("lathe_menu", () -> new MenuType<>(LatheMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ExtractorElcMenu>> EXTRACTOR_ELC_MENU_TYPE =
        MENU_TYPES.register("extractor_elc_menu", () -> new MenuType<>(ExtractorElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<CompressorElcMenu>> COMPRESSOR_ELC_MENU_TYPE =
        MENU_TYPES.register("compressor_elc_menu", () -> new MenuType<>(CompressorElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<CompressorAdvancedElcMenu>> COMPRESSOR_ADVANCED_ELC_MENU_TYPE =
        MENU_TYPES.register("compressor_advanced_elc_menu", () -> new MenuType<>(CompressorAdvancedElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<CannerElcMenu>> CANNER_ELC_MENU_TYPE =
        MENU_TYPES.register("canner_elc_menu", () -> new MenuType<>(CannerElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MinerElcMenu>> MINER_ELC_MENU_TYPE =
        MENU_TYPES.register("miner_elc_menu", () -> new MenuType<>(MinerElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedMinerElcMenu>> ADVANCED_MINER_ELC_MENU_TYPE =
        MENU_TYPES.register("advanced_miner_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<AdvancedMinerElcMenu>) (containerId, playerInventory, data) -> new AdvancedMinerElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PumpElcMenu>> PUMP_ELC_MENU_TYPE =
        MENU_TYPES.register("pump_elc_menu", () -> new MenuType<>(PumpElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectrolyzerElcMenu>> ELECTROLYZER_MENU_TYPE =
        MENU_TYPES.register("electrolyzer_menu", () -> new MenuType<>(ElectrolyzerElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<RecyclerElcMenu>> RECYCLER_ELC_MENU_TYPE =
        MENU_TYPES.register("recycler_elc_menu", () -> new MenuType<>(RecyclerElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<InductionElcMenu>> INDUCTION_ELC_MENU_TYPE =
        MENU_TYPES.register("induction_elc_menu", () -> new MenuType<>(InductionElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterElcMenu>> MATTER_ELC_MENU_TYPE =
        MENU_TYPES.register("matter_elc_menu", () -> new MenuType<>(MatterElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<LargeFabricatorCoreMenu>> LARGE_FABRICATOR_CORE_MENU_TYPE =
        MENU_TYPES.register("large_fabricator_core_menu", () -> new MenuType<>(LargeFabricatorCoreMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<NeutronPolymerizerMenu>> NEUTRON_POLYMERIZER_MENU_TYPE =
        MENU_TYPES.register("neutron_polymerizer_menu", () -> new MenuType<>(NeutronPolymerizerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MetalFormerMenu>> METAL_FORMER_MENU_TYPE =
        MENU_TYPES.register("metal_former_menu", () -> new MenuType<>(MetalFormerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MetalFormerAdvancedMenu>> METAL_FORMER_ADVANCED_MENU_TYPE =
        MENU_TYPES.register("metal_former_advanced_menu", () -> new MenuType<>(MetalFormerAdvancedMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeElcMenu>> CENTRIFUGE_ELC_MENU_TYPE =
        MENU_TYPES.register("centrifuge_elc_menu", () -> new MenuType<>(CentrifugeElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FutureElcMenu>> FUTURE_ELC_MENU_TYPE =
        MENU_TYPES.register("future_elc_menu", () -> new MenuType<>(FutureElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneReactorCoolantInjectorMenu>> REDSTONE_REACTOR_COOLANT_INJECTOR_MENU_TYPE =
        MENU_TYPES.register("redstone_reactor_coolant_injector_menu", () -> new MenuType<>(RedstoneReactorCoolantInjectorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<LapisReactorCoolantInjectorMenu>> LAPIS_REACTOR_COOLANT_INJECTOR_MENU_TYPE =
        MENU_TYPES.register("lapis_reactor_coolant_injector_menu", () -> new MenuType<>(LapisReactorCoolantInjectorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MagnetizerMenu>> MAGNETIZER_MENU_TYPE =
        MENU_TYPES.register("magnetizer_menu", () -> new MenuType<>(MagnetizerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MatronElcMenu>> MATRON_ELC_MENU_TYPE =
        MENU_TYPES.register("matron_elc_menu", () -> new MenuType<>(MatronElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<HarvestElcMenu>> HARVEST_ELC_MENU_TYPE =
        MENU_TYPES.register("harvest_elc_menu", () -> new MenuType<>(HarvestElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ScannerElcMenu>> SCANNER_ELC_MENU_TYPE =
        MENU_TYPES.register("scanner_elc_menu", () -> new MenuType<>(ScannerElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MolecularTransformerMenu>> MOLECULAR_TRANSFORMER_MENU_TYPE =
        MENU_TYPES.register("molecular_transformer_menu", () -> new MenuType<>(MolecularTransformerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PatternStorageMenu>> PATTERN_STORAGE_MENU_TYPE =
        MENU_TYPES.register("pattern_storage_menu", () -> new MenuType<>(PatternStorageMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ReplicatorElcMenu>> REPLICATOR_ELC_MENU_TYPE =
        MENU_TYPES.register("replicator_elc_menu", () -> new MenuType<>(ReplicatorElcMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemBufferMenu>> ITEM_BUFFER_MENU_TYPE =
        MENU_TYPES.register("item_buffer_menu", () -> new MenuType<>(ItemBufferMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SorterElcMenu>> SORTER_ELC_MENU_TYPE =
        MENU_TYPES.register("sorter_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<SorterElcMenu>) (containerId, playerInventory, data) -> new SorterElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemDistributorElcMenu>> ITEM_DISTRIBUTOR_ELC_MENU_TYPE =
        MENU_TYPES.register("item_distributor_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<ItemDistributorElcMenu>) (containerId, playerInventory, data) -> new ItemDistributorElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidDistributorElcMenu>> FLUID_DISTRIBUTOR_ELC_MENU_TYPE =
        MENU_TYPES.register("fluid_distributor_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<FluidDistributorElcMenu>) (containerId, playerInventory, data) -> new FluidDistributorElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WeightedFluidDistributorElcMenu>> WEIGHTED_FLUID_DISTRIBUTOR_ELC_MENU_TYPE =
        MENU_TYPES.register("weighted_fluid_distributor_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<WeightedFluidDistributorElcMenu>) (containerId, playerInventory, data) -> new WeightedFluidDistributorElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidRegulatorElcMenu>> FLUID_REGULATOR_ELC_MENU_TYPE =
        MENU_TYPES.register("fluid_regulator_elc_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<FluidRegulatorElcMenu>) (containerId, playerInventory, data) -> new FluidRegulatorElcMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<BatchCrafterMenu>> BATCH_CRAFTER_MENU_TYPE =
        MENU_TYPES.register("batch_crafter_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<BatchCrafterMenu>) (containerId, playerInventory, data) -> new BatchCrafterMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ChunkLoaderMenu>> CHUNK_LOADER_MENU_TYPE =
        MENU_TYPES.register("chunk_loader_menu", () -> new MenuType<>((net.neoforged.neoforge.network.IContainerFactory<ChunkLoaderMenu>) (containerId, playerInventory, data) -> new ChunkLoaderMenu(containerId, playerInventory, data), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // 工业工作台菜�?
    public static final DeferredHolder<MenuType<?>, MenuType<IndustrialWorkbenchMenu>> INDUSTRIAL_WORKBENCH_MENU_TYPE =
        MENU_TYPES.register("industrial_workbench_menu", () -> new MenuType<>(IndustrialWorkbenchMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== Storage =====
    public static final DeferredHolder<MenuType<?>, MenuType<BatBoxMenu>> BAT_BOX_MENU_TYPE =
        MENU_TYPES.register("bat_box_menu", () -> new MenuType<>(BatBoxMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<TransformerMenu>> TRANSFORMER_MENU_TYPE =
        MENU_TYPES.register("transformer_menu", () -> new MenuType<>(TransformerMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU_27 =
        MENU_TYPES.register("storage_box_menu_27", () -> new MenuType<>((id, inv) -> new StorageBoxMenu(id, inv, 27), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU_45 =
        MENU_TYPES.register("storage_box_menu_45", () -> new MenuType<>((id, inv) -> new StorageBoxMenu(id, inv, 45), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU_63 =
        MENU_TYPES.register("storage_box_menu_63", () -> new MenuType<>((id, inv) -> new StorageBoxMenu(id, inv, 63), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU_84 =
        MENU_TYPES.register("storage_box_menu_84", () -> new MenuType<>((id, inv) -> new StorageBoxMenu(id, inv, 84), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU_126 =
        MENU_TYPES.register("storage_box_menu_126", () -> new MenuType<>((id, inv) -> new StorageBoxMenu(id, inv, 126), net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== Barrel =====
    public static final DeferredHolder<MenuType<?>, MenuType<BarrelMenu>> BARREL_MENU_TYPE =
        MENU_TYPES.register("barrel_menu", () -> new MenuType<>(BarrelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== GESU =====
    public static final DeferredHolder<MenuType<?>, MenuType<GESUCoreMenu>> GESU_CORE_MENU_TYPE =
        MENU_TYPES.register("gesu_core_menu", () -> new MenuType<>(GESUCoreMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ===== OilRig =====
    public static final DeferredHolder<MenuType<?>, MenuType<OilRigPanelMenu>> OIL_RIG_PANEL_MENU_TYPE =
        MENU_TYPES.register("oil_rig_panel_menu", () -> new MenuType<>(OilRigPanelMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}