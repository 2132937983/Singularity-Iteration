package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_generator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_solid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_fluid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Kinetic_Generator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Manual_KineticU_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Water_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_geo_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_twin_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_rt_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_drop_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_drop_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_semifluid_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_geomagnetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_diesel_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_experience_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_experience_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_lTom;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_mToh;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_hToe;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_eTos;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_iv;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_luv;
import com.singularity_iteration.mio_icif.Blocks.entity.energy_converter.mio_icif_energy_converter_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_batbox_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_batbox_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfsu_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfe_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_cesu_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_lesu_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_eesu_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_eesu_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_gesu_core_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_gesu_input_iv_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_gesu_output_iv_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_gesu_output_luv_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_lesu_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_cesu_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfe_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfsu_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wireless_power_transmission_node;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_Semifluid_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_solar_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_AdvancedSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_HybridSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_UltimateHybridSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_QuantumSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_MetsAdvancedSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_PhotonResonanceSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_UltimatePhotonResonanceSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_QuantumGenerator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_wind_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_water_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_item_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_default;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fermenter_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_oil_refinery_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_barrel_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace_advanced;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_repressurizer;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_solar_distiller;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_condenser;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_block_cutter;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_lathe;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_steam_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_powder_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_powder_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_washer_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_future_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_extrator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_compressor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_compressor_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former_advanced;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_canner_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_electrolyzer_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_redstone_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_replicator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_lapis_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_terra_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_scanner_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_molecular_transformer;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pattern_storage;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_buffer_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_sorter_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_weighted_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_regulator_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@SuppressWarnings("null")
public class mio_icif_block_entities {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 注册 batbox entity 方块实体类型
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Singularity_Iteration.MOD_ID);
    

    // Energy Block 是抽象基类，不能直接注册为方块实体类型
    // 子类（如 batbox、generator 等）应各自注册自己的 BlockEntityType
    
    // 注册 BatBox 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_batbox_entity>> BATBOX =
        BLOCK_ENTITIES.register("batbox", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_batbox_entity(pos, state), mio_icif_blocks.BAT_BOX.get()).build(null));
    
    // 注册 BatBox 充电仓 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_batbox_charger_entity>> BATBOX_CHARGER =
        BLOCK_ENTITIES.register("batbox_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_batbox_charger_entity(pos, state), mio_icif_blocks.BATBOX_CHARGER.get()).build(null));
    
    // 注册 CESU 充电仓 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_cesu_charger_entity>> CESU_CHARGER =
        BLOCK_ENTITIES.register("cesu_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_cesu_charger_entity(pos, state), mio_icif_blocks.CESU_CHARGER.get()).build(null));
    
    // 注册 MFE 充电仓 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_mfe_charger_entity>> MFE_CHARGER =
        BLOCK_ENTITIES.register("mfe_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_mfe_charger_entity(pos, state), mio_icif_blocks.MFE_CHARGER.get()).build(null));
    
    // 注册 MFSU 充电仓 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_mfsu_charger_entity>> MFSU_CHARGER =
        BLOCK_ENTITIES.register("mfsu_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_mfsu_charger_entity(pos, state), mio_icif_blocks.MFSU_CHARGER.get()).build(null));
    
    // 注册 LV 导线 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_LV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_lv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.LV), mio_icif_blocks.WIRE_LV.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.LV, type);
            return type;
        });

    // 注册 MV 导线 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_MV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_mv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.MV), mio_icif_blocks.WIRE_MV.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.MV, type);
            return type;
        });

    // 注册 HV 导线 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_HV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_hv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.HV), mio_icif_blocks.WIRE_HV.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.HV, type);
            return type;
        });

    // 注册 EV 导线 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_EV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_ev", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.EV), mio_icif_blocks.WIRE_EV.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.EV, type);
            return type;
        });

    // 注册 IV 导线 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_IV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_iv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.IV), mio_icif_blocks.WIRE_IV.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.IV, type);
            return type;
        });

    // 注册LuV 电线方块实体类型（超导电缆，使用 mio_icif_wire 实体类）
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_LuV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_luv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.LuV, true), mio_icif_blocks.SUPERCONDUCTING_CABLE.get()).build(null);
            mio_icif_wire.registerWireType(CableTier.LuV, type);
            return type;
        });

    // 注册 wire isolation lv 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_ISOLATION_LV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_isolation_lv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.LV, true), mio_icif_blocks.WIRE_ISOLATION_LV.get()).build(null);
            mio_icif_wire.registerIsolationWireType(CableTier.LV, type);
            return type;
        });

    // 注册 wire isolation mv 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_ISOLATION_MV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_isolation_mv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.MV, true), mio_icif_blocks.WIRE_ISOLATION_MV.get()).build(null);
            mio_icif_wire.registerIsolationWireType(CableTier.MV, type);
            return type;
        });

    // 注册 wire isolation hv 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_ISOLATION_HV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_isolation_hv", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.HV, true), mio_icif_blocks.WIRE_ISOLATION_HV.get()).build(null);
            mio_icif_wire.registerIsolationWireType(CableTier.HV, type);
            return type;
        });

    // 注册 wire isolation ev 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_ISOLATION_EV_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wire_isolation_ev", () -> {
            BlockEntityType<mio_icif_wire> type = BlockEntityType.Builder.of(
                (pos, state) -> new mio_icif_wire(pos, state, CableTier.EV, true), mio_icif_blocks.WIRE_ISOLATION_EV.get()).build(null);
            mio_icif_wire.registerIsolationWireType(CableTier.EV, type);
            return type;
        });

    // 废弃线缆类型
    @Deprecated
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wire>> WIRE_ENTITY_TYPE = WIRE_LV_ENTITY_TYPE;
    
    // 注册 thermal generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Thermal_Generator>> THERMAL_GENERATOR_ENTITY_TYPE = 
        BLOCK_ENTITIES.register("thermal_generator", () -> {
            LOGGER.info("Registering THERMAL_GENERATOR_ENTITY_TYPE");
            BlockEntityType<mio_icif_Thermal_Generator> type = BlockEntityType.Builder.of((pos, state) -> new mio_icif_Thermal_Generator(pos, state), mio_icif_blocks.THERMAL_GENERATOR.get()).build(null);
            LOGGER.info("THERMAL_GENERATOR_ENTITY_TYPE registered: " + type);
            return type;
        });
    
    // 注册火力发电机 MenuType
    
    // 注册 BatBox MenuType

    // 注册 mfsu 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_mfsu_entity>> MFSU = 
        BLOCK_ENTITIES.register("mfsu", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_mfsu_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.MFSU.get()).build(null));

    // 注册 mfe 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_mfe_entity>> MFE = 
        BLOCK_ENTITIES.register("mfe", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_mfe_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.MFE.get()).build(null));

    // 注册 cesu 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_cesu_entity>> CESU = 
        BLOCK_ENTITIES.register("cesu", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_cesu_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.CESU.get()).build(null));

    // 注册 LESU 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_lesu_entity>> LESU = 
        BLOCK_ENTITIES.register("lesu", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_lesu_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.LESU.get()).build(null));

    // 注册 LESU 充电仓 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_lesu_charger_entity>> LESU_CHARGER =
        BLOCK_ENTITIES.register("lesu_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_lesu_charger_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.LESU_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_eesu_entity>> EESU = 
        BLOCK_ENTITIES.register("eesu", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_eesu_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.EESU.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_eesu_charger_entity>> EESU_CHARGER =
        BLOCK_ENTITIES.register("eesu_charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_eesu_charger_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.EESU_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_gesu_core_entity>> GESU_CORE = 
        BLOCK_ENTITIES.register("gesu_core", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_gesu_core_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.GESU_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_gesu_input_iv_entity>> GESU_INPUT_IV = 
        BLOCK_ENTITIES.register("gesu_input_iv", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_gesu_input_iv_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.GESU_INPUT_IV.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_gesu_output_iv_entity>> GESU_OUTPUT_IV = 
        BLOCK_ENTITIES.register("gesu_output_iv", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_gesu_output_iv_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.GESU_OUTPUT_IV.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_gesu_output_luv_entity>> GESU_OUTPUT_LUV = 
        BLOCK_ENTITIES.register("gesu_output_luv", () -> 
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_gesu_output_luv_entity(pos, state), com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.GESU_OUTPUT_LUV.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wireless_power_transmission_node>> WIRELESS_POWER_TRANSMISSION_NODE =
        BLOCK_ENTITIES.register("wireless_power_transmission_node", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_wireless_power_transmission_node(pos, state), mio_icif_blocks.WIRELESS_POWER_TRANSMISSION_NODE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wire_detector>> WIRE_DETECTOR =
        BLOCK_ENTITIES.register("wire_detector", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wire_detector::new, mio_icif_blocks.WIRE_DETECTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wire_splitter>> WIRE_SPLITTER =
        BLOCK_ENTITIES.register("wire_splitter", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wire_splitter::new, mio_icif_blocks.WIRE_SPLITTER.get()).build(null));

 // 注册 电 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_furnace_elc>> FURNACE_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("furnace_elc", () ->
            BlockEntityType.Builder.of(mio_icif_furnace_elc::new, mio_icif_blocks.FURNACE_ELC.get()).build(null));

    // 注册 打粉机 方块实体类型

    // 注册 打粉机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_powder_elc>> POWDER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("powder_elc", () ->
            BlockEntityType.Builder.of(mio_icif_powder_elc::new, mio_icif_blocks.POWDER_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_powder_advanced_elc>> POWDER_ADVANCED_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("powder_advanced_elc", () ->
            BlockEntityType.Builder.of(mio_icif_powder_advanced_elc::new, mio_icif_blocks.POWDER_ADVANCED_ELC.get()).build(null));

    // 注册打粉机 MenuType

    // 注册 洗衣机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_washer_elc>> WASHER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("washer_elc", () ->
            BlockEntityType.Builder.of(mio_icif_washer_elc::new, mio_icif_blocks.WASHER_ELC.get()).build(null));

    // 注册洗衣机 MenuType

    // 注册 发酵机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_fermenter_elc>> FERMENTER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("fermenter_elc", () ->
            BlockEntityType.Builder.of(mio_icif_fermenter_elc::new, mio_icif_blocks.FERMENTER_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_oil_refinery_elc>> OIL_REFINERY_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("oil_refinery_elc", () ->
            BlockEntityType.Builder.of(mio_icif_oil_refinery_elc::new, mio_icif_blocks.OIL_REFINERY_ELC.get()).build(null));

 // 注册 高 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_blast_furnace>> BLAST_FURNACE_ENTITY_TYPE =
        BLOCK_ENTITIES.register("blast_furnace", () ->
            BlockEntityType.Builder.of(mio_icif_blast_furnace::new, mio_icif_blocks.BLAST_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_blast_furnace_elc>> BLAST_FURNACE_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("blast_furnace_elc", () ->
            BlockEntityType.Builder.of(mio_icif_blast_furnace_elc::new, mio_icif_blocks.BLAST_FURNACE_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_blast_furnace_advanced>> BLAST_FURNACE_ADVANCED_ENTITY_TYPE =
        BLOCK_ENTITIES.register("blast_furnace_advanced", () ->
            BlockEntityType.Builder.of(mio_icif_blast_furnace_advanced::new, mio_icif_blocks.BLAST_FURNACE_ADVANCED.get()).build(null));

    // 注册高炉 MenuType

    // 注册 蒸汽发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_steam_generator>> STEAM_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("steam_generator", () ->
            BlockEntityType.Builder.of(mio_icif_steam_generator::new, mio_icif_blocks.STEAM_GENERATOR.get()).build(null));

    // 注册蒸汽压缩机 MenuType

    // 注册 蒸汽压缩机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_steam_repressurizer>> STEAM_REPRESSURIZER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("steam_repressurizer", () ->
            BlockEntityType.Builder.of(mio_icif_steam_repressurizer::new, mio_icif_blocks.STEAM_REPRESSURIZER.get()).build(null));

    // 注册 太阳能蒸馏器 MenuType

    // 注册 太阳能蒸馏器 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_solar_distiller>> SOLAR_DISTILLER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("solar_distiller", () ->
            BlockEntityType.Builder.of(mio_icif_solar_distiller::new, mio_icif_blocks.SOLAR_DISTILLER.get()).build(null));

    // 注册 condenser MenuType

    // 注册 condenser MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_condenser>> CONDENSER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("condenser", () ->
            BlockEntityType.Builder.of(mio_icif_condenser::new, mio_icif_blocks.CONDENSER.get()).build(null));

    // 注册 电动切割机 MenuType

    // 注册 电动切割机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_block_cutter>> BLOCK_CUTTER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("block_cutter", () ->
            BlockEntityType.Builder.of(mio_icif_block_cutter::new, mio_icif_blocks.BLOCK_CUTTER.get()).build(null));

    // 娉ㄥ唽鏂瑰潡鍒囧壊鏈?MenuType

    // 注册 车床 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_lathe>> LATHE_ENTITY_TYPE =
        BLOCK_ENTITIES.register("lathe", () ->
            BlockEntityType.Builder.of(mio_icif_lathe::new, mio_icif_blocks.LATHE.get()).build(null));

    // 注册车床 MenuType

    // 注册 蒸汽动能发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_steam_kinetic_generator>> STEAM_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("steam_kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_steam_kinetic_generator::new, mio_icif_blocks.STEAM_KINETIC_GENERATOR.get()).build(null));

    // 注册蒸汽动能发电机 MenuType

    // 注册 提取机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_extrator_elc>> EXTRACTOR_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("extractor_elc", () ->
            BlockEntityType.Builder.of(mio_icif_extrator_elc::new, mio_icif_blocks.EXTRACTOR_ELC.get()).build(null));

    // 注册提取机 MenuType

    // 注册 pipe water 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_pipe_default>> PIPE_WATER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pipe_water", () ->
            BlockEntityType.Builder.of(mio_icif_block_entities::createLegacyPipeEntity,
                mio_icif_blocks.PIPE_WATER.get(),
                mio_icif_blocks.PIPE_WATER_EXTRACT.get(),
                mio_icif_blocks.PIPE_ITEM_INPUT.get(),
                mio_icif_blocks.PIPE_ITEM_TRANSPORT.get()).build(null));

    // 注册 pipe water extract 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_pipe_fluid_extract>> PIPE_WATER_EXTRACT_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pipe_water_extract", () ->
            BlockEntityType.Builder.of(mio_icif_pipe_fluid_extract::new, mio_icif_blocks.PIPE_WATER_EXTRACT.get()).build(null));

    // 注册 pipe item input 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_pipe_item_extract>> PIPE_ITEM_INPUT_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pipe_item_input", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_pipe_item_extract(pos, state), mio_icif_blocks.PIPE_ITEM_INPUT.get()).build(null));

    // 注册 pipe item transport 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_pipe_item>> PIPE_ITEM_TRANSPORT_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pipe_item_transport", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_pipe_item(null, pos, state), mio_icif_blocks.PIPE_ITEM_TRANSPORT.get()).build(null));

    /**
     * Migrates pipe block entities saved by versions that assigned every pipe
     * the pipe_water id when their constructor did not receive a type.
     */
    private static mio_icif_pipe_default createLegacyPipeEntity(BlockPos pos, BlockState state) {
        if (state.is(mio_icif_blocks.PIPE_WATER_EXTRACT.get())) {
            return new mio_icif_pipe_fluid_extract(PIPE_WATER_EXTRACT_ENTITY_TYPE.get(), pos, state);
        }
        if (state.is(mio_icif_blocks.PIPE_ITEM_INPUT.get())) {
            return new mio_icif_pipe_item_extract(PIPE_ITEM_INPUT_ENTITY_TYPE.get(), pos, state);
        }
        if (state.is(mio_icif_blocks.PIPE_ITEM_TRANSPORT.get())) {
            return new mio_icif_pipe_item(PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get(), pos, state);
        }
        return new mio_icif_pipe_fluid(PIPE_WATER_ENTITY_TYPE.get(), pos, state);
    }

    // 注册 地热发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_geo_generator>> GEO_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("geo_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_geo_generator(pos, state), mio_icif_blocks.GEO_GENERATOR.get()).build(null));

    // 娉ㄥ唽鍦扮儹鍙戠數鏈篗enuType

    // 注册 太阳能发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_solar_generator>> SOLAR_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("solar_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_solar_generator(pos, state), mio_icif_blocks.SOLAR_GENERATOR.get()).build(null));

    // 注册 advanced solar panel 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_AdvancedSolarPanel>> ADVANCED_SOLAR_PANEL_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_solar_panel", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_AdvancedSolarPanel(pos, state), mio_icif_blocks.ADVANCED_SOLAR_PANEL.get()).build(null));

    // 注册 hybrid solar panel 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_HybridSolarPanel>> HYBRID_SOLAR_PANEL_ENTITY_TYPE =
        BLOCK_ENTITIES.register("hybrid_solar_panel", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_HybridSolarPanel(pos, state), mio_icif_blocks.HYBRID_SOLAR_PANEL.get()).build(null));

    // 注册 ultimate hybrid solar panel 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_UltimateHybridSolarPanel>> ULTIMATE_HYBRID_SOLAR_PANEL_ENTITY_TYPE =
        BLOCK_ENTITIES.register("ultimate_hybrid_solar_panel", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_UltimateHybridSolarPanel(pos, state), mio_icif_blocks.ULTIMATE_HYBRID_SOLAR_PANEL.get()).build(null));

    // 注册 quantum solar panel 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_QuantumSolarPanel>> QUANTUM_SOLAR_PANEL_ENTITY_TYPE =
        BLOCK_ENTITIES.register("quantum_solar_panel", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_QuantumSolarPanel(pos, state), mio_icif_blocks.QUANTUM_SOLAR_PANEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_MetsAdvancedSolarGenerator>> METS_ADVANCED_SOLAR_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("mets_advanced_solar_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_MetsAdvancedSolarGenerator(pos, state), mio_icif_blocks.METS_ADVANCED_SOLAR_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_PhotonResonanceSolarGenerator>> PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("photon_resonance_solar_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_PhotonResonanceSolarGenerator(pos, state), mio_icif_blocks.PHOTON_RESONANCE_SOLAR_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_UltimatePhotonResonanceSolarGenerator>> ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("ultimate_photon_resonance_solar_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_UltimatePhotonResonanceSolarGenerator(pos, state), mio_icif_blocks.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR.get()).build(null));

    // 注册 quantum generator MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_QuantumGenerator>> QUANTUM_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("quantum_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_QuantumGenerator(pos, state), mio_icif_blocks.QUANTUM_GENERATOR.get()).build(null));

    // 注册 风力发电机 方块实体类型

    // 注册 风力发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_wind_generator>> WIND_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wind_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_wind_generator(pos, state), mio_icif_blocks.WIND_GENERATOR.get()).build(null));

    // 注册风力发电机 MenuType

    // 注册 水力发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_water_generator>> WATER_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("water_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_water_generator(pos, state), mio_icif_blocks.WATER_GENERATOR.get()).build(null));

    // 注册水力发电机 MenuType

    // 注册 半流体发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Semifluid_generator>> SEMIFLUID_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("semifluid_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_Semifluid_generator(pos, state), mio_icif_blocks.SEMIFLUID_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_geomagnetic_generator>> GEOMAGNETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("geomagnetic_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_geomagnetic_generator(pos, state), mio_icif_blocks.GEOMAGNETIC_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_diesel_generator>> DIESEL_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("diesel_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_diesel_generator(pos, state), mio_icif_blocks.DIESEL_GENERATOR.get()).build(null));

    // 注册 电力发热机 方块实体类型

    // 注册 电力发热机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_heat_generator_elc>> HEAT_GENERATOR_ELC =
        BLOCK_ENTITIES.register("heat_generator_elc", () ->
            BlockEntityType.Builder.of(mio_icif_heat_generator_elc::new, mio_icif_blocks.HEAT_GENERATOR_ELC.get()).build(null));

    // 注册 固体发热机 方块实体类型

    // 注册 固体发热机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_solid_heat_generator>> SOLID_HEAT_GENERATOR =
        BLOCK_ENTITIES.register("solid_heat_generator", () ->
            BlockEntityType.Builder.of(mio_icif_solid_heat_generator::new, mio_icif_blocks.SOLID_HEAT_GENERATOR.get()).build(null));

    // 娉ㄥ唽鍥轰綋鍔犵儹鏈篗enuType

    // 注册 fluid heat generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_fluid_heat_generator>> FLUID_HEAT_GENERATOR =
        BLOCK_ENTITIES.register("fluid_heat_generator", () ->
            BlockEntityType.Builder.of(mio_icif_fluid_heat_generator::new, mio_icif_blocks.FLUID_HEAT_GENERATOR.get()).build(null));

    // 娉ㄥ唽娴佷綋鍔犵儹鏈篗enuType

    // 注册 stirling generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_stirling_generator>> STIRLING_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("stirling_generator", () ->
            BlockEntityType.Builder.of(mio_icif_stirling_generator::new, mio_icif_blocks.STIRLING_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_drop_generator>> DROP_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("drop_generator", () ->
            BlockEntityType.Builder.of(mio_icif_drop_generator::new, mio_icif_blocks.DROP_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_advanced_drop_generator>> ADVANCED_DROP_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_drop_generator", () ->
            BlockEntityType.Builder.of(mio_icif_advanced_drop_generator::new, mio_icif_blocks.ADVANCED_DROP_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_advanced_stirling_generator>> ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_stirling_generator", () ->
            BlockEntityType.Builder.of(mio_icif_advanced_stirling_generator::new, mio_icif_blocks.ADVANCED_STIRLING_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_advanced_semifluid_generator>> ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_semifluid_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_advanced_semifluid_generator(pos, state), mio_icif_blocks.ADVANCED_SEMIFLUID_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_experience_generator>> EXPERIENCE_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("experience_generator", () ->
            BlockEntityType.Builder.of(mio_icif_experience_generator::new, mio_icif_blocks.EXPERIENCE_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_advanced_experience_generator>> ADVANCED_EXPERIENCE_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_experience_generator", () ->
            BlockEntityType.Builder.of(mio_icif_advanced_experience_generator::new, mio_icif_blocks.ADVANCED_EXPERIENCE_GENERATOR.get()).build(null));

    // 注册 手动动能发电机 方块实体类型

    // 注册 手动动能发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Manual_KineticU_Generator>> MANUAL_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("manual_kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_Manual_KineticU_Generator::new, mio_icif_blocks.MANUAL_KINETIC_GENERATOR.get()).build(null));

    // 注册 kinetic generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_kinetic_generator>> KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_kinetic_generator::new, mio_icif_blocks.KINETIC_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_turbo_kinetic_generator>> TURBO_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("turbo_kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_turbo_kinetic_generator::new, mio_icif_blocks.TURBO_KINETIC_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_twin_turbo_kinetic_generator>> TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("twin_turbo_kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_twin_turbo_kinetic_generator::new, mio_icif_blocks.TWIN_TURBO_KINETIC_GENERATOR.get()).build(null));

    // 注册动能发电机 MenuType

    // 注册 风力动能发电机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Wind_Kinetic_Generator>> WIND_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("wind_kinetic_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_Wind_Kinetic_Generator(pos, state, null), mio_icif_blocks.WIND_KINETIC_GENERATOR.get()).build(null));

    // 注册风力动能发电机 MenuType

    // 注册 水力动能发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Water_Kinetic_Generator>> WATER_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("water_kinetic_generator", () ->
            BlockEntityType.Builder.of((pos, state) -> new mio_icif_Water_Kinetic_Generator(pos, state, null), mio_icif_blocks.WATER_KINETIC_GENERATOR.get()).build(null));

    // 注册水力动能发电机 MenuType

    // 注册 电力动能发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Kinetic_Generator_elc>> KINETIC_GENERATOR_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("kinetic_generator_elc", () ->
            BlockEntityType.Builder.of(mio_icif_Kinetic_Generator_elc::new, mio_icif_blocks.KINETIC_GENERATOR_ELC.get()).build(null));

    // 注册 斯特林动能发电机 MenuType

    // 注册 斯特林动能发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_Stirling_Kinetic_Generator>> STIRLING_KINETIC_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("stirling_kinetic_generator", () ->
            BlockEntityType.Builder.of(mio_icif_Stirling_Kinetic_Generator::new, mio_icif_blocks.STIRLING_KINETIC_GENERATOR.get()).build(null));

    // 注册 LV-MV 变压器 方块实体类型

    // 注册 LV-MV 变压器 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_lTom>> TRANSFORMER_LV_MV =
        BLOCK_ENTITIES.register("transformer_lv_mv", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_lTom::new, mio_icif_blocks.TRANSFORMER_LV_MV.get()).build(null));

    // 注册 MV-HV 变压器 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_mToh>> TRANSFORMER_MV_HV =
        BLOCK_ENTITIES.register("transformer_mv_hv", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_mToh::new, mio_icif_blocks.TRANSFORMER_MV_HV.get()).build(null));

    // 注册 HV-EV 变压器 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_hToe>> TRANSFORMER_HV_EV =
        BLOCK_ENTITIES.register("transformer_hv_ev", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_hToe::new, mio_icif_blocks.TRANSFORMER_HV_EV.get()).build(null));

    // 注册 EV-SC 变压器 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_eTos>> TRANSFORMER_EV_SC =
        BLOCK_ENTITIES.register("transformer_ev_sc", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_eTos::new, mio_icif_blocks.TRANSFORMER_EV_SC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_iv>> TRANSFORMER_IV_LUV =
        BLOCK_ENTITIES.register("transformer_iv_luv", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_iv::new, mio_icif_blocks.TRANSFORMER_IV_LUV.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_transformer_luv>> TRANSFORMER_LUV_ZPMV =
        BLOCK_ENTITIES.register("transformer_luv_zpmv", () ->
            BlockEntityType.Builder.of(mio_icif_transformer_luv::new, mio_icif_blocks.TRANSFORMER_LUV_ZPMV.get()).build(null));

    // 注册变压器 MenuType，所有变压器共用同一GUI

    // 注册 检测器 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.checker.mio_icif_checker>> CHECKER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("checker", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.checker.mio_icif_checker::new, mio_icif_blocks.CHECKER.get()).build(null));

    // 注册 未来交易机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_future_elc>> FUTURE_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("future_elc", () ->
            BlockEntityType.Builder.of(mio_icif_future_elc::new, mio_icif_blocks.FUTURE_ELC.get()).build(null));

    // 注册热源流体 MenuType

    // 注册 heat source fluid MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_heat_source_fluid>> HEAT_SOURCE_FLUID =
        BLOCK_ENTITIES.register("heat_source_fluid", () ->
            BlockEntityType.Builder.of(mio_icif_heat_source_fluid::new, mio_icif_blocks.HEAT_SOURCE_FLUID.get()).build(null));

    // 注册 压缩机 方块实体类型

    // 注册 压缩机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_compressor_elc>> COMPRESSOR_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("compressor_elc", () ->
            BlockEntityType.Builder.of(mio_icif_compressor_elc::new, mio_icif_blocks.COMPRESSOR_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_compressor_advanced_elc>> COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("compressor_advanced_elc", () ->
            BlockEntityType.Builder.of(mio_icif_compressor_advanced_elc::new, mio_icif_blocks.COMPRESSOR_ADVANCED_ELC.get()).build(null));

    // 注册压缩机 MenuType

    // 注册 封装机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_canner_elc>> CANNER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("canner_elc", () ->
            BlockEntityType.Builder.of(mio_icif_canner_elc::new, mio_icif_blocks.CANNER_ELC.get()).build(null));

    // 注册封装机 MenuType

    // 注册 采矿机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_miner_elc>> MINER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("miner_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_miner_elc::new, mio_icif_blocks.MINER_ELC.get()).build(null));

    // 注册采矿机 MenuType

    // 注册 高级采矿机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_advanced_miner_elc>> ADVANCED_MINER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("advanced_miner_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_advanced_miner_elc::new, mio_icif_blocks.ADVANCED_MINER_ELC.get()).build(null));

    // 注册高级采矿机 MenuType

    // 注册 pump elc MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pump_elc>> PUMP_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pump_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pump_elc::new, mio_icif_blocks.PUMP_ELC.get()).build(null));

    // 注册电解机 MenuType

    // 注册 电解机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_electrolyzer_elc>> ELECTROLYZER =
        BLOCK_ENTITIES.register("electrolyzer", () ->
            BlockEntityType.Builder.of(mio_icif_electrolyzer_elc::new, mio_icif_blocks.ELECTROLYZER.get()).build(null));

    // 注册 回收机 MenuType

    // 注册 回收机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_recycler_elc>> RECYCLER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("recycler_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_recycler_elc::new, mio_icif_blocks.RECYCLER_ELC.get()).build(null));

    // 注册回收机 MenuType

 // 注册 感应 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_induction_elc>> INDUCTION_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("induction_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_induction_elc::new, mio_icif_blocks.INDUCTION_ELC.get()).build(null));

    // 注册感应炉 MenuType

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matter_elc>> MATTER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("matter_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matter_elc::new, mio_icif_blocks.MATTER_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_core_entity>> LARGE_FABRICATOR_CORE =
        BLOCK_ENTITIES.register("large_fabricator_core", () ->
            BlockEntityType.Builder.of((pos, state) -> new com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_core_entity(pos, state), mio_icif_blocks.LARGE_FABRICATOR_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_input_iv_entity>> LARGE_FABRICATOR_INPUT_IV =
        BLOCK_ENTITIES.register("large_fabricator_input_iv", () ->
            BlockEntityType.Builder.of((pos, state) -> new com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_input_iv_entity(pos, state), mio_icif_blocks.LARGE_FABRICATOR_INPUT_IV.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_tank_entity>> LARGE_FABRICATOR_TANK =
        BLOCK_ENTITIES.register("large_fabricator_tank", () ->
            BlockEntityType.Builder.of((pos, state) -> new com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_tank_entity(pos, state), mio_icif_blocks.LARGE_FABRICATOR_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_scrap_entity>> LARGE_FABRICATOR_SCRAP =
        BLOCK_ENTITIES.register("large_fabricator_scrap", () ->
            BlockEntityType.Builder.of((pos, state) -> new com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_scrap_entity(pos, state), mio_icif_blocks.LARGE_FABRICATOR_SCRAP.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_neutron_polymerizer>> NEUTRON_POLYMERIZER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("neutron_polymerizer", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_neutron_polymerizer::new, mio_icif_blocks.NEUTRON_POLYMERIZER.get()).build(null));

    // 注册 unlimit generator 方块实体类型

    // 注册 unlimit generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_unlimit_generator>> UNLIMIT_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("unlimit_generator", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_unlimit_generator::new, mio_icif_blocks.UNLIMIT_GENERATOR.get()).build(null));

    // 注册 金属成型机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_metal_former>> METAL_FORMER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("metal_former", () ->
            BlockEntityType.Builder.of(mio_icif_metal_former::new, mio_icif_blocks.METAL_FORMER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_metal_former_advanced>> METAL_FORMER_ADVANCED_ENTITY_TYPE =
        BLOCK_ENTITIES.register("metal_former_advanced", () ->
            BlockEntityType.Builder.of(mio_icif_metal_former_advanced::new, mio_icif_blocks.METAL_FORMER_ADVANCED.get()).build(null));

    // 注册 离心机 MenuType

    // 注册 离心机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_centrifuge_elc>> CENTRIFUGE_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("centrifuge_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_centrifuge_elc::new, mio_icif_blocks.CENTRIFUGE_ELC.get()).build(null));

    // 注册 nuclear reactor generator 方块实体类型

    // 注册 nuclear reactor generator 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_nuclear_reactor_generator>> NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("nuclear_reactor_generator", () ->
            BlockEntityType.Builder.of(mio_icif_nuclear_reactor_generator::new, mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get()).build(null));

    // 注册 反应堆舱 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber>> REACTOR_CHAMBER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("reactor_chamber", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber::new, mio_icif_blocks.REACTOR_CHAMBER.get()).build(null));

    // 注册 reactor fluid port 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port>> REACTOR_FLUID_PORT_ENTITY_TYPE =
        BLOCK_ENTITIES.register("reactor_fluid_port", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port::new, mio_icif_blocks.REACTOR_FLUID_PORT.get()).build(null));

    // 注册 reactor access hatch MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_access_hatch>> REACTOR_ACCESS_HATCH_ENTITY_TYPE =
        BLOCK_ENTITIES.register("reactor_access_hatch", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_access_hatch::new, mio_icif_blocks.REACTOR_ACCESS_HATCH.get()).build(null));

    // 注册 红石反应堆冷却液注入器 MenuType

    // 注册 红石反应堆冷却液注入器 MenuType

    // 注册 红石反应堆冷却液注入器 MenuType

    // 注册 红石反应堆冷却液注入器 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_redstone_reactor_coolant_injector>> REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("redstone_reactor_coolant_injector", () ->
            BlockEntityType.Builder.of(mio_icif_redstone_reactor_coolant_injector::new, mio_icif_blocks.REDSTONE_REACTOR_COOLANT_INJECTOR.get()).build(null));

    // 娉ㄥ唽鍙�?簲鍫嗗喎鍗存恫娉ㄥ�?鍣?MenuType

    // 注册 lapis reactor coolant injector MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_lapis_reactor_coolant_injector>> LAPIS_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("lapis_reactor_coolant_injector", () ->
            BlockEntityType.Builder.of(mio_icif_lapis_reactor_coolant_injector::new, mio_icif_blocks.LAPIS_REACTOR_COOLANT_INJECTOR.get()).build(null));

    // 注册 terra elc 方块实体类型

    // 注册 terra elc 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_terra_elc>> TERRA_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("terra_elc", () ->
            BlockEntityType.Builder.of(mio_icif_terra_elc::new, mio_icif_blocks.TERRA_ELC.get()).build(null));

    // 注册 核弹 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_nuke>> NUKE_ENTITY_TYPE =
        BLOCK_ENTITIES.register("nuke", () ->
            BlockEntityType.Builder.of((pos, state) -> new com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_nuke(pos, state), mio_icif_blocks.NUKE.get()).build(null));

    // 注册 RTG 发电机 MenuType

    // 注册 RTG 发电机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_rt_generator>> RT_GENERATOR_ENTITY_TYPE =
        BLOCK_ENTITIES.register("rt_generator", () ->
            BlockEntityType.Builder.of(mio_icif_rt_generator::new, mio_icif_blocks.RT_GENERATOR.get()).build(null));

    // 注册 rt heat generator MenuType

    // 注册 rt heat generator MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_rt_heat_generator>> RT_HEAT_GENERATOR =
        BLOCK_ENTITIES.register("rt_heat_generator", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_rt_heat_generator::new, mio_icif_blocks.RT_HEAT_GENERATOR.get()).build(null));

    // 注册 磁化机 MenuType

    // 注册 磁化机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_megnetizer>> MAGNETIZER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("magnetizer", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_megnetizer::new, mio_icif_blocks.MAGNETIZER.get()).build(null));

    // 娉ㄥ唽纾佸寲�?MenuType

    // 注册 特斯拉线圈 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_tesla>> TESLA_ENTITY_TYPE =
        BLOCK_ENTITIES.register("tesla", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_tesla::new, mio_icif_blocks.TESLA.get()).build(null));

    // 注册 配电器 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matron_elc>> MATRON_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("matron_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matron_elc::new, mio_icif_blocks.MATRON_ELC.get()).build(null));

    // 注册 收割机 MenuType

    // 注册 收割机 MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_harvest_elc>> HARVEST_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("harvest_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_harvest_elc::new, mio_icif_blocks.HARVEST_ELC.get()).build(null));

    // 注册 传送机 方块实体类型

    // 注册 传送机 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_teleporter_elc>> TELEPORTER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("teleporter_elc", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_teleporter_elc::new, mio_icif_blocks.TELEPORTER_ELC.get()).build(null));

    // 注册 crop 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity>> CROP_ENTITY_TYPE =
        BLOCK_ENTITIES.register("crop", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.crop.mio_icif_crop_entity::new,
                mio_icif_blocks.CROP_STICK.get(),
                mio_icif_blocks.CROP_STICK_UPGRADED.get()).build(null));

    // 注册 scanner elc 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_scanner_elc>> SCANNER_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("scanner_elc", () ->
            BlockEntityType.Builder.of(mio_icif_scanner_elc::new, mio_icif_blocks.SCANNER_ELC.get()).build(null));

    // 注册 molecular transformer MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_molecular_transformer>> MOLECULAR_TRANSFORMER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("molecular_transformer", () ->
            BlockEntityType.Builder.of(mio_icif_molecular_transformer::new, mio_icif_blocks.MOLECULAR_TRANSFORMER.get()).build(null));

    // 娉ㄥ唽妯″紡鎵���弿�??MenuType

    // 注册 pattern storage MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_pattern_storage>> PATTERN_STORAGE_ENTITY_TYPE =
        BLOCK_ENTITIES.register("pattern_storage", () ->
            BlockEntityType.Builder.of(mio_icif_pattern_storage::new, mio_icif_blocks.PATTERN_STORAGE.get()).build(null));

    // 娉ㄥ唽妯″紡瀛樺偍鏈?MenuType

    // 注册 replicator elc MenuType
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_replicator_elc>> REPLICATOR_ELC_ENTITY_TYPE =
        BLOCK_ENTITIES.register("replicator_elc", () ->
            BlockEntityType.Builder.of(mio_icif_replicator_elc::new, mio_icif_blocks.REPLICATOR_ELC.get()).build(null));

    // 娉ㄥ唽澶�?埗�??MenuType

    // 注册 foam 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_block_foam_entity>> FOAM_ENTITY_TYPE =
        BLOCK_ENTITIES.register("foam", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_block_foam_entity::new, mio_icif_blocks.CONSTRUCTION_FOAM.get()).build(null));

    // 注册储物箱方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity>> STORAGE_BOX_ENTITY_TYPE =
        BLOCK_ENTITIES.register("storage_box", () ->
            BlockEntityType.Builder.of(
                com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity::new,
                mio_icif_blocks.STORAGE_BOX_WOOD.get(),
                mio_icif_blocks.STORAGE_BOX_BRONZE.get(),
                mio_icif_blocks.STORAGE_BOX_IRON.get(),
                mio_icif_blocks.STORAGE_BOX_ADVIRON.get(),
                mio_icif_blocks.STORAGE_BOX_IRIDIUM.get(),
                mio_icif_blocks.STORAGE_BOX_TITANIUM.get()
            ).build(null));

    // 注册青铜储罐方块实体类型 (16桶 = 16000 mB)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_bronze_tank_entity>> BRONZE_TANK =
        BLOCK_ENTITIES.register("bronze_tank", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_bronze_tank_entity::new, mio_icif_blocks.BRONZE_TANK.get()).build(null));
    // 注册铁储罐方块实体类型 (32桶 = 32000 mB)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iron_tank_entity>> IRON_TANK =
        BLOCK_ENTITIES.register("iron_tank", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iron_tank_entity::new, mio_icif_blocks.IRON_TANK.get()).build(null));
    // 注册钛储罐方块实体类型 (64桶 = 64000 mB)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_titanium_tank_entity>> TITANIUM_TANK =
        BLOCK_ENTITIES.register("titanium_tank", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_titanium_tank_entity::new, mio_icif_blocks.TITANIUM_TANK.get()).build(null));
    // 注册精炼铁(adviron)储罐方块实体类型 (128桶 = 128000 mB)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_adviron_tank_entity>> ADVIRON_TANK =
        BLOCK_ENTITIES.register("adviron_tank", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_adviron_tank_entity::new, mio_icif_blocks.ADVIRON_TANK.get()).build(null));
    // 注册铱储罐方块实体类型 (1024桶 = 1024000 mB)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iridium_tank_entity>> IRIDIUM_TANK =
        BLOCK_ENTITIES.register("iridium_tank", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iridium_tank_entity::new, mio_icif_blocks.IRIDIUM_TANK.get()).build(null));

    // 注册�?�桶方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_barrel_entity>> BARREL =
        BLOCK_ENTITIES.register("barrel", () ->
            BlockEntityType.Builder.of(mio_icif_barrel_entity::new, mio_icif_blocks.BARREL.get()).build(null));

    // 注册 item buffer elc 方块实体类型
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_item_buffer_elc>> ITEM_BUFFER_ELC =
        BLOCK_ENTITIES.register("item_buffer_elc", () ->
            BlockEntityType.Builder.of(mio_icif_item_buffer_elc::new, mio_icif_blocks.ITEM_BUFFER_ELC.get()).build(null));

    // 注册电动作?拣机方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_sorter_elc>> SORTER_ELC =
        BLOCK_ENTITIES.register("sorter_elc", () ->
            BlockEntityType.Builder.of(mio_icif_sorter_elc::new, mio_icif_blocks.SORTER_ELC.get()).build(null));

    // 注册高级物品栈??配机方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_item_distributor_elc>> ITEM_DISTRIBUTOR_ELC =
        BLOCK_ENTITIES.register("item_distributor_elc", () ->
            BlockEntityType.Builder.of(mio_icif_item_distributor_elc::new, mio_icif_blocks.ITEM_DISTRIBUTOR_ELC.get()).build(null));

    // 注册流体�??配机方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_fluid_distributor_elc>> FLUID_DISTRIBUTOR_ELC =
        BLOCK_ENTITIES.register("fluid_distributor_elc", () ->
            BlockEntityType.Builder.of(mio_icif_fluid_distributor_elc::new, mio_icif_blocks.FLUID_DISTRIBUTOR_ELC.get()).build(null));

    // 注册高级流体�??配机方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_weighted_fluid_distributor_elc>> WEIGHTED_FLUID_DISTRIBUTOR_ELC =
        BLOCK_ENTITIES.register("weighted_fluid_distributor_elc", () ->
            BlockEntityType.Builder.of(mio_icif_weighted_fluid_distributor_elc::new, mio_icif_blocks.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get()).build(null));

    // 注册流体�?量�?节机方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_fluid_regulator_elc>> FLUID_REGULATOR_ELC =
        BLOCK_ENTITIES.register("fluid_regulator_elc", () ->
            BlockEntityType.Builder.of(mio_icif_fluid_regulator_elc::new, mio_icif_blocks.FLUID_REGULATOR_ELC.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_batch_crafter>> BATCH_CRAFTER =
        BLOCK_ENTITIES.register("batch_crafter", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_batch_crafter::new, mio_icif_blocks.BATCH_CRAFTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_chunk_loader>> CHUNK_LOADER =
        BLOCK_ENTITIES.register("chunk_loader", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_chunk_loader::new, mio_icif_blocks.CHUNK_LOADER.get()).build(null));

    // 注册工业工作台方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_industrial_workbench>> INDUSTRIAL_WORKBENCH_ENTITY_TYPE =
        BLOCK_ENTITIES.register("industrial_workbench", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_industrial_workbench::new, mio_icif_blocks.INDUSTRIAL_WORKBENCH.get()).build(null));

    // 注册能源转换器方块实体类�?
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_energy_converter_entity>> ENERGY_CONVERTER_ENTITY_TYPE =
        BLOCK_ENTITIES.register("energy_converter", () ->
            BlockEntityType.Builder.of(mio_icif_energy_converter_entity::new, mio_icif_blocks.ENERGY_CONVERTER.get()).build(null));

    // ========== 石油钻机方块实体 ==========
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_core_entity>> OIL_RIG_CORE =
        BLOCK_ENTITIES.register("oil_rig_core", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_core_entity::new, mio_icif_blocks.OIL_RIG_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_base_entity>> OIL_RIG_BASE =
        BLOCK_ENTITIES.register("oil_rig_base", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_base_entity::new, mio_icif_blocks.OIL_RIG_BASE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_input_entity>> OIL_RIG_INPUT =
        BLOCK_ENTITIES.register("oil_rig_input", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_input_entity::new, mio_icif_blocks.OIL_RIG_INPUT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_output_entity>> OIL_RIG_OUTPUT =
        BLOCK_ENTITIES.register("oil_rig_output", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_output_entity::new, mio_icif_blocks.OIL_RIG_OUTPUT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_panel_entity>> OIL_RIG_PANEL =
        BLOCK_ENTITIES.register("oil_rig_panel", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_panel_entity::new, mio_icif_blocks.OIL_RIG_PANEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_dimension_oil_rig_core_entity>> DIMENSION_OIL_RIG_CORE =
        BLOCK_ENTITIES.register("dimension_oil_rig_core", () ->
            BlockEntityType.Builder.of(com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_dimension_oil_rig_core_entity::new, mio_icif_blocks.DIMENSION_OIL_RIG_CORE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}