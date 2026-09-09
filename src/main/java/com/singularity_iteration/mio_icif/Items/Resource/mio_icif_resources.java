package com.singularity_iteration.mio_icif.Items.Resource;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


@SuppressWarnings("null")
public class mio_icif_resources {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 材料
    
    public static final DeferredItem<Item> ADVIRON_CASING = ITEMS.register("resource/item_adviron_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_CUTBLADE = ITEMS.register("resource/item_adviron_cutblade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_DENSEPLATE = ITEMS.register("resource/item_adviron_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_INGOT = ITEMS.register("resource/item_adviron_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_PLATE = ITEMS.register("resource/item_adviron_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_ROTOR_BLADE = ITEMS.register("resource/item_adviron_rotor_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADVIRON_SHAFT = ITEMS.register("resource/item_adviron_shaft", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALLOY_INGOT = ITEMS.register("resource/item_alloy_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALLOY_PLATE = ITEMS.register("resource/item_alloy_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ASH = ITEMS.register("resource/item_ash", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_CASING = ITEMS.register("resource/item_bronze_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_DENSEPLATE = ITEMS.register("resource/item_bronze_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_DUST = ITEMS.register("resource/item_bronze_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_DUST_SMALL = ITEMS.register("resource/item_bronze_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_PLATE = ITEMS.register("resource/item_bronze_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARBON_PLATE = ITEMS.register("resource/item_carbon_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARBON_ROTORBLADE = ITEMS.register("resource/item_carbon_rotorblade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CF_DUST = ITEMS.register("resource/item_cf_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CLAY_DUST = ITEMS.register("resource/item_clay_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_BALL = ITEMS.register("resource/item_coal_ball", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_CHUNK = ITEMS.register("resource/item_coal_chunk", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_DUST = ITEMS.register("resource/item_coal_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_FABRE = ITEMS.register("resource/item_coal_fabre", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COAL_MESH = ITEMS.register("resource/item_coal_mesh", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COFFEE_BEAN = ITEMS.register("resource/item_coffee_bean", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COFFEE_DUST = ITEMS.register("resource/item_coffee_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COIL = ITEMS.register("resource/item_coil", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_BOILER = ITEMS.register("resource/item_copper_boiler", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_CASING = ITEMS.register("resource/item_copper_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_DENSEPLATE = ITEMS.register("resource/item_copper_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.register("resource/item_copper_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_DUST_SMALL = ITEMS.register("resource/item_copper_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_ORE_CRUSHED = ITEMS.register("resource/item_copper_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_copper_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.register("resource/item_copper_plate", () -> new Item(new Item.Properties()));
    

    public static final DeferredItem<Item> CRYSTAL_MEMORY = ITEMS.register("resource/item_crystal_memory", () -> new mio_icif_memory(new Item.Properties()));
    public static final DeferredItem<Item> CRYSTAL_MEMORY_RAW = ITEMS.register("resource/item_crystal_memory_raw", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DCP_PLATE = ITEMS.register("resource/item_dcp_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIAMOND_CUT_BLADE = ITEMS.register("resource/item_diamond_cut_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIAMOND_DUST = ITEMS.register("resource/item_diamond_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENERGIUM_DUST = ITEMS.register("resource/item_energium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FERTILIZER = ITEMS.register("resource/item_fertilizer", () -> new MatronFertilizerItem(new Item.Properties()));
    public static final DeferredItem<Item> FERTILIZER_MATRON = ITEMS.register("resource/item_fertilizer_matron", () -> new MatronFertilizerItem(new Item.Properties()));
    public static final DeferredItem<Item> FUEL_COAL_CMPR = ITEMS.register("resource/item_fuel_coal_cmpr", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FUEL_COAL_DUST = ITEMS.register("resource/item_fuel_coal_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FUEL_PLANT_BALL = ITEMS.register("resource/item_fuel_plant_ball", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_CASING = ITEMS.register("resource/item_golden_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_DENSEPLATE = ITEMS.register("resource/item_golden_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_DUST = ITEMS.register("resource/item_golden_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_DUST_SMALL = ITEMS.register("resource/item_golden_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_PLATE = ITEMS.register("resource/item_golden_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_ORE_CRUSHED = ITEMS.register("resource/item_gold_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_gold_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GRIN_DUST = ITEMS.register("resource/item_grin_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HARZ = ITEMS.register("resource/item_harz", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HEATCONDUCTOR = ITEMS.register("resource/item_heatconductor", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INGOT_BRONZE = ITEMS.register("resource/item_ingot_bronze", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INGOT_TIN = ITEMS.register("resource/item_ingot_tin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRIDIUM = ITEMS.register("resource/item_iridium", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> IRIDIUM_PLATE = ITEMS.register("resource/item_iridium_plate", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> IRON_CASING = ITEMS.register("resource/item_iron_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_CUT_BLADE = ITEMS.register("resource/item_iron_cut_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_DENSEPLATE = ITEMS.register("resource/item_iron_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_DUST = ITEMS.register("resource/item_iron_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_DUST_SMALL = ITEMS.register("resource/item_iron_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_ORE_CRUSHED = ITEMS.register("resource/item_iron_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_iron_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.register("resource/item_iron_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_ROTOR_BLADE = ITEMS.register("resource/item_iron_rotor_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_SHAFT = ITEMS.register("resource/item_iron_shaft", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPI_DENSEPLATE = ITEMS.register("resource/item_lapi_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPI_DUST = ITEMS.register("resource/item_lapi_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPI_DUST_SMALL = ITEMS.register("resource/item_lapi_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPI_PLATE = ITEMS.register("resource/item_lapi_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_CASING = ITEMS.register("resource/item_lead_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_DENSEPLATE = ITEMS.register("resource/item_lead_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_DUST = ITEMS.register("resource/item_lead_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_DUST_SMALL = ITEMS.register("resource/item_lead_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register("resource/item_lead_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_ORE_CRUSHED = ITEMS.register("resource/item_lead_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_lead_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_PLATE = ITEMS.register("resource/item_lead_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LITHIUM_DUST = ITEMS.register("resource/item_lithium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LITHIUM_DUST_SMALL = ITEMS.register("resource/item_lithium_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHERRACK_DUST = ITEMS.register("resource/item_netherrack_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOTOR = ITEMS.register("resource/item_motor", () -> new Item(new Item.Properties()));
    // MOX燃料 - 放射性约为铀238的2-3倍
    public static final DeferredItem<Item> MOX = ITEMS.register("resource/item_mox", () -> new mio_icif_nuclear_material(new Item.Properties(), 2.5F));
    public static final DeferredItem<Item> MOX_PELLET = ITEMS.register("resource/item_mox_pellet", () -> new mio_icif_nuclear_material(new Item.Properties(), 2.5F));
    public static final DeferredItem<Item> OBSIDIAN_DENSEPLATE = ITEMS.register("resource/item_obsidian_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_DUST = ITEMS.register("resource/item_obsidian_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_DUST_SMALL = ITEMS.register("resource/item_obsidian_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_PLATE = ITEMS.register("resource/item_obsidian_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PELLET = ITEMS.register("resource/item_pellet", () -> new Item(new Item.Properties()));
    // 钚 - 放射性极强，约为铀238的10-15倍
    public static final DeferredItem<Item> PLUTONIUM = ITEMS.register("resource/item_plutonium", () -> new mio_icif_nuclear_material(new Item.Properties(), 15.0F));
    public static final DeferredItem<Item> PLUTONIUM_SMALL = ITEMS.register("resource/item_plutonium_small", () -> new mio_icif_nuclear_material(new Item.Properties(), 15.0F));
    // RTG燃料 - 使用钚238，放射性约为铀238的5倍
    public static final DeferredItem<Item> RTG_PELLET = ITEMS.register("resource/item_rtg_pellet", () -> new mio_icif_nuclear_material(new Item.Properties(), 5.0F));
    public static final DeferredItem<Item> RUBBER = ITEMS.register("resource/item_rubber", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHARD_IRIDIUM = ITEMS.register("resource/item_shard_iridium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILICONDIOXIDE_DUST = ITEMS.register("resource/item_silicondioxide_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_CASING = ITEMS.register("resource/item_silver_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_DUST = ITEMS.register("resource/item_silver_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_DUST_SMALL = ITEMS.register("resource/item_silver_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.register("resource/item_silver_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_ORE_CRUSHED = ITEMS.register("resource/item_silver_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_silver_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STONE_DUST = ITEMS.register("resource/item_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SULFUR_DUST = ITEMS.register("resource/item_sulfur_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SULFUR_DUST_SMALL = ITEMS.register("resource/item_sulfur_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TERRA_WART = ITEMS.register("resource/item_terra_wart", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_CASING = ITEMS.register("resource/item_tin_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_DENSEPLATE = ITEMS.register("resource/item_tin_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_DUST = ITEMS.register("resource/item_tin_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_DUST_SMALL = ITEMS.register("resource/item_tin_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_ORE_CRUSHED = ITEMS.register("resource/item_tin_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_tin_ore_crushed_purified", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_PLATE = ITEMS.register("resource/item_tin_plate", () -> new Item(new Item.Properties()));
    // 天然铀 - 放射性基准1.0（铀238为主，包含少量铀235）
    public static final DeferredItem<Item> URAN = ITEMS.register("resource/item_uran", () -> new mio_icif_nuclear_material(new Item.Properties(), 1.0F));
    // 浓缩铀235 - 放射性约为铀238的6倍（半衰期更短）
    public static final DeferredItem<Item> URAN_235 = ITEMS.register("resource/item_uran_235", () -> new mio_icif_nuclear_material(new Item.Properties(), 6.0F));
    // 铀238 - 放射性基准1.0（半衰期45亿年，放射性较弱）
    public static final DeferredItem<Item> URAN_238 = ITEMS.register("resource/item_uran_238", () -> new mio_icif_nuclear_material(new Item.Properties(), 1.0F));
    // 小撮铀235 - 放射性约为铀238的6倍
    public static final DeferredItem<Item> URAN_235_SMALL = ITEMS.register("resource/item_uran_235_small", () -> new mio_icif_nuclear_material(new Item.Properties(), 6.0F));
    // 小撮铀238 - 同上
    public static final DeferredItem<Item> URAN_238_SMALL = ITEMS.register("resource/item_uran_238_small", () -> new mio_icif_nuclear_material(new Item.Properties(), 1.0F));
    // 粉碎铀矿石 - 放射性约为纯铀的0.3倍（含杂质），手持时不会造成伤害
    public static final DeferredItem<Item> URAN_ORE_CRUSHED = ITEMS.register("resource/item_uran_ore_crushed", () -> new mio_icif_nuclear_material(new Item.Properties(), 0.3F, false));
    // 纯净的粉碎铀矿石 - 放射性约为纯铀的0.5倍，手持时不会造成伤害
    public static final DeferredItem<Item> URAN_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_uran_ore_crushed_purified", () -> new mio_icif_nuclear_material(new Item.Properties(), 0.5F, false));
    // 铀燃料 - 放射性约为铀238的1.5倍（加工后密度更高）
    public static final DeferredItem<Item> URAN_PELLET = ITEMS.register("resource/item_uran_pellet", () -> new mio_icif_nuclear_material(new Item.Properties(), 1.5F));
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.register("resource/item_raw_lead_ore", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TIN = ITEMS.register("resource/item_raw_tin_ore", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_SILVER = ITEMS.register("resource/item_raw_silver_ore", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_URAN = ITEMS.register("resource/item_raw_uran_ore", () -> new mio_icif_nuclear_material(new Item.Properties(), 0.3F, false));
    public static final DeferredItem<Item> RAW_NIOBIUM = ITEMS.register("resource/item_raw_niobium_ore", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.register("resource/item_raw_titanium_ore", () -> new Item(new Item.Properties()));

    // 铌材料
    public static final DeferredItem<Item> NIOBIUM_INGOT = ITEMS.register("resource/item_niobium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_DUST = ITEMS.register("resource/item_niobium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_DUST_SMALL = ITEMS.register("resource/item_niobium_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_PLATE = ITEMS.register("resource/item_niobium_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_DENSEPLATE = ITEMS.register("resource/item_niobium_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_CASING = ITEMS.register("resource/item_niobium_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_ORE_CRUSHED = ITEMS.register("resource/item_niobium_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_niobium_ore_crushed_purified", () -> new Item(new Item.Properties()));

    // 钛材料
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.register("resource/item_titanium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_DUST = ITEMS.register("resource/item_titanium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_DUST_SMALL = ITEMS.register("resource/item_titanium_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_PLATE = ITEMS.register("resource/item_titanium_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_DENSEPLATE = ITEMS.register("resource/item_titanium_denseplate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_CASING = ITEMS.register("resource/item_titanium_casing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_ORE_CRUSHED = ITEMS.register("resource/item_titanium_ore_crushed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_ORE_CRUSHED_PURIFIED = ITEMS.register("resource/item_titanium_ore_crushed_purified", () -> new Item(new Item.Properties()));

    // 铌钛合金材料
    public static final DeferredItem<Item> NIOBIUM_TITANIUM_INGOT = ITEMS.register("resource/item_niobium_titanium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_TITANIUM_DUST = ITEMS.register("resource/item_niobium_titanium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NIOBIUM_TITANIUM_PLATE = ITEMS.register("resource/item_niobium_titanium_plate", () -> new Item(new Item.Properties()));

    // 钍材料
    public static final DeferredItem<Item> THORIUM_DUST = ITEMS.register("resource/item_thorium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> THORIUM_DUST_SMALL = ITEMS.register("resource/item_thorium_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> THORIUM_SCRAP = ITEMS.register("normal/item_thorium_scrapbox", () -> new com.singularity_iteration.mio_icif.Items.Normal.mio_icif_thorium_scrapbox(new Item.Properties()));

    // 金属粒
    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.register("resource/item_copper_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.register("resource/item_tin_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TITANIUM_NUGGET = ITEMS.register("resource/item_titanium_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_NUGGET = ITEMS.register("resource/item_lead_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_NUGGET = ITEMS.register("resource/item_silver_nugget", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WOOD_ROTOR_BLADE = ITEMS.register("resource/item_wood_rotor_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZIP_COAL_BLOCK = ITEMS.register("resource/item_zip_coal_block", () -> new Item(new Item.Properties()));

    // 新增粉末物品
    public static final DeferredItem<Item> FLINT_DUST = ITEMS.register("resource/item_flint_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHARCOAL_DUST = ITEMS.register("resource/item_charcoal_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NITRE_DUST = ITEMS.register("resource/item_nitre_dust", () -> new Item(new Item.Properties()));

    // ASP - Advanced Solar Panels 物品
    public static final DeferredItem<Item> SUNNARIUM = ITEMS.register("resource/item_sunnarium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SUNNARIUM_ALLOY = ITEMS.register("resource/item_sunnarium_alloy", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SUNNARIUM_SMALL = ITEMS.register("resource/item_sunnarium_small", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENRICHED_SUNNARIUM = ITEMS.register("resource/item_enriched_sunnarium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENRICHED_SUNNARIUM_ALLOY = ITEMS.register("resource/item_enriched_sunnarium_alloy", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRRADIANT_URANIUM_INGOT = ITEMS.register("resource/item_irradiant_uranium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRRADIANT_REINFORCED_PLATE = ITEMS.register("resource/item_irradiant_reinforced_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRIDIUMIRON_PLATE = ITEMS.register("resource/item_iridiumiron_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRIDIUMIRON_REINFORCED_PLATE = ITEMS.register("resource/item_iridiumiron_reinforced_plate", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRIDIUM_INGOT = ITEMS.register("resource/item_iridium_ingot", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.register("resource/item_uranium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUANTUM_CORE = ITEMS.register("resource/item_quantum_core", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> MOLECULAR_TRANSFORMER_CORE = ITEMS.register("resource/item_molecular_transformer_core", () -> new Item(new Item.Properties()));

    // 钛柄
    public static final DeferredItem<Item> METS_TITANIUM_SHAFT = ITEMS.register("resource/item_titanium_shaft", () -> new Item(new Item.Properties()));
    
    
    // 超级铱合金锭
    public static final DeferredItem<Item> METS_SUPER_IRIDIUM_ALLOY = ITEMS.register("resource/item_super_iridium_alloy", () -> new Item(new Item.Properties()));
    // 致密超级铱合金板
    public static final DeferredItem<Item> METS_SUPER_IRIDIUM_COMPRESS_PLATE = ITEMS.register("resource/item_super_iridium_compress_plate", () -> new Item(new Item.Properties()));
    // 植物精华
    public static final DeferredItem<Item> METS_PLANT_EXTRACT = ITEMS.register("resource/item_plant_extract", () -> new Item(new Item.Properties()));
    // 纳米活体金属
    public static final DeferredItem<Item> METS_NANO_LIVING_METAL = ITEMS.register("resource/item_nano_living_metal", () -> new Item(new Item.Properties()));

    // 简并态中子板
    public static final DeferredItem<Item> METS_NEUTRON_PLATE = ITEMS.register("resource/item_neutron_plate", () -> new Item(new Item.Properties()));

    // 钛铁合金扇叶
    public static final DeferredItem<Item> TITANIUM_IRON_ROTOR_BLADE = ITEMS.register("resource/item_titanium_iron_alloy_rotor_blade", () -> new Item(new Item.Properties()));
    // 超级铱合金扇叶
    public static final DeferredItem<Item> SUPER_IRIDIUM_ROTOR_BLADE = ITEMS.register("resource/item_super_iridium_alloy_rotor_blade", () -> new Item(new Item.Properties()));

    //CarryEvent
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}