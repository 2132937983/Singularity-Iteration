package com.singularity_iteration.mio_icif.Blocks;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_buffer_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_weighted_fluid_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_regulator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_geo_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_Semifluid_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_powder_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_powder_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_compressor_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former_advanced;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_washer_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fermenter_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_oil_refinery_elc;
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
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_extrator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_compressor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pump_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_redstone_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_lapis_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_terra_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_generator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_solid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_fluid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_source_fluid;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Kinetic_Generator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Manual_KineticU_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Stirling_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_twin_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_rt_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_advanced_semifluid_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_rt_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer;
import com.singularity_iteration.mio_icif.Blocks.entity.energy_converter.mio_icif_energy_converter_entity;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.ILongEnergyStorage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 能力注册类
 * 统一管理所有方块实体的能力注册
 */
@SuppressWarnings("null")
public class mio_icif_capacities {

    private static void registerAPIHeatCapability(RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            entityType,
            (blockEntity, direction) -> MioIcifAPI.instance().getCapabilities().adaptHeatStorage(blockEntity)
        );
    }

    private static void registerAPIKineticCapability(RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            entityType,
            (blockEntity, direction) -> MioIcifAPI.instance().getCapabilities().adaptKineticStorage(blockEntity)
        );
    }

    private static void registerWireCapabilities(RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
        event.registerBlockEntity(
            EUApi.SIDED,
            entityType,
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            ILongEnergyStorage.BLOCK,
            entityType,
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
    }

    private static void registerEnergyCapabilities(RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<?> entityType) {
        event.registerBlockEntity(
            EUApi.SIDED,
            entityType,
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            ILongEnergyStorage.BLOCK,
            entityType,
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
    }

    private static void registerFEAndLongCapabilities(RegisterCapabilitiesEvent event) {
        var energyBlockEntities = java.util.List.of(
            mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.BATBOX.get(),
            mio_icif_block_entities.BATBOX_CHARGER.get(),
            mio_icif_block_entities.CESU_CHARGER.get(),
            mio_icif_block_entities.MFE_CHARGER.get(),
            mio_icif_block_entities.MFSU_CHARGER.get(),
            mio_icif_block_entities.MFSU.get(),
            mio_icif_block_entities.MFE.get(),
            mio_icif_block_entities.CESU.get(),
            mio_icif_block_entities.LESU.get(),
            mio_icif_block_entities.LESU_CHARGER.get(),
            mio_icif_block_entities.EESU.get(),
            mio_icif_block_entities.EESU_CHARGER.get(),
            mio_icif_block_entities.GESU_CORE.get(),
            mio_icif_block_entities.GESU_INPUT_IV.get(),
            mio_icif_block_entities.GESU_OUTPUT_IV.get(),
            mio_icif_block_entities.GESU_OUTPUT_LUV.get(),
            mio_icif_block_entities.FURNACE_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.POWDER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get(),
            mio_icif_block_entities.WASHER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get(),
            mio_icif_block_entities.BLAST_FURNACE_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get(),
            mio_icif_block_entities.SOLAR_DISTILLER_ENTITY_TYPE.get(),
            mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get(),
            mio_icif_block_entities.BLOCK_CUTTER_ENTITY_TYPE.get(),
            mio_icif_block_entities.LATHE_ENTITY_TYPE.get(),
            mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.EXTRACTOR_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIND_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.WATER_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),
            mio_icif_block_entities.SOLID_HEAT_GENERATOR.get(),
            mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(),
            mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.DROP_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_DROP_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.EXPERIENCE_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_EXPERIENCE_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.TRANSFORMER_LV_MV.get(),
            mio_icif_block_entities.TRANSFORMER_MV_HV.get(),
            mio_icif_block_entities.TRANSFORMER_HV_EV.get(),
            mio_icif_block_entities.TRANSFORMER_EV_SC.get(),
            mio_icif_block_entities.TRANSFORMER_IV_LUV.get(),
            mio_icif_block_entities.TRANSFORMER_LUV_ZPMV.get(),
            mio_icif_block_entities.FUTURE_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.HEAT_SOURCE_FLUID.get(),
            mio_icif_block_entities.COMPRESSOR_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.MINER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_MINER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.ELECTROLYZER.get(),
            mio_icif_block_entities.RECYCLER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.METAL_FORMER_ENTITY_TYPE.get(),
            mio_icif_block_entities.CENTRIFUGE_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(),
            mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get(),
            mio_icif_block_entities.REACTOR_ACCESS_HATCH_ENTITY_TYPE.get(),
            mio_icif_block_entities.REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.LAPIS_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.TERRA_ELC_ENTITY_TYPE.get(),
            mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ADVANCED_SOLAR_PANEL_ENTITY_TYPE.get(),
            mio_icif_block_entities.HYBRID_SOLAR_PANEL_ENTITY_TYPE.get(),
            mio_icif_block_entities.ULTIMATE_HYBRID_SOLAR_PANEL_ENTITY_TYPE.get(),
            mio_icif_block_entities.QUANTUM_SOLAR_PANEL_ENTITY_TYPE.get(),
            mio_icif_block_entities.METS_ADVANCED_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.QUANTUM_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_LV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_MV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_HV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_EV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_IV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_LuV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_ISOLATION_LV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_ISOLATION_MV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_ISOLATION_HV_ENTITY_TYPE.get(),
            mio_icif_block_entities.WIRE_ISOLATION_EV_ENTITY_TYPE.get()
        );
        for (var entityType : energyBlockEntities) {
            event.registerBlockEntity(
                ILongEnergyStorage.BLOCK,
                entityType,
                (blockEntity, direction) -> {
                    if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                        return energyBlock.getEnergyStorageCapability(direction);
                    }
                    return null;
                }
            );
        }
        Singularity_Iteration.LOGGER.info("Registered ILongEnergyStorage capabilities for all energy block entities");
    }

    /**
     * 注册所有方块实体的能力
     * @param event 能力注册事件
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerFEAndLongCapabilities(event);

        // Register EU energy storage capability for Thermal Generator
        registerEnergyCapabilities(event, mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get());

        // Register item handler capability for Thermal Generator
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy, FE and item capabilities for Thermal Generator");

        // Register EU energy storage capability for BatBox
        registerEnergyCapabilities(event, mio_icif_block_entities.BATBOX.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for BatBox");

        // Register EU energy storage capability for MFSU
        registerEnergyCapabilities(event, mio_icif_block_entities.MFSU.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for MFSU");

        // Register EU energy storage capability for MFE
        registerEnergyCapabilities(event, mio_icif_block_entities.MFE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for MFE");

        // Register EU energy storage capability for CESU
        registerEnergyCapabilities(event, mio_icif_block_entities.CESU.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for CESU");

        // Register EU energy storage capability for LESU
        registerEnergyCapabilities(event, mio_icif_block_entities.LESU.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for LESU");

        // Register EU energy storage capability for LESU Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.LESU_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for LESU Charger");

        // Register EU energy storage capability for EESU
        registerEnergyCapabilities(event, mio_icif_block_entities.EESU.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for EESU");

        // Register EU energy storage capability for EESU Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.EESU_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for EESU Charger");

        // Register EU energy storage capability for GESU Core
        registerEnergyCapabilities(event, mio_icif_block_entities.GESU_CORE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for GESU Core");

        // Register EU energy storage capability for GESU Input Module IV
        registerEnergyCapabilities(event, mio_icif_block_entities.GESU_INPUT_IV.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for GESU Input Module IV");

        // Register EU energy storage capability for GESU Output Module IV
        registerEnergyCapabilities(event, mio_icif_block_entities.GESU_OUTPUT_IV.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for GESU Output Module IV");

        // Register EU energy storage capability for GESU Output Module LuV
        registerEnergyCapabilities(event, mio_icif_block_entities.GESU_OUTPUT_LUV.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for GESU Output Module LuV");

        // Register EU energy storage capability for Oil Rig Core
        registerEnergyCapabilities(event, mio_icif_block_entities.OIL_RIG_CORE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Oil Rig Core");

        // Register EU energy storage capability for Oil Rig Base
        registerEnergyCapabilities(event, mio_icif_block_entities.OIL_RIG_BASE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Oil Rig Base");

        // Register EU energy storage capability for Oil Rig Input
        registerEnergyCapabilities(event, mio_icif_block_entities.OIL_RIG_INPUT.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Oil Rig Input");

        // Register EU energy storage capability for Oil Rig Output
        registerEnergyCapabilities(event, mio_icif_block_entities.OIL_RIG_OUTPUT.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Oil Rig Output");

        // Register EU energy storage capability for Oil Rig Panel
        registerEnergyCapabilities(event, mio_icif_block_entities.OIL_RIG_PANEL.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Oil Rig Panel");

        // Register EU energy storage capability for Dimension Oil Rig Core
        registerEnergyCapabilities(event, mio_icif_block_entities.DIMENSION_OIL_RIG_CORE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for Dimension Oil Rig Core");

        // Register EU energy storage capability for BatBox Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.BATBOX_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for BatBox Charger");

        // Register EU energy storage capability for CESU Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.CESU_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for CESU Charger");

        // Register EU energy storage capability for MFE Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.MFE_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for MFE Charger");

        // Register EU energy storage capability for MFSU Charger
        registerEnergyCapabilities(event, mio_icif_block_entities.MFSU_CHARGER.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and FE capabilities for MFSU Charger");

        registerEnergyCapabilities(event, mio_icif_block_entities.WIRELESS_POWER_TRANSMISSION_NODE.get());

        // Register EU energy storage capability for Energy Converter
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ENERGY_CONVERTER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_energy_converter_entity converter) {
                    return converter.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        // Register FE energy storage capability for Energy Converter
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            mio_icif_block_entities.ENERGY_CONVERTER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_energy_converter_entity converter) {
                    return converter.getFEStorage();
                }
                return null;
            }
        );
        // Register ILongEnergyStorage capability for Energy Converter
        event.registerBlockEntity(
            ILongEnergyStorage.BLOCK,
            mio_icif_block_entities.ENERGY_CONVERTER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_energy_converter_entity converter) {
                    return converter.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU and FE energy capabilities for Energy Converter");

        // Register EU energy storage capability for all Wire tiers
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_LV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_MV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_HV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_EV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_IV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_LuV_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for all Wire tiers");

        // Register EU energy storage capability for all Isolation Wire tiers
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_ISOLATION_LV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_ISOLATION_MV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_ISOLATION_HV_ENTITY_TYPE.get());
        registerWireCapabilities(event, mio_icif_block_entities.WIRE_ISOLATION_EV_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for all Isolation Wire tiers");

        // Register EU energy storage capability for Electric Furnace
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.FURNACE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Electric Furnace
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.FURNACE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_furnace_elc furnace) {
                    return furnace.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Electric Furnace");

        // Register EU energy storage capability for Powder Machine (打粉机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.POWDER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Powder Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.POWDER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_powder_elc powder) {
                    return powder.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Powder Machine");

        // Register EU energy storage capability for Advanced Powder Machine (进阶旋风打粉机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Advanced Powder Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.POWDER_ADVANCED_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_powder_advanced_elc powder) {
                    return powder.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Advanced Powder Machine");

        // Register capabilities for Advanced Compressor Machine (进阶压缩机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.COMPRESSOR_ADVANCED_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_compressor_advanced_elc compressor) {
                    return compressor.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Extruding Machine (挤压机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_metal_former_advanced metalFormer) {
                    return metalFormer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Extractor Machine (提取机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.EXTRACTOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Extractor Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.EXTRACTOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_extrator_elc extractor) {
                    return extractor.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Extractor Machine");

        // Register capabilities for Washer Machine (洗矿机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.WASHER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.WASHER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_washer_elc washer) {
                    return washer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.WASHER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_washer_elc washer) {
                    return washer.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Washer Machine");

        // Register capabilities for Fermenter Machine (发酵机)
        // Register HU heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fermenter_elc fermenter) {
                    return fermenter.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fermenter_elc fermenter) {
                    return fermenter.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fermenter_elc fermenter) {
                    return fermenter.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.FERMENTER_ELC_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered HU heat, item and fluid capabilities for Fermenter Machine");

        // Register capabilities for Oil Refinery (精炼机)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
                    return refinery.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
                    return refinery.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_oil_refinery_elc refinery) {
                    return refinery.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Oil Refinery Machine");

        // Register capabilities for Blast Furnace (高炉)
        // Register HU heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace furnace) {
                    return furnace.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace furnace) {
                    return furnace.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace furnace) {
                    return furnace.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered HU heat, item and fluid capabilities for Blast Furnace");

        // Register capabilities for Advanced Titanium Alloy Blast Furnace (进阶钛合金高炉)
        // Register HU heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ADVANCED_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace_advanced furnace) {
                    return furnace.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ADVANCED_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace_advanced furnace) {
                    return furnace.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ADVANCED_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace_advanced furnace) {
                    return furnace.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.BLAST_FURNACE_ADVANCED_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered HU heat, item and fluid capabilities for Advanced Titanium Alloy Blast Furnace");

 // Register capabilities for Electric Nano Blast Furnace (电力纳米高)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.BLAST_FURNACE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.BLAST_FURNACE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_blast_furnace_elc furnace) {
                    return furnace.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Electric Nano Blast Furnace");

        // Register capabilities for Steam Generator (蒸汽机)
        // Register HU heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_generator generator) {
                    return generator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_generator generator) {
                    return generator.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.STEAM_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered HU heat, item and fluid capabilities for Steam Generator");

        // Register capabilities for Steam Repressurizer (蒸汽再加压机)
        // Register HU heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                    return repressurizer.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                    return repressurizer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_repressurizer repressurizer) {
                    return repressurizer.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.STEAM_REPRESSURIZER_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered HU heat, item and fluid capabilities for Steam Repressurizer");

        // Register capabilities for Steam Kinetic Generator (蒸汽动能发生机)
        // Register KU kinetic storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                    return generator.getKineticStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
                    return generator.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.STEAM_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic, item and fluid capabilities for Steam Kinetic Generator");

        // Register fluid handler capability for Water Pipe (水管)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.PIPE_WATER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pipe_fluid pipe) {
                    return pipe.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered fluid capabilities for Water Pipe");

        // Register fluid handler capability for Water Extract Pipe (抽水管)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.PIPE_WATER_EXTRACT_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pipe_fluid_extract pipe) {
                    return pipe.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered fluid capabilities for Water Extract Pipe");

        // Register item handler capability for Item Pipe - Input (输入型物流管)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.PIPE_ITEM_INPUT_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pipe_item pipe) {
                    return pipe.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Item Pipe - Transport (运输型物流管)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pipe_item pipe) {
                    return pipe.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered item capabilities for Item Pipes (Input, Output, Transport)");

        // Register capabilities for Geo Generator (地热发电机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability for lava
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_geo_generator geoGenerator) {
                    return geoGenerator.getLavaHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Geo Generator");

        // Register capabilities for Solar Generator (太阳能发电机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SOLAR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Solar Generator");

        // Register capabilities for Advanced Solar Panel (高级太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.ADVANCED_SOLAR_PANEL_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_SOLAR_PANEL_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Advanced Solar Panel");

        // Register capabilities for Hybrid Solar Panel (混合太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.HYBRID_SOLAR_PANEL_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.HYBRID_SOLAR_PANEL_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Hybrid Solar Panel");

        // Register capabilities for Ultimate Hybrid Solar Panel (终极混合太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.ULTIMATE_HYBRID_SOLAR_PANEL_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ULTIMATE_HYBRID_SOLAR_PANEL_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Ultimate Hybrid Solar Panel");

        // Register capabilities for Quantum Solar Panel (量子太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.QUANTUM_SOLAR_PANEL_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.QUANTUM_SOLAR_PANEL_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Quantum Solar Panel");

        // Register capabilities for METS Advanced Solar Generator (METS进阶太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.METS_ADVANCED_SOLAR_GENERATOR_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.METS_ADVANCED_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for METS Advanced Solar Generator");

        // Register capabilities for Photon Resonance Solar Generator (光子振谐太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Photon Resonance Solar Generator");

        // Register capabilities for Ultimate Photon Resonance Solar Generator (终极光子振谐太阳能发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Ultimate Photon Resonance Solar Generator");

        // Register capabilities for Quantum Generator (量子发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.QUANTUM_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for Quantum Generator");

        // Register capabilities for Wind Generator (风力发电机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.WIND_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.WIND_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Wind Generator");

        // Register capabilities for Water Generator (水力发电机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.WATER_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.WATER_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Water Generator");

        // Register capabilities for Semifluid Generator (半流质发电机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register fluid handler capability for fuel
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Semifluid_generator semifluidGenerator) {
                    return semifluidGenerator.getFuelHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Semifluid Generator");

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_advanced_semifluid_generator semifluidGenerator) {
                    return semifluidGenerator.getFuelHandlerCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_advanced_stirling_generator generator) {
                    return generator.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE.get());

        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_advanced_stirling_generator generator) {
                    return generator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Heat Generator Electric (电力发热机)
        // 电力发热机继承自 mio_icif_producer，使用父类的能量存储
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力（用于电池充电）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_heat_generator_elc heatGenerator) {
                    return heatGenerator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_heat_generator_elc heatGenerator) {
                    return heatGenerator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.HEAT_GENERATOR_ELC.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and HU heat capabilities for Heat Generator Electric");

        // Register capabilities for Solid Heat Generator (固体加热机)
        // 注册物品栏能力（用于燃料输入和灰烬输出）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SOLID_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_solid_heat_generator solidHeatGenerator) {
                    return solidHeatGenerator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.SOLID_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_solid_heat_generator solidHeatGenerator) {
                    return solidHeatGenerator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.SOLID_HEAT_GENERATOR.get());
        Singularity_Iteration.LOGGER.info("Registered item handler and HU heat capabilities for Solid Heat Generator");

        // Register capabilities for Fluid Heat Generator (流体加热机)
        // 注册物品栏能力（用于燃料桶输入和空桶输出）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fluid_heat_generator fluidHeatGenerator) {
                    return fluidHeatGenerator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册流体处理能力（用于沼气输入）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fluid_heat_generator fluidHeatGenerator) {
                    return fluidHeatGenerator.getFuelHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fluid_heat_generator fluidHeatGenerator) {
                    return fluidHeatGenerator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.FLUID_HEAT_GENERATOR.get());
        Singularity_Iteration.LOGGER.info("Registered item handler, fluid handler and HU heat capabilities for Fluid Heat Generator");

        // Register capabilities for RTG Heat Generator (放射性同位素温差加热机)
        // 注册物品栏能力（6个槽位用于RTG靶丸）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.RT_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_rt_heat_generator rtHeatGenerator) {
                    return rtHeatGenerator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.RT_HEAT_GENERATOR.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_rt_heat_generator rtHeatGenerator) {
                    return rtHeatGenerator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.RT_HEAT_GENERATOR.get());
        Singularity_Iteration.LOGGER.info("Registered item handler and HU heat capabilities for RTG Heat Generator");

        // Register capabilities for Stirling Generator (斯特林发电机)
        // 斯特林发电机接收 HU 热能并转换为 EU 电能
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_stirling_generator stirlingGenerator) {
                    return stirlingGenerator.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能接收能力（只从正面接收）
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_stirling_generator stirlingGenerator) {
                    return stirlingGenerator.getHeatStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy and HU heat capabilities for Stirling Generator");

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.DROP_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ADVANCED_DROP_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.EXPERIENCE_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ADVANCED_EXPERIENCE_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Manual Kinetic Generator (手动动能发电机)
        // 注册 KU 动能存储能力
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Manual_KineticU_Generator generator) {
                    return generator.getKineticStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.MANUAL_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic capabilities for Manual Kinetic Generator");

        // Register capabilities for Kinetic Generator (动能发电机)
        // 注册 KU 动能接收能力（只从正面接收，由方块实体控制）
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_kinetic_generator generator) {
                    return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(generator.getKineticStorageCapability(direction));
                }
                return null;
            }
        );

        // 注册 EU 电能输出能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力（用于电池充电）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic and EU energy capabilities for Kinetic Generator");

        // Register capabilities for Turbo Kinetic Generator (涡轮增压动能发电机)
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_turbo_kinetic_generator generator) {
                    return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(generator.getKineticStorageCapability(direction));
                }
                return null;
            }
        );
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        registerAPIKineticCapability(event, mio_icif_block_entities.TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic and EU energy capabilities for Turbo Kinetic Generator");

        // Register capabilities for Twin Turbo Kinetic Generator (双涡轮增压动能发电机)
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_twin_turbo_kinetic_generator generator) {
                    return MioIcifAPI.instance().getCapabilities().adaptKineticStorage(generator.getKineticStorageCapability(direction));
                }
                return null;
            }
        );
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        registerAPIKineticCapability(event, mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic and EU energy capabilities for Twin Turbo Kinetic Generator");

        // Register capabilities for Wind Kinetic Generator (风力动能发生机)
        // 注册 KU 动能存储能力
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Wind_Kinetic_Generator generator) {
                    return generator.getKineticStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力（用于转子槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Wind_Kinetic_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic and item capabilities for Wind Kinetic Generator");

        // Register capabilities for Electric Kinetic Generator (电力动能机)
        // 注册 EU 能量存储能力（输入）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力（用于电池和马达）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Kinetic_Generator_elc kineticGenerator) {
                    return kineticGenerator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 KU 动能输出能力（只从正面输出）
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Kinetic_Generator_elc kineticGenerator) {
                    return kineticGenerator.getKineticStorageCapability(direction);
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and KU kinetic capabilities for Electric Kinetic Generator");

        // Register capabilities for Stirling Kinetic Generator (斯特林动能发生机)
        // 注册 KU 动能输出能力（只从正面输出）
        event.registerBlockEntity(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,
            mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
                    return generator.getKineticStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
                    return generator.getItemHandler();
                }
                return null;
            }
        );

        // 注册流体处理能力（水槽）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
                    // 返回组合流体处理器，同时处理水和热水
                    return new net.neoforged.neoforge.fluids.capability.IFluidHandler() {
                        @Override
                        public int getTanks() {
                            return 2;
                        }

                        @Override
                        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
                            return tank == 0 ? generator.getWaterTank().getFluid() : generator.getHotWaterTank().getFluid();
                        }

                        @Override
                        public int getTankCapacity(int tank) {
                            return tank == 0 ? generator.getWaterTank().getCapacity() : generator.getHotWaterTank().getCapacity();
                        }

                        @Override
                        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
                            return tank == 0 ? generator.getWaterTank().isFluidValid(stack) : generator.getHotWaterTank().isFluidValid(stack);
                        }

                        @Override
                        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
                            // 只能向水槽输入水
                            if (resource.getFluid() == net.minecraft.world.level.material.Fluids.WATER) {
                                return generator.getWaterTank().fill(resource, action);
                            }
                            return 0;
                        }

                        @Override
                        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
                            // 只能从热水槽排出热水
                            if (resource.getFluid() == com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.HOTWATER.get()) {
                                return generator.getHotWaterTank().drain(resource, action);
                            }
                            // 如果请求的是水，不从热水槽排水
                            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
                        }

                        @Override
                        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
                            // 当不明确指定流体类型时，检查热水槽
                            // 但只返回热水，让调用者决定是否接受
                            FluidStack hotWater = generator.getHotWaterTank().getFluid();
                            if (!hotWater.isEmpty()) {
                                int toDrain = Math.min(maxDrain, hotWater.getAmount());
                                if (action.execute()) {
                                    return generator.getHotWaterTank().drain(toDrain, action);
                                } else {
                                    // 模拟模式：返回热水，让调用者决定是否接受
                                    return new FluidStack(hotWater.getFluid(), toDrain);
                                }
                            }
                            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
                        }
                    };
                }
                return null;
            }
        );

        // 注册 HU 热能接收能力（从除正面外的其他五个面接收）
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
                    // 获取方块朝向
                    net.minecraft.core.Direction facing = generator.getBlockState().getValue(
                        com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
                    // 只有非正面方向才能接收热能
                    if (direction != facing) {
                        return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(generator.getHeatStorageCapability(direction));
                    }
                }
                return null;
            }
        );

        registerAPIKineticCapability(event, mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get());
        registerAPIHeatCapability(event, mio_icif_block_entities.STIRLING_KINETIC_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered KU kinetic, item handler, fluid and HU heat capabilities for Stirling Kinetic Generator");

        // Register capabilities for LV-MV Transformer (LV-MV 变压器)
        // 注册 EU 能量存储能力（输入面和输出面有不同的电压等级）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_LV_MV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for LV-MV Transformer");

        // Register capabilities for MV-HV Transformer (MV-HV 变压器)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_MV_HV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for MV-HV Transformer");

        // Register capabilities for HV-EV Transformer (HV-EV 变压器)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_HV_EV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for HV-EV Transformer");

        // Register capabilities for EV-SC Transformer (EV-SC 变压器)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_EV_SC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for EV-SC Transformer");

        // Register capabilities for IV-LuV Transformer (IV-LuV 变压器)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_IV_LUV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for IV-LuV Transformer");

        // Register capabilities for LuV-ZPMV Transformer (LuV-ZPMV 变压器)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TRANSFORMER_LUV_ZPMV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_transformer transformer) {
                    return transformer.getEnergyStorage(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for LuV-ZPMV Transformer");

        // Register capabilities for Voltage Checker (电压检测器)
        // 注册 EU 能量存储能力（检测器只检测不存储，使用自定义包装器）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.CHECKER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.checker.mio_icif_checker checker) {
                    return checker.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy capabilities for Voltage Checker");

        // Register capabilities for Futures Machine (期货机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.FUTURE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.FUTURE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_future_elc futureElc) {
                    return futureElc.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Futures Machine");

        // Register capabilities for Heat Source Fluid (热交换机)
        // Register item handler capability
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.HEAT_SOURCE_FLUID.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_heat_source_fluid heatSource) {
                    return heatSource.getItemHandler();
                }
                return null;
            }
        );

        // Register combined fluid handler capability (for Jade to show both tanks)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.HEAT_SOURCE_FLUID.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_heat_source_fluid heatSource) {
                    return heatSource.getCombinedFluidHandler();
                }
                return null;
            }
        );

        // Register heat storage capability
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.HEAT_SOURCE_FLUID.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_heat_source_fluid heatSource) {
                    com.singularity_iteration.mio_icif.energy.heat.HeatStorage internalStorage = new com.singularity_iteration.mio_icif.energy.heat.HeatStorage(
                        heatSource.getHeatCapacity(),
                        0,  // maxReceive - 热交换机不产生热能，只通过流体转换
                        heatSource.getCurrentHeatOutput(),  // maxExtract - 当前输出功率
                        heatSource.getHeatStored(),  // 当前热能
                        20,     // baseTemp - 基础温度
                        1000,   // maxTemp - 最高温度
                        0.0f    // lossFactor - 无热损失（由方块实体自己管理）
                    ) {
                        @Override
                        public long extractHeat(long maxExtract, boolean simulate) {
                            long extracted = super.extractHeat(maxExtract, simulate);
                            if (!simulate && extracted > 0) {
                                heatSource.consumeHeat((int) extracted);
                            }
                            return extracted;
                        }
                    };
                    return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(internalStorage);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.HEAT_SOURCE_FLUID.get());
        Singularity_Iteration.LOGGER.info("Registered item, fluid and heat capabilities for Heat Source Fluid");

        // Register capabilities for Compressor Machine (压缩机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.COMPRESSOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Compressor Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.COMPRESSOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_compressor_elc compressor) {
                    return compressor.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Compressor Machine");

        // Register capabilities for Canner Machine (装罐机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Canner Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_canner_elc canner) {
                    return canner.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register combined fluid handler capability for Canner Machine (for Jade to show both tanks)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_canner_elc canner) {
                    return canner.getCombinedFluidHandler();
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Canner Machine");

        // Register capabilities for Miner Machine (采矿机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.MINER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Miner Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.MINER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_miner_elc miner) {
                    return miner.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Miner Machine");

        // Register capabilities for Advanced Miner Machine (高级采矿机)
        // Register EU energy storage capability
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ADVANCED_MINER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register item handler capability for Advanced Miner Machine
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_MINER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_advanced_miner_elc miner) {
                    return miner.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Advanced Miner Machine");

        // Register capabilities for Pump Machine (泵)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pump_elc pump) {
                    return pump.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册流体处理能力
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_pump_elc pump) {
                    return pump.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item and fluid capabilities for Pump Machine");

        // Register capabilities for Electrolyzer Machine (电解机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.ELECTROLYZER.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（使用基类的 SidedItemHandler）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ELECTROLYZER.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_electrolyzer_elc electrolyzer) {
                    return electrolyzer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Electrolyzer Machine");

        // Register capabilities for Recycler Machine (回收机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.RECYCLER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（使用基类的 SidedItemHandler）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.RECYCLER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_recycler_elc recycler) {
                    return recycler.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Recycler Machine");

        // Register capabilities for Induction Furnace (感应炉)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（输入槽、电池槽、输出槽和升级槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_induction_elc induction) {
                    return induction.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_induction_elc induction) {
                    return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(induction.getHeatStorageCapability(direction));
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.INDUCTION_ELC_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and HU heat capabilities for Induction Furnace");

        // Register capabilities for Matter Fabricator (UU物质生成机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（废料槽、输入槽、输出槽、电池槽和插件槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matter_elc matterElc) {
                    return matterElc.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册流体处理能力（UU物质槽）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matter_elc matterElc) {
                    return matterElc.getUuMatterTank();
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and fluid capabilities for Matter Fabricator");

        // Register capabilities for Large Fabricator Core (大型物质生成机核心)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.LARGE_FABRICATOR_CORE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.LARGE_FABRICATOR_CORE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_core_entity core) {
                    return core.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Large Fabricator Input IV (大型物质生成机输入模块)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.LARGE_FABRICATOR_INPUT_IV.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Large Fabricator Tank (大型物质生成机储罐模块)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.LARGE_FABRICATOR_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Large Fabricator Scrap (大型物质生成机废料模块)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.LARGE_FABRICATOR_SCRAP.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_scrap_entity scrap) {
                    return scrap.getItemHandler();
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered capabilities for Large Fabricator modules");

        // Register capabilities for Neutron Polymerizer (粒子聚合发生器)
        registerEnergyCapabilities(event, mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get());

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_neutron_polymerizer polymerizer) {
                    return polymerizer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // Register capabilities for Unlimited Generator (无限发电机)
        // 注册 EU 能量存储能力（输出）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（充电槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.UNLIMIT_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Unlimited Generator");

        // Register capabilities for Metal Former (金属成型机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.METAL_FORMER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.METAL_FORMER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_metal_former metalFormer) {
                    return metalFormer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Metal Former");

        // Register capabilities for Centrifuge Machine (热能离心机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.CENTRIFUGE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.CENTRIFUGE_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_centrifuge_elc centrifuge) {
                    return centrifuge.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item capabilities for Centrifuge Machine");

        // Register capabilities for Nuclear Reactor Generator (核反应堆发电机)
        // 注册 EU 能量存储能力（输出）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（54个槽位用于燃料棒、散热片等）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_nuclear_reactor_generator reactor) {
                    return reactor.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册 HU 热能能力（核反应堆会产生热量）
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_nuclear_reactor_generator reactor) {
                    return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(reactor.getHeatStorage());
                }
                return null;
            }
        );

        // 注册流体处理能力（流体反应堆模式使用）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_nuclear_reactor_generator reactor) {
                    return reactor.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        registerAPIHeatCapability(event, mio_icif_block_entities.NUCLEAR_REACTOR_GENERATOR_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler, HU heat and fluid capabilities for Nuclear Reactor Generator");

        // Register capabilities for Reactor Fluid Port (流体反应堆流体端口)
        // 注册流体处理能力（作为流体反应堆的流体访问接口）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port fluidPort) {
                    return fluidPort;
                }
                return null;
            }
        );

        // 注册物品栏能力（用于插件槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_fluid_port fluidPort) {
                    return fluidPort.getItemHandler();
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered fluid and item capabilities for Reactor Fluid Port");

        // Register capabilities for Reactor Chamber (核反应仓)
        // 核反应仓本身没有存储，只是转发到连接的核反应堆
        
        // 注册 EU 能量存储能力（转发到核反应堆）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber chamber) {
                    return chamber.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        
        // 注册 HU 热能能力（转发到核反应堆）
        event.registerBlockEntity(
            IMioIcifCapabilities.HEAT_STORAGE_BLOCK,
            mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber chamber) {
                    return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(chamber.getHeatStorageCapability(direction));
                }
                return null;
            }
        );
        
        // 注册物品栏能力（转发到核反应堆）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber chamber) {
                    return chamber.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        
        registerAPIHeatCapability(event, mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get());
        Singularity_Iteration.LOGGER.info("Registered EU energy, HU heat and item handler capabilities for Reactor Chamber");

        // Register capabilities for Redstone Reactor Coolant Injector (反应堆冷却液注入器)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_redstone_reactor_coolant_injector injector) {
                    return injector.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Redstone Reactor Coolant Injector");

        // Register capabilities for Lapis Reactor Coolant Injector (青金石反应堆冷却液注入器)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.LAPIS_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.LAPIS_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_lapis_reactor_coolant_injector injector) {
                    return injector.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Lapis Reactor Coolant Injector");

        // Register capabilities for RTG Generator (放射性同位素温差发电机)
        // 注册 EU 能量存储能力（输出）
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（6个槽位用于RTG靶丸）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_rt_generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for RTG Generator");

        // 注册磁化机的 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.MAGNETIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册磁化机的物品栏能力（电池槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.MAGNETIZER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_megnetizer magnetizer) {
                    return magnetizer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Magnetizer");

        // Register capabilities for Terra ELC (地形转换机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TERRA_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（模板槽和电池槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.TERRA_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_terra_elc terraElc) {
                    return terraElc.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Terra ELC");

        // Register capabilities for Teleporter (传送机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TELEPORTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );



        // 注册物品栏能力（电池槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.TELEPORTER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_teleporter_elc teleporter) {
                    return teleporter.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Teleporter");

        // Register capabilities for Tesla Coil (特斯拉线圈)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.TESLA_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（电池槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.TESLA_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_tesla tesla) {
                    return tesla.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Tesla Coil");

        // 注册作物监管机 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.MATRON_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册作物监管机物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.MATRON_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matron_elc matron) {
                    return matron.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册作物监管机流体处理能力
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.MATRON_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_matron_elc matron) {
                    return matron.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and fluid handler capabilities for Crop Matron");

        // Register capabilities for Crop Harvester (作物收割机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.HARVEST_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.HARVEST_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_harvest_elc harvester) {
                    return harvester.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Crop Harvester");

        // Register capabilities for Pattern Scanner (模式扫描机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.SCANNER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（电池槽和物品槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SCANNER_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_scanner_elc scanner) {
                    return scanner.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Pattern Scanner");

        // Register capabilities for Pattern Storage (模式存储机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力（记忆水晶槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pattern_storage storage) {
                    return storage.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Pattern Storage");

        // Register capabilities for Replicator (复制机)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );


        // 注册物品栏能力
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_replicator_elc replicator) {
                    return replicator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );

        // 注册流体处理能力（UU物质槽）
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_replicator_elc replicator) {
                    return replicator.getUuMatterTank();
                }
                return null;
            }
        );

        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and fluid handler capabilities for Replicator");

        // Register capabilities for Solar Distiller (太阳能蒸馏机)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SOLAR_DISTILLER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_solar_distiller distiller) {
                    return distiller.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.SOLAR_DISTILLER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_solar_distiller distiller) {
                    return distiller.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered item handler and fluid handler capabilities for Solar Distiller");

        // Register capabilities for Condenser (冷凝机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_condenser condenser) {
                    return condenser.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_condenser condenser) {
                    return condenser.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy, item handler and fluid handler capabilities for Condenser");

        // Register capabilities for Block Cutter (方块切割机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.BLOCK_CUTTER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.BLOCK_CUTTER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_block_cutter cutter) {
                    return cutter.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Block Cutter");

        // Register capabilities for Lathe (车床)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.LATHE_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_lathe lathe) {
                    return lathe.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered item handler capability for Lathe");

        // Register capabilities for Electric Sorter (电动分拣机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.SORTER_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.SORTER_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_producer producer) {
                    return producer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Electric Sorter");

        // Register capabilities for Item Buffer (物品缓冲机)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ITEM_BUFFER_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_item_buffer_elc buffer) {
                    return buffer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered item handler capability for Item Buffer");

        // Register capabilities for Advanced Item Distributor (高级物品分配机 - 无需电力)
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.ITEM_DISTRIBUTOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_item_distributor_elc dist) {
                    return dist.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered item handler capability for Advanced Item Distributor");

        // Register capabilities for Fluid Distributor (流体分配机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.FLUID_DISTRIBUTOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.FLUID_DISTRIBUTOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fluid_distributor_elc distributor) {
                    return distributor.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and fluid handler capabilities for Fluid Distributor");

        // Register capabilities for Advanced Fluid Distributor (高级流体分配机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_weighted_fluid_distributor_elc distributor) {
                    return distributor.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and fluid handler capabilities for Advanced Fluid Distributor");

        // Register capabilities for Fluid Regulator (流体流量调节机)
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.FLUID_REGULATOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.FLUID_REGULATOR_ELC.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_fluid_regulator_elc regulator) {
                    return regulator.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );

        event.registerBlockEntity(EUApi.SIDED, mio_icif_block_entities.BATCH_CRAFTER.get(), (blockEntity, direction) -> {
            if (blockEntity instanceof mio_icif_Energy_Block energyBlock) return energyBlock.getEnergyStorageCapability(direction);
            return null;
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, mio_icif_block_entities.BATCH_CRAFTER.get(), (blockEntity, direction) -> {
            if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_batch_crafter crafter) return crafter.getItemHandlerCapability(direction);
            return null;
        });

        event.registerBlockEntity(EUApi.SIDED, mio_icif_block_entities.CHUNK_LOADER.get(), (blockEntity, direction) -> {
            if (blockEntity instanceof mio_icif_Energy_Block energyBlock) return energyBlock.getEnergyStorageCapability(direction);
            return null;
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, mio_icif_block_entities.CHUNK_LOADER.get(), (blockEntity, direction) -> {
            if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_chunk_loader loader) return loader.getItemHandler();
            return null;
        });

        // Register item handler capability for Storage Box
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.STORAGE_BOX_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity storageBox) {
                    return storageBox.getItemHandler();
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and fluid handler capabilities for Fluid Regulator");

        // Register fluid handler capability for Oil Rig Output (储油模块)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.OIL_RIG_OUTPUT.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_output_entity output) {
                    return output.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered fluid handler capability for Oil Rig Output");

        // Register capabilities for Molecular Transformer (分子重组仪)
        // 注册 EU 能量存储能力
        event.registerBlockEntity(
            EUApi.SIDED,
            mio_icif_block_entities.MOLECULAR_TRANSFORMER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    return energyBlock.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );

        // 注册物品栏能力（输入槽和输出槽）
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.MOLECULAR_TRANSFORMER_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_molecular_transformer transformer) {
                    return transformer.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Molecular Transformer");

        // Register capabilities for Geomagnetic Generator (地磁发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.GEOMAGNETIC_GENERATOR_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.GEOMAGNETIC_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Geomagnetic Generator");

        // Register capabilities for Diesel Generator (柴油发电机)
        registerEnergyCapabilities(event, mio_icif_block_entities.DIESEL_GENERATOR_ENTITY_TYPE.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            mio_icif_block_entities.DIESEL_GENERATOR_ENTITY_TYPE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof mio_icif_Energy_Generator generator) {
                    return generator.getItemHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered EU energy and item handler capabilities for Diesel Generator");

        // 注册青铜储罐的流体能力 (16桶 = 16000 mB)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.BRONZE_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_bronze_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        // 注册铁储罐的流体能力 (32桶 = 32000 mB)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.IRON_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iron_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        // 注册钛储罐的流体能力 (64桶 = 64000 mB)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.TITANIUM_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_titanium_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        // 注册精炼铁(adviron)储罐的流体能力 (128桶 = 128000 mB)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.ADVIRON_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_adviron_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        // 注册铱储罐的流体能力 (1024桶 = 1024000 mB)
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.IRIDIUM_TANK.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_iridium_tank_entity tank) {
                    return tank.getFluidHandlerCapability(direction);
                }
                return null;
            }
        );
        Singularity_Iteration.LOGGER.info("Registered fluid handler capabilities for all tanks (Bronze/Iron/Titanium/Adviron/Iridium)");

        // 注册流体单元的 Forge Fluid Capability
        registerCellFluidCapabilities(event);
    }

    /**
     * 注册流体单元的 Forge Fluid Handler Item Capability
     * 使单元可以与其他模组的流体系统交互
     */
    private static void registerCellFluidCapabilities(RegisterCapabilitiesEvent event) {
        // 注册所有流体单元的 FluidHandler.ITEM 能力
        var cellItems = java.util.List.of(
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_WATER.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_LAVA.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_BIOGAS.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_HOTWATER.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_BIOMASS.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_CONSTRUCTIONFOAM.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_COOLANT.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_DISTILLEDWATER.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_HOTCOOLANT.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_PAHOEHOELAVA.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_STEAM.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_SUPERHEATEDSTEAM.get(),
            com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_UUMATTER.get()
        );

        for (var cellItem : cellItems) {
            if (cellItem instanceof com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cell cell) {
                event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> cell.createFluidHandler(stack),
                    cellItem
                );
            }
        }
        
        // 注册动态单元的 Fluid Capability
        var dynamicCell = com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_EMPTY.get();
        if (dynamicCell instanceof com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell dynamicCellItem) {
            event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> dynamicCellItem.createFluidHandler(stack),
                dynamicCell
            );
        }
        
        Singularity_Iteration.LOGGER.info("Registered FluidHandler.ITEM capabilities for all fluid cells including dynamic cell");
    }

    /**
     * 将能力注册器注册到事件总线
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        eventBus.addListener(mio_icif_capacities::registerCapabilities);
    }
}