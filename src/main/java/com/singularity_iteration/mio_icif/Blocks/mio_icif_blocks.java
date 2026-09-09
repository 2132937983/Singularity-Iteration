package com.singularity_iteration.mio_icif.Blocks;

import com.singularity_iteration.mio_icif.Blocks.Environment.BlockRubberWood;
import com.singularity_iteration.mio_icif.Blocks.Environment.BlockStrippedRubberWood;
import com.singularity_iteration.mio_icif.Blocks.Environment.mio_icif_have_rub_wood;
import com.singularity_iteration.mio_icif.Blocks.Environment.mio_icif_radioactive_block;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.mio_icif_sounds;
import com.singularity_iteration.mio_icif.world.feature.WorldGeneration;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_IC_TNT;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Nuke;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Access_Hatch;
import com.singularity_iteration.mio_icif.Blocks.Ore.mio_icif_block_Ore_Lead;
import com.singularity_iteration.mio_icif.Blocks.Ore.mio_icif_block_Ore_Uranium;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water_extract;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_alloy_door;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_alloy_glass;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_irradiant_glass_pane;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_bronze;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_fence_iron;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_foam;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_scaffold;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_storage_box;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_planks;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_stairs;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_slab;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_fence;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_fence_gate;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_door;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_trapdoor;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_pressure_plate;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_button;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_sign;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_wall_sign;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_hanging_sign;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_rubber_wall_hanging_sign;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_bronze_tank;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_iron_tank;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_titanium_tank;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_adviron_tank;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_iridium_tank;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.CopperRichCrop;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.IronRichCrop;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.LeadRichCrop;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.TinRichCrop;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.TitaniumRichCrop;
import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.UraniumRichCrop;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_core;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_base;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_input;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_output;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_panel;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_titanium_drill_frame;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_dimension_oil_rig_core;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_bat_box;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_batbox_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_cesu_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_mfe_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_mfsu_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_mfsu;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_mfe;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_cesu;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_lesu;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_lesu_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_eesu;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_eesu_charger;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_core;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_input_iv;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_output_iv;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_output_luv;
import com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick;
import com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick_upgraded;
import com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_block_weed;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_magnetizer;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_tesla;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matron;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_harvest;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_powder_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_powder_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_washer_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_centrifuge_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_future_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_extrator_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_compressor_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_compressor_advanced_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former_advanced;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_industrial_workbench;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fermenter_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_oil_refinery_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_barrel;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace_advanced;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_generator;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_steam_repressurizer;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_solar_distiller;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_condenser;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_block_cutter;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_lathe;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_block_steam_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_canner_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_miner_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_advanced_miner_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_mining_pipe;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_pump_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_electrolyzer_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_recycler_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_redstone_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_lapis_reactor_coolant_injector;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_terra_elc;
import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_teleporter_elc;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_lTom;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_mToh;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_hToe;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_eTos;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_iv;
import com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer_luv;
import com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wireless_power_transmission_node;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Manual_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Wind_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Water_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_block_Kinetic_Generator_elc;
import com.singularity_iteration.mio_icif.Blocks.KUGenerator.mio_icif_Block_Stirling_Kinetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Geo_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_twin_turbo_kinetic_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_RT_Generator;
import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_generator_elc;
import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_solid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_fluid_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_rt_heat_generator;
import com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_heat_source_fluid;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_reactorvessel;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_block_reactor_redstone_port;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Fluid_Port;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Semifluid_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Geomagnetic_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_geomagnetic_antenna;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_geomagnetic_pedestal;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Diesel_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_drop_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_drop_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_stirling_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Advanced_Semifluid_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_experience_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_experience_generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Solar_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_AdvancedSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_HybridSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_UltimateHybridSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_QuantumSolarPanel;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_MetsAdvancedSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_PhotonResonanceSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_UltimatePhotonResonanceSolarGenerator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_QuantumGenerator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Thermal_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Wind_Generator;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Water_Generator;
import com.singularity_iteration.mio_icif.Blocks.energy_converter.mio_icif_block_energy_converter;
import com.singularity_iteration.mio_icif.Items.mio_icif_items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class mio_icif_blocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);

    private static <T extends Block> void registerBlockItems(String name, DeferredBlock<T> block) {
        mio_icif_items.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    private static <T extends Block> DeferredBlock<T> registerBlockNoItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    public static final DeferredBlock<Block> BLOCK_ORE_TIN = registerBlock("block_ore_tin", () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BLOCK_TIN = registerBlock("block_tin", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    
    public static final DeferredBlock<mio_icif_block_Ore_Uranium> BLOCK_ORE_URAN = registerBlock("block_ore_uran", () -> new mio_icif_block_Ore_Uranium(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks()));

    public static final DeferredBlock<Block> BLOCK_URAN = registerBlock("block_uranium", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_Ore_Lead> BLOCK_ORE_LEAD = registerBlock("block_ore_lead", () -> new mio_icif_block_Ore_Lead(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks()));

    public static final DeferredBlock<Block> BLOCK_LEAD = registerBlock("block_lead", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册粗矿块
    public static final DeferredBlock<Block> BLOCK_RAW_TIN = registerBlock("block_raw_tin", () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_RAW_URAN = registerBlock("block_raw_uranium", () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_RAW_LEAD = registerBlock("block_raw_lead", () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_RAW_NIOBIUM = registerBlock("block_raw_niobium", () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_RAW_TITANIUM = registerBlock("block_raw_titanium", () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // 注册深层矿石变种
    public static final DeferredBlock<Block> BLOCK_ORE_TIN_IN_DEEP = registerBlock("block_ore_tin_in_deep", () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BLOCK_ORE_URAN_IN_DEEP = registerBlock("block_ore_uran_in_deep", () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops().randomTicks()));

    public static final DeferredBlock<Block> BLOCK_ORE_LEAD_IN_DEEP = registerBlock("block_ore_lead_in_deep", () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops().randomTicks()));

    // 注册铌矿石
    public static final DeferredBlock<Block> BLOCK_ORE_NIOBIUM = registerBlock("block_ore_niobium", () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_ORE_NIOBIUM_IN_DEEP = registerBlock("block_ore_niobium_in_deep", () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_NIOBIUM = registerBlock("block_niobium", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册钛矿石
    public static final DeferredBlock<Block> BLOCK_ORE_TITANIUM = registerBlock("block_ore_titanium", () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_ORE_TITANIUM_IN_DEEP = registerBlock("block_ore_titanium_in_deep", () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BLOCK_TITANIUM = registerBlock("block_titanium", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册基础机器外壳
    public static final DeferredBlock<Block> MACHINE_HULL_BASIC = registerBlock("producer/block_machine_hull_basic", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册高级机器外壳
    public static final DeferredBlock<Block> MACHINE_HULL_ADVANCED = registerBlock("producer/block_machine_hull_advanced", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册火力发电机方法
public static final DeferredBlock<mio_icif_Block_Thermal_Generator> THERMAL_GENERATOR =
        registerBlock("generator/block_thermal_generator", () -> new mio_icif_Block_Thermal_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册地热发电机方法
public static final DeferredBlock<mio_icif_Block_Geo_Generator> GEO_GENERATOR =
        registerBlock("generator/block_geo_generator", () -> new mio_icif_Block_Geo_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册太阳能发电机方块
    public static final DeferredBlock<mio_icif_Block_Solar_Generator> SOLAR_GENERATOR =
        registerBlock("generator/block_solar_generator", () -> new mio_icif_Block_Solar_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册高级太阳能发电机方块
    public static final DeferredBlock<mio_icif_Block_AdvancedSolarPanel> ADVANCED_SOLAR_PANEL =
        registerBlock("generator/block_advanced_solar_panel", () -> new mio_icif_Block_AdvancedSolarPanel(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册混合太阳能发电机方块
    public static final DeferredBlock<mio_icif_Block_HybridSolarPanel> HYBRID_SOLAR_PANEL =
        registerBlock("generator/block_hybrid_solar_panel", () -> new mio_icif_Block_HybridSolarPanel(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册终极混合太阳能发电机方块
    public static final DeferredBlock<mio_icif_Block_UltimateHybridSolarPanel> ULTIMATE_HYBRID_SOLAR_PANEL =
        registerBlock("generator/block_ultimate_hybrid_solar_panel", () -> new mio_icif_Block_UltimateHybridSolarPanel(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册量子太阳能发电机方块
    public static final DeferredBlock<mio_icif_Block_QuantumSolarPanel> QUANTUM_SOLAR_PANEL =
        registerBlock("generator/block_quantum_solar_panel", () -> new mio_icif_Block_QuantumSolarPanel(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_MetsAdvancedSolarGenerator> METS_ADVANCED_SOLAR_GENERATOR =
        registerBlock("generator/block_mets_advanced_solar_generator", () -> new mio_icif_Block_MetsAdvancedSolarGenerator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_PhotonResonanceSolarGenerator> PHOTON_RESONANCE_SOLAR_GENERATOR =
        registerBlock("generator/block_photon_resonance_solar_generator", () -> new mio_icif_Block_PhotonResonanceSolarGenerator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_UltimatePhotonResonanceSolarGenerator> ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR =
        registerBlock("generator/block_ultimate_photon_resonance_solar_generator", () -> new mio_icif_Block_UltimatePhotonResonanceSolarGenerator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册量子发电机方块
    public static final DeferredBlock<mio_icif_Block_QuantumGenerator> QUANTUM_GENERATOR =
        registerBlock("generator/block_quantum_generator", () -> new mio_icif_Block_QuantumGenerator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册风力发电机方法
public static final DeferredBlock<mio_icif_Block_Wind_Generator> WIND_GENERATOR =
        registerBlock("generator/block_wind_generator", () -> new mio_icif_Block_Wind_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册水力发电机方法
public static final DeferredBlock<mio_icif_Block_Water_Generator> WATER_GENERATOR =
        registerBlock("generator/block_water_generator", () -> new mio_icif_Block_Water_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册半流质发电机方块
    public static final DeferredBlock<mio_icif_Block_Semifluid_Generator> SEMIFLUID_GENERATOR =
        registerBlock("generator/block_semifluid_generator", () -> new mio_icif_Block_Semifluid_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册地磁发电机方块
    public static final DeferredBlock<mio_icif_Block_Geomagnetic_Generator> GEOMAGNETIC_GENERATOR =
        registerBlock("generator/block_geomagnetic_generator", () -> new mio_icif_Block_Geomagnetic_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册地磁发电机天线方块
    public static final DeferredBlock<mio_icif_block_geomagnetic_antenna> GEOMAGNETIC_ANTENNA =
        registerBlock("generator/block_geomagnetic_antenna", () -> new mio_icif_block_geomagnetic_antenna(Block.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 2.5f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册地磁发电机基座方块
    public static final DeferredBlock<mio_icif_block_geomagnetic_pedestal> GEOMAGNETIC_PEDESTAL =
        registerBlock("generator/block_geomagnetic_pedestal", () -> new mio_icif_block_geomagnetic_pedestal(Block.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 2.5f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册柴油发电机方块
    public static final DeferredBlock<mio_icif_Block_Diesel_Generator> DIESEL_GENERATOR =
        registerBlock("generator/block_diesel_generator", () -> new mio_icif_Block_Diesel_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册电力发热机方块（使用 FE 产生 HU）
public static final DeferredBlock<mio_icif_block_heat_generator_elc> HEAT_GENERATOR_ELC =
        registerBlock("hugenerator/block_heat_generator_elc", () -> new mio_icif_block_heat_generator_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册固体加热机方块（燃烧燃料产生 HU）
public static final DeferredBlock<mio_icif_block_solid_heat_generator> SOLID_HEAT_GENERATOR =
        registerBlock("hugenerator/block_solid_heat_generator", () -> new mio_icif_block_solid_heat_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体加热机方块（燃烧沼气产生 HU）
public static final DeferredBlock<mio_icif_block_fluid_heat_generator> FLUID_HEAT_GENERATOR =
        registerBlock("hugenerator/block_fluid_heat_generator", () -> new mio_icif_block_fluid_heat_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册放射性同位素温差加热机方块（使用RTG靶丸产生 HU）
public static final DeferredBlock<mio_icif_block_rt_heat_generator> RT_HEAT_GENERATOR =
        registerBlock("hugenerator/block_rt_heat_generator", () -> new mio_icif_block_rt_heat_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册热交换机方块（通过热交换器将热冷却剂或岩浆转化为热能）
    public static final DeferredBlock<mio_icif_block_heat_source_fluid> HEAT_SOURCE_FLUID =
        registerBlock("hugenerator/block_heat_source_fluid", () -> new mio_icif_block_heat_source_fluid(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册斯特林发电机方块（使用 HU 产生 FE）
public static final DeferredBlock<mio_icif_block_stirling_generator> STIRLING_GENERATOR =
        registerBlock("generator/block_stirling_generator", () -> new mio_icif_block_stirling_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_drop_generator> DROP_GENERATOR =
        registerBlock("generator/block_drop_generator", () -> new mio_icif_block_drop_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_advanced_drop_generator> ADVANCED_DROP_GENERATOR =
        registerBlock("generator/block_advanced_drop_generator", () -> new mio_icif_block_advanced_drop_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_advanced_stirling_generator> ADVANCED_STIRLING_GENERATOR =
        registerBlock("generator/block_advanced_stirling_generator", () -> new mio_icif_block_advanced_stirling_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_Advanced_Semifluid_Generator> ADVANCED_SEMIFLUID_GENERATOR =
        registerBlock("generator/block_advanced_semifluid_generator", () -> new mio_icif_Block_Advanced_Semifluid_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_experience_generator> EXPERIENCE_GENERATOR =
        registerBlock("generator/block_experience_generator", () -> new mio_icif_block_experience_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_advanced_experience_generator> ADVANCED_EXPERIENCE_GENERATOR =
        registerBlock("generator/block_advanced_experience_generator", () -> new mio_icif_block_advanced_experience_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册手动动能发电机方法
public static final DeferredBlock<mio_icif_Block_Manual_Kinetic_Generator> MANUAL_KINETIC_GENERATOR =
        registerBlock("kugenerator/block_manual_kinetic_generator", () -> new mio_icif_Block_Manual_Kinetic_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册风力动能发生机方法
public static final DeferredBlock<mio_icif_Block_Wind_Kinetic_Generator> WIND_KINETIC_GENERATOR =
        registerBlock("kugenerator/block_wind_kinetic_generator", () -> new mio_icif_Block_Wind_Kinetic_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册水力动能发生机方法
public static final DeferredBlock<mio_icif_Block_Water_Kinetic_Generator> WATER_KINETIC_GENERATOR =
        registerBlock("kugenerator/block_water_kinetic_generator", () -> new mio_icif_Block_Water_Kinetic_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册电力动能机方块（使用 FE 产生 KU）
public static final DeferredBlock<mio_icif_block_Kinetic_Generator_elc> KINETIC_GENERATOR_ELC =
        registerBlock("kugenerator/block_kinetic_generator_elc", () -> new mio_icif_block_Kinetic_Generator_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册斯特林动能发生机方块（使用 HU + 水产生 KU + 热水）
public static final DeferredBlock<mio_icif_Block_Stirling_Kinetic_Generator> STIRLING_KINETIC_GENERATOR =
        registerBlock("kugenerator/block_stirling_kinetic_generator", () -> new mio_icif_Block_Stirling_Kinetic_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册动能发电机方块（将动能转换为电能）
public static final DeferredBlock<mio_icif_Block_kinetic_generator> KINETIC_GENERATOR =
        registerBlock("generator/block_kinetic_generator", () -> new mio_icif_Block_kinetic_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_turbo_kinetic_generator> TURBO_KINETIC_GENERATOR =
        registerBlock("generator/block_turbo_kinetic_generator", () -> new mio_icif_Block_turbo_kinetic_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_Block_twin_turbo_kinetic_generator> TWIN_TURBO_KINETIC_GENERATOR =
        registerBlock("generator/block_twin_turbo_kinetic_generator", () -> new mio_icif_Block_twin_turbo_kinetic_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册放射性同位素温差发电机（RTG）
    public static final DeferredBlock<mio_icif_Block_RT_Generator> RT_GENERATOR =
        registerBlock("generator/block_rt_generator", () -> new mio_icif_Block_RT_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 BatBox 方块
    public static final DeferredBlock<mio_icif_bat_box> BAT_BOX = registerBlock("wiring/block_bat_box", () -> new mio_icif_bat_box(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    
    // 注册充电座方法
public static final DeferredBlock<mio_icif_batbox_charger> BATBOX_CHARGER = registerBlock("wiring/block_batbox_charger", () -> new mio_icif_batbox_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    
    // 注册 CESU 充电座方法
public static final DeferredBlock<mio_icif_cesu_charger> CESU_CHARGER = registerBlock("wiring/block_cesu_charger", () -> new mio_icif_cesu_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    
    // 注册 MFE 充电座方法
public static final DeferredBlock<mio_icif_mfe_charger> MFE_CHARGER = registerBlock("wiring/block_mfe_charger", () -> new mio_icif_mfe_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    
    // 注册 MFSU 充电座方法
public static final DeferredBlock<mio_icif_mfsu_charger> MFSU_CHARGER = registerBlock("wiring/block_mfsu_charger", () -> new mio_icif_mfsu_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    
    // 注册 MFSU 方块
    public static final DeferredBlock<mio_icif_mfsu> MFSU = registerBlock("wiring/block_mfsu", () -> new mio_icif_mfsu(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 MFE 方块
    public static final DeferredBlock<mio_icif_mfe> MFE = registerBlock("wiring/block_mfe", () -> new mio_icif_mfe(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 CESU 方块
    public static final DeferredBlock<mio_icif_cesu> CESU = registerBlock("wiring/block_cesu", () -> new mio_icif_cesu(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 LESU 方块
    public static final DeferredBlock<mio_icif_lesu> LESU = registerBlock("wiring/block_lesu", () -> new mio_icif_lesu(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 LESU 充电座方块
    public static final DeferredBlock<mio_icif_lesu_charger> LESU_CHARGER = registerBlock("wiring/block_lesu_charger", () -> new mio_icif_lesu_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 EESU 方块
    public static final DeferredBlock<mio_icif_eesu> EESU = registerBlock("wiring/block_eesu", () -> new mio_icif_eesu(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 EESU 充电座方块
    public static final DeferredBlock<mio_icif_eesu_charger> EESU_CHARGER = registerBlock("wiring/block_eesu_charger", () -> new mio_icif_eesu_charger(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 GESU 核心方块
    public static final DeferredBlock<mio_icif_gesu_core> GESU_CORE = registerBlock("wiring/block_gesu_core", () -> new mio_icif_gesu_core(Block.Properties.of().mapColor(MapColor.METAL).strength(10.0f, 20.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 GESU 输入模块(IV)方块
    public static final DeferredBlock<mio_icif_gesu_input_iv> GESU_INPUT_IV = registerBlock("wiring/block_gesu_input_iv", () -> new mio_icif_gesu_input_iv(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 GESU 输出模块(IV)方块
    public static final DeferredBlock<mio_icif_gesu_output_iv> GESU_OUTPUT_IV = registerBlock("wiring/block_gesu_output_iv", () -> new mio_icif_gesu_output_iv(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 GESU 输出模块(LuV)方块
    public static final DeferredBlock<mio_icif_gesu_output_luv> GESU_OUTPUT_LUV = registerBlock("wiring/block_gesu_output_luv", () -> new mio_icif_gesu_output_luv(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 LV 电线方块（低压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_LV = registerBlock("wiring/cable/block_tin_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.LV));

    // 注册 MV 电线方块（中压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_MV = registerBlock("wiring/cable/block_cable_o", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.MV));

    // 注册 HV 电线方块（高压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_HV = registerBlock("wiring/cable/block_gold_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.HV));

    // 注册 EV 电线方块（超高压）
public static final DeferredBlock<mio_icif_block_wire> WIRE_EV = registerBlock("wiring/cable/block_iron_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.EV));

    // 注册 IV 电线方块（超高电压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_IV = registerBlock("wiring/cable/block_glass_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.IV));

// 注册超导电缆方块（无损、无触电伤害，使用 LuV 电压等级）
    public static final DeferredBlock<mio_icif_block_wire> SUPERCONDUCTING_CABLE = registerBlock("wiring/cable/block_superconducting_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.LuV, true));

    // 注册 LV 绝缘电线方块（低压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_ISOLATION_LV = registerBlock("wiring/cable/block_tin_cable_1", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.LV, true));

    // 注册 MV 绝缘电线方块（中压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_ISOLATION_MV = registerBlock("wiring/cable/block_cable", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.MV, true));

    // 注册 HV 绝缘电线方块（高压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_ISOLATION_HV = registerBlock("wiring/cable/block_gold_cable_1", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.HV, true));

    // 注册 EV 绝缘电线方块（超高压）
    public static final DeferredBlock<mio_icif_block_wire> WIRE_ISOLATION_EV = registerBlock("wiring/cable/block_iron_cable_1", () -> new mio_icif_block_wire(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.WOOL).noOcclusion(), CableTier.EV, true));

    // 注册 LV-MV 变压器方法
public static final DeferredBlock<mio_icif_block_transformer_lTom> TRANSFORMER_LV_MV = registerBlock("wiring/transformer_lv_mv", () -> new mio_icif_block_transformer_lTom(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 MV-HV 变压器方法
public static final DeferredBlock<mio_icif_block_transformer_mToh> TRANSFORMER_MV_HV = registerBlock("wiring/transformer_mv_hv", () -> new mio_icif_block_transformer_mToh(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 HV-EV 变压器方法
public static final DeferredBlock<mio_icif_block_transformer_hToe> TRANSFORMER_HV_EV = registerBlock("wiring/transformer_hv_ev", () -> new mio_icif_block_transformer_hToe(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册 EV-SC 变压器方法
public static final DeferredBlock<mio_icif_block_transformer_eTos> TRANSFORMER_EV_SC = registerBlock("wiring/transformer_ev_sc", () -> new mio_icif_block_transformer_eTos(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_transformer_iv> TRANSFORMER_IV_LUV = registerBlock("wiring/transformer_iv_luv", () -> new mio_icif_block_transformer_iv(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_transformer_luv> TRANSFORMER_LUV_ZPMV = registerBlock("wiring/transformer_luv_zpmv", () -> new mio_icif_block_transformer_luv(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_wireless_power_transmission_node> WIRELESS_POWER_TRANSMISSION_NODE = registerBlock("wiring/block_wireless_power_transmission_node", () -> new mio_icif_block_wireless_power_transmission_node(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wireless_power_transmission_node.LIT) ? 13 : 0)));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_detector> WIRE_DETECTOR = registerBlock("wiring/block_eu_detector_cable", () -> new com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_detector(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(net.minecraft.world.level.block.SoundType.WOOL).noOcclusion().lightLevel(state -> state.getValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_detector.ACTIVE) ? 13 : 0)));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_splitter> WIRE_SPLITTER = registerBlock("wiring/block_eu_splitter_cable", () -> new com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_splitter(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(net.minecraft.world.level.block.SoundType.WOOL).noOcclusion()));

    // 注册电压检测器方块
    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.checker.mio_icif_block_checker> CHECKER = registerBlock("checker/block_checker", () -> new com.singularity_iteration.mio_icif.Blocks.checker.mio_icif_block_checker(Block.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(mio_icif_sounds.getMachineSoundType()).noOcclusion()));

    // 注册磁化机方法
public static final DeferredBlock<mio_icif_block_magnetizer> MAGNETIZER =
        registerBlock("producer/block_magnetizer", () -> new mio_icif_block_magnetizer(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册特斯拉线圈方法
public static final DeferredBlock<mio_icif_block_tesla> TESLA =
        registerBlock("producer/block_tesla", () -> new mio_icif_block_tesla(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册作物监管机方法
public static final DeferredBlock<mio_icif_block_matron> MATRON_ELC =
        registerBlock("producer/block_matron_elc", () -> new mio_icif_block_matron(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册作物收割机方法
public static final DeferredBlock<mio_icif_block_harvest> HARVEST_ELC =
        registerBlock("producer/block_harvest_elc", () -> new mio_icif_block_harvest(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册传送机方块
    public static final DeferredBlock<mio_icif_block_teleporter_elc> TELEPORTER_ELC =
        registerBlock("producer/block_teleporter_elc", () -> new mio_icif_block_teleporter_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册基础作物架方法
public static final DeferredBlock<mio_icif_crop_stick> CROP_STICK = registerBlock("crop/stick", () -> new mio_icif_crop_stick(Block.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noCollission()));

    // 注册高级作物架方法
public static final DeferredBlock<mio_icif_crop_stick_upgraded> CROP_STICK_UPGRADED = registerBlock("crop/stick_upgraded", () -> new mio_icif_crop_stick_upgraded(Block.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noCollission()));

    // 注册杂草方块
    public static final DeferredBlock<mio_icif_block_weed> WEED_BLOCK = registerBlock("crop/weed", () -> new mio_icif_block_weed());

 // 注册电方块
    public static final DeferredBlock<mio_icif_block_furnace_elc> FURNACE_ELC = registerBlock("producer/block_furnace_elc", () -> new mio_icif_block_furnace_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册打粉机方法
public static final DeferredBlock<mio_icif_block_powder_elc> POWDER_ELC = registerBlock("producer/block_powder_elc", () -> new mio_icif_block_powder_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    public static final DeferredBlock<mio_icif_block_powder_advanced_elc> POWDER_ADVANCED_ELC = registerBlock("producer/block_powder_advanced_elc", () -> new mio_icif_block_powder_advanced_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 5.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册洗矿机方法
public static final DeferredBlock<mio_icif_block_washer_elc> WASHER_ELC = registerBlock("producer/block_washer_elc", () -> new mio_icif_block_washer_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册发酵机方法
public static final DeferredBlock<mio_icif_block_fermenter_elc> FERMENTER_ELC = registerBlock("producer/block_fermenter_elc", () -> new mio_icif_block_fermenter_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

public static final DeferredBlock<mio_icif_block_oil_refinery_elc> OIL_REFINERY_ELC = registerBlock("producer/block_oil_refinery_elc", () -> new mio_icif_block_oil_refinery_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册酒桶方块
    public static final DeferredBlock<mio_icif_block_barrel> BARREL = registerBlock("producer/block_barrel", () -> new mio_icif_block_barrel(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD)));

    // 注册高炉方块
    public static final DeferredBlock<mio_icif_block_blast_furnace> BLAST_FURNACE = registerBlock("producer/block_blast_furnace", () -> new mio_icif_block_blast_furnace(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_blast_furnace_elc> BLAST_FURNACE_ELC = registerBlock("producer/block_blast_furnace_elc", () -> new mio_icif_block_blast_furnace_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_blast_furnace_advanced> BLAST_FURNACE_ADVANCED = registerBlock("producer/block_blast_furnace_advanced", () -> new mio_icif_block_blast_furnace_advanced(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 5.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册蒸汽机方法
public static final DeferredBlock<mio_icif_block_steam_generator> STEAM_GENERATOR = registerBlock("producer/block_steam_generator", () -> new mio_icif_block_steam_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册蒸汽再加压机方块
    public static final DeferredBlock<mio_icif_block_steam_repressurizer> STEAM_REPRESSURIZER =
        registerBlock("producer/block_steam_repressurizer", () -> new mio_icif_block_steam_repressurizer(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册太阳能蒸馏机方块
    public static final DeferredBlock<mio_icif_block_solar_distiller> SOLAR_DISTILLER =
        registerBlock("producer/block_solar_distiller", () -> new mio_icif_block_solar_distiller(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册冷凝机方法
public static final DeferredBlock<mio_icif_block_condenser> CONDENSER =
        registerBlock("producer/block_condenser", () -> new mio_icif_block_condenser(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册方块切割机方法
public static final DeferredBlock<mio_icif_block_block_cutter> BLOCK_CUTTER =
        registerBlock("producer/block_block_cutter", () -> new mio_icif_block_block_cutter(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册车床方块
    public static final DeferredBlock<mio_icif_block_lathe> LATHE =
        registerBlock("producer/block_lathe", () -> new mio_icif_block_lathe(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册蒸汽动能发生机方法
public static final DeferredBlock<mio_icif_block_steam_kinetic_generator> STEAM_KINETIC_GENERATOR = registerBlock("producer/block_steam_kinetic_generator", () -> new mio_icif_block_steam_kinetic_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册热能离心机方法
public static final DeferredBlock<mio_icif_block_centrifuge_elc> CENTRIFUGE_ELC = registerBlock("producer/block_centrifuge_elc", () -> new mio_icif_block_centrifuge_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册提取机方法
public static final DeferredBlock<mio_icif_block_extrator_elc> EXTRACTOR_ELC = registerBlock("producer/block_extractor_elc", () -> new mio_icif_block_extrator_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册压缩机方法
public static final DeferredBlock<mio_icif_block_compressor_elc> COMPRESSOR_ELC = registerBlock("producer/block_compressor_elc", () -> new mio_icif_block_compressor_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    public static final DeferredBlock<mio_icif_block_compressor_advanced_elc> COMPRESSOR_ADVANCED_ELC = registerBlock("producer/block_compressor_advanced_elc", () -> new mio_icif_block_compressor_advanced_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 5.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册装罐机方法
public static final DeferredBlock<mio_icif_block_canner_elc> CANNER_ELC = registerBlock("producer/block_canner_elc", () -> new mio_icif_block_canner_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册采矿机方法
public static final DeferredBlock<mio_icif_block_miner_elc> MINER_ELC = registerBlock("producer/block_miner_elc", () -> new mio_icif_block_miner_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册高级采矿机方法
public static final DeferredBlock<mio_icif_block_advanced_miner_elc> ADVANCED_MINER_ELC = registerBlock("producer/block_advanced_miner_elc", () -> new mio_icif_block_advanced_miner_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册泵方法
public static final DeferredBlock<mio_icif_block_pump_elc> PUMP_ELC = registerBlock("producer/block_pump_elc", () -> new mio_icif_block_pump_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册电解机方法
public static final DeferredBlock<mio_icif_block_electrolyzer_elc> ELECTROLYZER = registerBlock("producer/block_electrolyzer_elc", () -> new mio_icif_block_electrolyzer_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册回收机方法
public static final DeferredBlock<mio_icif_block_recycler_elc> RECYCLER_ELC = registerBlock("producer/block_recycler_elc", () -> new mio_icif_block_recycler_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册水管道方法
public static final DeferredBlock<mio_icif_block_pipe_water> PIPE_WATER = registerBlock("pipe/block_pipe_water", () -> new mio_icif_block_pipe_water(Block.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(0.5f)
        .sound(SoundType.METAL)
        .noOcclusion()
        .isViewBlocking((state, level, pos) -> false)));

    // 注册抽水管道方块（主动从FACING方向抽取流体）
public static final DeferredBlock<mio_icif_block_pipe_water_extract> PIPE_WATER_EXTRACT = registerBlock("pipe/block_pipe_water_extract", () -> new mio_icif_block_pipe_water_extract(Block.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(0.5f)
        .sound(SoundType.METAL)
        .noOcclusion()
        .isViewBlocking((state, level, pos) -> false)));

    // 注册物流管道方块 - 输入型（从连接容器提取物品，输出给运输管道）
    public static final DeferredBlock<mio_icif_block_pipe_item> PIPE_ITEM_INPUT = registerBlock("pipe/block_pipe_item_input", () -> new mio_icif_block_pipe_item(Block.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(0.5f)
        .sound(SoundType.METAL)
        .noOcclusion()
        .isViewBlocking((state, level, pos) -> false), mio_icif_pipe_item.PipeMode.INPUT));

    // 注册物流管道方块 - 运输型（在管道之间传递物品，也可输出给容器）
    public static final DeferredBlock<mio_icif_block_pipe_item> PIPE_ITEM_TRANSPORT = registerBlock("pipe/block_pipe_item", () -> new mio_icif_block_pipe_item(Block.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(0.5f)
        .sound(SoundType.METAL)
        .noOcclusion()
        .isViewBlocking((state, level, pos) -> false), mio_icif_pipe_item.PipeMode.TRANSPORT));

    // 注册橡胶树木头（含树脂提取功能）
    public static final DeferredBlock<BlockRubberWood> BLOCK_RUBBER_TREE = registerBlock("block_rub_wood", () -> new BlockRubberWood(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).randomTicks()));

    // 注册被剥皮的橡胶木木头
    public static final DeferredBlock<BlockStrippedRubberWood> BLOCK_STRIPPED_RUBBER_WOOD = registerBlock("stripped_rubber_wood", () -> new BlockStrippedRubberWood(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<LeavesBlock> BLOCK_RUBBER_LEAF = registerBlock("block_rub_leaves", () -> new LeavesBlock(Block.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).sound(SoundType.GRASS).randomTicks().noOcclusion().isViewBlocking((state, level, pos) -> false)));

    // 有胶橡胶木 - 保留注册但不在创造模式标签页中显示
public static final DeferredBlock<mio_icif_have_rub_wood> BLOCK_HAVE_RUB_WOOD = registerBlockNoItem("block_have_rub_wood", () -> new mio_icif_have_rub_wood(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).randomTicks()));

    // 橡胶树生长器
    public static final TreeGrower RUBBER_TREE_GROWER = new TreeGrower(
        "mio_icif:rubber_tree",
        Optional.empty(),
        Optional.of(WorldGeneration.RUBBER_TREE_KEY),
        Optional.empty()
    );

    // 注册橡胶树树苗
public static final DeferredBlock<SaplingBlock> BLOCK_RUBBER_SAPLING = registerBlock("block_rub_sapling", () -> new SaplingBlock(
        RUBBER_TREE_GROWER,
        Block.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS)));

    // 注册期货机方法
public static final DeferredBlock<mio_icif_block_future_elc> FUTURE_ELC =
        registerBlock("producer/block_future_elc", () -> new mio_icif_block_future_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册感应加热机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_induction_elc> INDUCTION_ELC =
        registerBlock("producer/block_induction_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_induction_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册UU物质生成机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matter_elc> MATTER_ELC =
        registerBlock("producer/block_matter_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_matter_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_core> LARGE_FABRICATOR_CORE =
        registerBlock("producer/block_large_fabricator_core", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_core(Block.Properties.of().mapColor(MapColor.METAL).strength(10.0f, 20.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_input_iv> LARGE_FABRICATOR_INPUT_IV =
        registerBlock("producer/block_large_fabricator_input_iv", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_input_iv(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_tank> LARGE_FABRICATOR_TANK =
        registerBlock("producer/block_large_fabricator_tank", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_scrap> LARGE_FABRICATOR_SCRAP =
        registerBlock("producer/block_large_fabricator_scrap", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_scrap(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_neutron_polymerizer> NEUTRON_POLYMERIZER =
        registerBlock("producer/block_neutron_polymerizer", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_neutron_polymerizer(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_neutron_polymerizer.LIT) ? 13 : 0)));

    // 注册无限发电机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_unlimit_generator> UNLIMIT_GENERATOR =
        registerBlock("generator/block_unlimit_generator", () -> new com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_unlimit_generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops().lightLevel(state -> 15)));

    // 注册核反应堆发电机方法
public static final DeferredBlock<mio_icif_Block_Nuclear_Reactor_Generator> NUCLEAR_REACTOR_GENERATOR =
        registerBlock("generator/block_nuclear_reactor_generator", () -> new mio_icif_Block_Nuclear_Reactor_Generator(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册核反应仓方块
    public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber> REACTOR_CHAMBER =
        registerBlock("reactor/block_reactor_chamber", () -> new com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体核反应堆压力容器框架方块
    public static final DeferredBlock<mio_icif_Block_reactorvessel> REACTOR_VESSEL =
        registerBlock("reactor/block_reactor_vessel", () -> new mio_icif_Block_reactorvessel(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 1200.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体核反应堆红石端口方块
    public static final DeferredBlock<mio_icif_block_reactor_redstone_port> REACTOR_REDSTONE_PORT =
        registerBlock("reactor/block_reactor_redstone_port", () -> new mio_icif_block_reactor_redstone_port(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 1200.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体核反应堆流体端口方块
    public static final DeferredBlock<mio_icif_Block_Reactor_Fluid_Port> REACTOR_FLUID_PORT =
        registerBlock("reactor/block_reactor_fluid_port", () -> new mio_icif_Block_Reactor_Fluid_Port(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 1200.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体反应堆访问接口方法
public static final DeferredBlock<mio_icif_Block_Reactor_Access_Hatch> REACTOR_ACCESS_HATCH =
        registerBlock("reactor/block_reactor_access_hatch", () -> new mio_icif_Block_Reactor_Access_Hatch(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 1200.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册金属成型机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former> METAL_FORMER =
        registerBlock("producer/block_metal_former", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    public static final DeferredBlock<mio_icif_block_metal_former_advanced> METAL_FORMER_ADVANCED =
        registerBlock("producer/block_metal_former_advanced", () -> new mio_icif_block_metal_former_advanced(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 5.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册反应堆冷却液注入器方法
public static final DeferredBlock<mio_icif_block_redstone_reactor_coolant_injector> REDSTONE_REACTOR_COOLANT_INJECTOR =
        registerBlock("producer/block_redstone_reactor_coolant_injector", () -> new mio_icif_block_redstone_reactor_coolant_injector(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册青金石反应堆冷却液注入器方块
    public static final DeferredBlock<mio_icif_block_lapis_reactor_coolant_injector> LAPIS_REACTOR_COOLANT_INJECTOR =
        registerBlock("producer/block_lapis_reactor_coolant_injector", () -> new mio_icif_block_lapis_reactor_coolant_injector(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册地形转换机方法
public static final DeferredBlock<mio_icif_block_terra_elc> TERRA_ELC =
        registerBlock("producer/block_terra_elc", () -> new mio_icif_block_terra_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册铁栅栏 - 使用木栅栏的连接逻辑和碰撞箱
    public static final DeferredBlock<mio_icif_block_fence_iron> BLOCK_FENCE_IRON =
        registerBlock("block_fence_iron", () -> new mio_icif_block_fence_iron(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // 注册青铜方块
public static final DeferredBlock<mio_icif_block_bronze> BLOCK_BRONZE =
        registerBlock("block_bronze", () -> new mio_icif_block_bronze(Block.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册精炼铁块
    public static final DeferredBlock<Block> BLOCK_ADVIRON =
        registerBlock("block_adviron", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册防爆玻璃
    public static final DeferredBlock<mio_icif_block_alloy_glass> BLOCK_ALLOY_GLASS =
        registerBlock("build/block_alloy_glass", () -> new mio_icif_block_alloy_glass(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0f, 6000000.0f).sound(SoundType.GLASS).requiresCorrectToolForDrops().noOcclusion()));

    // 注册光辉玻璃板
    public static final DeferredBlock<mio_icif_block_irradiant_glass_pane> BLOCK_IRRADIANT_GLASS_PANE =
        registerBlock("build/block_irradiant_glass_pane", () -> new mio_icif_block_irradiant_glass_pane(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f, 3.0f).sound(SoundType.GLASS).requiresCorrectToolForDrops().noOcclusion().isValidSpawn((state, world, pos, entity) -> false).isRedstoneConductor((state, world, pos) -> false).isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false)));

    // 注册防爆石
public static final DeferredBlock<mio_icif_block_alloy_door> BLOCK_ALLOY_DOOR =
        registerBlock("build/block_alloy_door", () -> new mio_icif_block_alloy_door(net.minecraft.world.level.block.state.properties.BlockSetType.IRON, Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6000000.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // 注册工业TNT方块
    public static final DeferredBlock<mio_icif_Block_IC_TNT> IC_TNT =
        registerBlock("reactor/block_ic_tnt", () -> new mio_icif_Block_IC_TNT(Block.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.0f).sound(SoundType.GRASS).noOcclusion()));

    // 注册核弹方块
    public static final DeferredBlock<mio_icif_Block_Nuke> NUKE =
        registerBlock("reactor/block_reactor_nuke", () -> new mio_icif_Block_Nuke(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    // 注册采矿机配套方法
public static final DeferredBlock<mio_icif_block_mining_pipe> BLOCK_MINING_PIPE = registerBlock("produce/block_mining_pipe", () -> new mio_icif_block_mining_pipe(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BLOCK_MINING_TIP = registerBlock("produce/block_mining_tip", () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册辐射方块（核爆炸后留下的受污染方块）
    // 辐射等级 1: 轻度辐射
    public static final DeferredBlock<mio_icif_radioactive_block> BLOCK_RADIATING_DIRT = registerBlock("environment/block_radiating_dirt",
        () -> new mio_icif_radioactive_block(Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.DIRT).strength(0.5f, 0.5f).sound(SoundType.GRAVEL).randomTicks(), 1));

    // 辐射等级 2: 中度辐射
    public static final DeferredBlock<mio_icif_radioactive_block> BLOCK_RADIATING_STONE = registerBlock("environment/block_radiating_stone",
        () -> new mio_icif_radioactive_block(Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(2.0f, 2.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks(), 2));

    // 辐射等级 3: 重度辐射
    public static final DeferredBlock<mio_icif_radioactive_block> BLOCK_RADIATING_DEEPSLATE = registerBlock("environment/block_radiating_deepslate",
        () -> new mio_icif_radioactive_block(Block.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.DEEPSLATE).strength(3.0f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops().randomTicks(), 3));

    // 注册模式扫描机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_scanner_elc> SCANNER_ELC =
        registerBlock("producer/block_scanner_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_scanner_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册分子重组仪方块
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_molecular_transformer> MOLECULAR_TRANSFORMER =
        registerBlock("producer/block_molecular_transformer", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_molecular_transformer(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册模式存储机方块
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_pattern_storage> PATTERN_STORAGE =
        registerBlock("producer/block_pattern_storage", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_pattern_storage(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册复制机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_replicator_elc> REPLICATOR_ELC =
        registerBlock("producer/block_replicator_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_replicator_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册物品流体复合操作
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_item_buffer_elc> ITEM_BUFFER_ELC =
        registerBlock("producer/block_item_buffer_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_item_buffer_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册电动分拣机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_sorter_elc> SORTER_ELC =
        registerBlock("producer/block_sorter_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_sorter_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册高级物品分配机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_item_distributor_elc> ITEM_DISTRIBUTOR_ELC =
        registerBlock("producer/block_item_distributor_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_item_distributor_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体分配机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_distributor_elc> FLUID_DISTRIBUTOR_ELC =
        registerBlock("producer/block_fluid_distributor_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_distributor_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册高级流体分配机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_weighted_fluid_distributor_elc> WEIGHTED_FLUID_DISTRIBUTOR_ELC =
        registerBlock("producer/block_weighted_fluid_distributor_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_weighted_fluid_distributor_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册流体流量调节机方法
public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_regulator_elc> FLUID_REGULATOR_ELC =
       registerBlock("producer/block_fluid_regulator_elc", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_fluid_regulator_elc(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_batch_crafter> BATCH_CRAFTER =
        registerBlock("producer/block_batch_crafter", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_batch_crafter(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 10.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

public static final DeferredBlock<com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_chunk_loader> CHUNK_LOADER =
        registerBlock("producer/block_chunk_loader", () -> new com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_chunk_loader(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 10.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册脚手架方块（IC2风格）
// 木脚手架 - 支撑强度1，最终
public static final DeferredBlock<mio_icif_block_scaffold> SCAFFOLD_WOOD =
        registerBlock("build/block_scaffold_wood", () -> new mio_icif_block_scaffold(Block.Properties.of().mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noOcclusion().randomTicks(), 1));

    // 铁脚手架 - 支撑强度3，中等
public static final DeferredBlock<mio_icif_block_scaffold> SCAFFOLD_IRON =
        registerBlock("build/block_scaffold_iron", () -> new mio_icif_block_scaffold(Block.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.METAL).noOcclusion().randomTicks().requiresCorrectToolForDrops(), 3));

    // 钢脚手架 - 支撑强度5，较高
public static final DeferredBlock<mio_icif_block_scaffold> SCAFFOLD_STEEL =
        registerBlock("build/block_scaffold_steel", () -> new mio_icif_block_scaffold(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 3.0f).sound(SoundType.METAL).noOcclusion().randomTicks().requiresCorrectToolForDrops(), 5));

    // 碳纤维脚手架 - 支撑强度7，最终
public static final DeferredBlock<mio_icif_block_scaffold> SCAFFOLD_CARBON =
        registerBlock("build/block_scaffold_carbon", () -> new mio_icif_block_scaffold(Block.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0f, 5.0f).sound(SoundType.METAL).noOcclusion().randomTicks().requiresCorrectToolForDrops(), 7));

    // 注册建筑泡沫方块（IC2风格的CF泡沫）
    // 普通泡沫 - 在光照下逐渐硬化为石头
// IC2原版特性：泡沫没有碰撞箱，实体可以穿过
    public static final DeferredBlock<mio_icif_block_foam> CONSTRUCTION_FOAM =
        registerBlock("build/block_construction_foam", () -> new mio_icif_block_foam(Block.Properties.of().strength(0.5f).noOcclusion().randomTicks().mapColor(MapColor.SAND).sound(SoundType.WOOL), false));

    // 注册防爆石方块（IC2风格的Construction Wall）
// 强化泡沫硬化后的产物，防爆（爆炸抗性极高）
    public static final DeferredBlock<Block> CONSTRUCTION_WALL =
        registerBlock("build/block_construction_wall", () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 1200.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // 注册储物箱方块（IC2风格Storage Box）
// 木质储物箱：27组，可用斧头采集
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_WOOD =
        registerBlock("build/block_wood_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD), 27, true));
    // 青铜储物箱：45组，需扳手采集
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_BRONZE =
        registerBlock("build/block_bronze_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), 45, false) {
            @Override
            public String getStorageType() { return "bronze"; }
        });
    // 铁质储物箱：45组，需扳手采集
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_IRON =
        registerBlock("build/block_iron_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), 45, false) {
            @Override
            public String getStorageType() { return "iron"; }
        });
    // 钢制储物箱：63组，需扳手采集
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_ADVIRON =
        registerBlock("build/block_adviron_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.METAL).strength(7.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), 63, false) {
            @Override
            public String getStorageType() { return "adviron"; }
        });
    // 铱制存储箱：126组，需扳手采集 2列4行（可滚动）
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_IRIDIUM =
        registerBlock("build/block_iridium_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.DIAMOND).strength(10.0f, 1200.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), 126, false) {
            @Override
            public String getStorageType() { return "iridium"; }
        });
    // 钛制储物箱：84组（对齐METS），需扳手采集 2列4行（可滚动）
    public static final DeferredBlock<mio_icif_storage_box> STORAGE_BOX_TITANIUM =
        registerBlock("build/block_titanium_storage", () -> new mio_icif_storage_box(Block.Properties.of().mapColor(MapColor.METAL).strength(8.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), 84, false) {
            @Override
            public String getStorageType() { return "titanium"; }
        });
    // 青铜储罐：16000 mB (16桶)
    public static final DeferredBlock<mio_icif_bronze_tank> BRONZE_TANK =
        registerBlock("build/block_bronze_tank", () -> new mio_icif_bronze_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // 铁储罐：32000 mB (32桶)
    public static final DeferredBlock<mio_icif_iron_tank> IRON_TANK =
        registerBlock("build/block_iron_tank", () -> new mio_icif_iron_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // 钛储罐：64000 mB (64桶)
    public static final DeferredBlock<mio_icif_titanium_tank> TITANIUM_TANK =
        registerBlock("build/block_titanium_tank", () -> new mio_icif_titanium_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(8.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // 精炼铁(adviron)储罐：128000 mB (128桶)
    public static final DeferredBlock<mio_icif_adviron_tank> ADVIRON_TANK =
        registerBlock("build/block_adviron_tank", () -> new mio_icif_adviron_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(8.0f, 10.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // 铱储罐：1024000 mB (1024桶)
    public static final DeferredBlock<mio_icif_iridium_tank> IRIDIUM_TANK =
        registerBlock("build/block_iridium_tank", () -> new mio_icif_iridium_tank(Block.Properties.of().mapColor(MapColor.METAL).strength(10.0f, 20.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 注册工业工作台方块
public static final DeferredBlock<mio_icif_block_industrial_workbench> INDUSTRIAL_WORKBENCH =
        registerBlock("producer/block_industrial_workbench", () -> new mio_icif_block_industrial_workbench(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 3.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    // 注册能源转换器方块
    public static final DeferredBlock<mio_icif_block_energy_converter> ENERGY_CONVERTER =
        registerBlock("energy_converter/energy_converter", () -> new mio_icif_block_energy_converter(
            Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f, 3.0f)
                .sound(mio_icif_sounds.getMachineSoundType())
                .requiresCorrectToolForDrops()
        ));

    // 注册电力光源方块（不在创造标签页中显示，仅由电力光源产生器放置）
    public static final DeferredBlock<mio_icif_block_electric_light> ELECTRIC_LIGHT =
        registerBlockNoItem("block_electric_light", () -> new mio_icif_block_electric_light());

    // ========== 橡胶木系列方块 ==========
    // 橡胶木木板
    public static final DeferredBlock<mio_icif_block_rubber_planks> RUBBER_PLANKS =
        registerBlock("build/rubber_planks", () -> new mio_icif_block_rubber_planks());

    // 橡胶木楼梯
    public static final DeferredBlock<mio_icif_block_rubber_stairs> RUBBER_STAIRS =
        registerBlock("build/rubber_stairs", () -> new mio_icif_block_rubber_stairs(RUBBER_PLANKS.get().defaultBlockState()));

    // 橡胶木台阶
    public static final DeferredBlock<mio_icif_block_rubber_slab> RUBBER_SLAB =
        registerBlock("build/rubber_slab", () -> new mio_icif_block_rubber_slab());

    // 橡胶木栅栏
    public static final DeferredBlock<mio_icif_block_rubber_fence> RUBBER_FENCE =
        registerBlock("build/rubber_fence", () -> new mio_icif_block_rubber_fence());

    // 橡胶木栅栏门
    public static final DeferredBlock<mio_icif_block_rubber_fence_gate> RUBBER_FENCE_GATE =
        registerBlock("build/rubber_fence_gate", () -> new mio_icif_block_rubber_fence_gate());

    // 橡胶木门
    public static final DeferredBlock<mio_icif_block_rubber_door> RUBBER_DOOR =
        registerBlock("build/rubber_door", () -> new mio_icif_block_rubber_door());

    // 橡胶木活板门
    public static final DeferredBlock<mio_icif_block_rubber_trapdoor> RUBBER_TRAPDOOR =
        registerBlock("build/rubber_trapdoor", () -> new mio_icif_block_rubber_trapdoor());

    // 橡胶木压力板
    public static final DeferredBlock<mio_icif_block_rubber_pressure_plate> RUBBER_PRESSURE_PLATE =
        registerBlock("build/rubber_pressure_plate", () -> new mio_icif_block_rubber_pressure_plate());

    // 橡胶木按钮
    public static final DeferredBlock<mio_icif_block_rubber_button> RUBBER_BUTTON =
        registerBlock("build/rubber_button", () -> new mio_icif_block_rubber_button());

    // 橡胶木告示牌
    public static final DeferredBlock<mio_icif_block_rubber_sign> RUBBER_SIGN =
        registerBlock("build/rubber_sign", () -> new mio_icif_block_rubber_sign());

    // 橡胶木墙告示牌
    public static final DeferredBlock<mio_icif_block_rubber_wall_sign> RUBBER_WALL_SIGN =
        registerBlockNoItem("build/rubber_wall_sign", () -> new mio_icif_block_rubber_wall_sign());

    // 橡胶木悬挂告示牌
    public static final DeferredBlock<mio_icif_block_rubber_hanging_sign> RUBBER_HANGING_SIGN =
        registerBlock("build/rubber_hanging_sign", () -> new mio_icif_block_rubber_hanging_sign());

    // 橡胶木墙悬挂告示牌
    public static final DeferredBlock<mio_icif_block_rubber_wall_hanging_sign> RUBBER_WALL_HANGING_SIGN =
        registerBlockNoItem("build/rubber_wall_hanging_sign", () -> new mio_icif_block_rubber_wall_hanging_sign());

    // ========== 富集作物 ==========
    public static final DeferredBlock<IronRichCrop> CROP_IRON_RICH = registerBlock("crop/iron_rich_crop", () -> new IronRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<CopperRichCrop> CROP_COPPER_RICH = registerBlock("crop/copper_rich_crop", () -> new CopperRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<TinRichCrop> CROP_TIN_RICH = registerBlock("crop/tin_rich_crop", () -> new TinRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<TitaniumRichCrop> CROP_TITANIUM_RICH = registerBlock("crop/titanium_rich_crop", () -> new TitaniumRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<LeadRichCrop> CROP_LEAD_RICH = registerBlock("crop/lead_rich_crop", () -> new LeadRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<UraniumRichCrop> CROP_URANIUM_RICH = registerBlock("crop/uranium_rich_crop", () -> new UraniumRichCrop(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));

    // ========== 石油钻机系列方块 ==========
    public static final DeferredBlock<mio_icif_block_oil_rig_core> OIL_RIG_CORE =
        registerBlock("oilrig/block_oil_rig_core", () -> new mio_icif_block_oil_rig_core(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_oil_rig_base> OIL_RIG_BASE =
        registerBlock("oilrig/block_oil_rig_base", () -> new mio_icif_block_oil_rig_base(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_oil_rig_input> OIL_RIG_INPUT =
        registerBlock("oilrig/block_oil_rig_input", () -> new mio_icif_block_oil_rig_input(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_oil_rig_output> OIL_RIG_OUTPUT =
        registerBlock("oilrig/block_oil_rig_output", () -> new mio_icif_block_oil_rig_output(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_oil_rig_panel> OIL_RIG_PANEL =
        registerBlock("oilrig/block_oil_rig_panel", () -> new mio_icif_block_oil_rig_panel(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_titanium_drill_frame> TITANIUM_DRILL_FRAME =
        registerBlock("build/block_scaffold_titanium", () -> new mio_icif_block_titanium_drill_frame(Block.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).noOcclusion().randomTicks().requiresCorrectToolForDrops()));

    public static final DeferredBlock<mio_icif_block_dimension_oil_rig_core> DIMENSION_OIL_RIG_CORE =
        registerBlock("oilrig/block_dimension_oil_rig_core", () -> new mio_icif_block_dimension_oil_rig_core(Block.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(8.0f, 1200.0f).sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));

}