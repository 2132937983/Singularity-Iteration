package com.singularity_iteration.mio_icif.Items.Tools;


import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.mio_icif_items;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_items_tools {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // BronzePickaxe
    public static final DeferredItem<Item> TOOL_BRONZE_PICKAXE = ITEMS.register("item_tool_bronze_pickaxe", () -> new PickaxeItem(mio_icif_items.Bronze_Tier.BRONZE, new Item.Properties().attributes(PickaxeItem.createAttributes(mio_icif_items.Bronze_Tier.BRONZE, 1.0F, -2.8F))));
    // BronzeAxe
    public static final DeferredItem<Item> TOOL_BRONZE_AXE = ITEMS.register("item_tool_bronze_axe", () -> new AxeItem(mio_icif_items.Bronze_Tier.BRONZE, new Item.Properties().attributes(AxeItem.createAttributes(mio_icif_items.Bronze_Tier.BRONZE, 7.0F, -3.2F))));

    //BronzeShovel
    public static final DeferredItem<Item> TOOL_BRONZE_SHOVEL = ITEMS.register("item_tool_bronze_shovel", () -> new ShovelItem(mio_icif_items.Bronze_Tier.BRONZE, new Item.Properties().attributes(ShovelItem.createAttributes(mio_icif_items.Bronze_Tier.BRONZE, 1.5F, -3.0F))));
    //BronzeHoe
    public static final DeferredItem<Item> TOOL_BRONZE_HOE = ITEMS.register("item_tool_bronze_hoe", () -> new HoeItem(mio_icif_items.Bronze_Tier.BRONZE, new Item.Properties().attributes(HoeItem.createAttributes(mio_icif_items.Bronze_Tier.BRONZE, 0.0F, -3.0F))));

    //Hammer
    public static final DeferredItem<Item> TOOL_HAMMER = ITEMS.register("item_tool_hammer",() -> new mio_icif_hammer(new Item.Properties()));

    // Cutter
    public static final DeferredItem<Item> TOOL_CUTTER = ITEMS.register("item_tool_cutter",() -> new mio_icif_cutter(new Item.Properties()));

    // TreeTap
    public static final DeferredItem<Item> TOOL_TREE_TAP = ITEMS.register("item_tool_wooden_treetap",() -> new mio_icif_treetap(new Item.Properties().durability(16)));

    // MinerLaserGun
    public static final DeferredItem<Item> TOOL_LASER_MINER = ITEMS.register("item_tool_laser_miner",() -> new mio_icif_laser_miner(new Item.Properties()));

    // Power Unit
    public static final DeferredItem<Item> POWER_UNIT = ITEMS.register("item_tool_power_unit",() -> new mio_icif_power_unit(new Item.Properties()));

    // Power Unit Small
    public static final DeferredItem<Item> POWER_UNIT_SMALL = ITEMS.register("item_tool_power_unit_small",() -> new mio_icif_power_unit_small(new Item.Properties()));

    // Iron Driller
    public static final DeferredItem<Item> IRON_DRILLER = ITEMS.register("item_tool_iron_driller",() -> new mio_icif_iron_driller(new Item.Properties()));

    // Diamond Driller
    public static final DeferredItem<Item> DIAMOND_DRILLER = ITEMS.register("item_tool_diamond_driller",() -> new mio_icif_diamond_driller(new Item.Properties()));

    // Iridium Driller
    public static final DeferredItem<Item> IRIDIUM_DRILLER = ITEMS.register("item_tool_iridium_driller",() -> new mio_icif_iridium_driller(new Item.Properties()));

    // Iron Chainsaw
    public static final DeferredItem<Item> IRON_CHAINSAW = ITEMS.register("item_tool_iron_chainsaw",() -> new mio_icif_iron_chainsaw(new Item.Properties()));

    // Wrench
    public static final DeferredItem<Item> WRENCH = ITEMS.register("item_tool_wrench",() -> new mio_icif_wrench(new Item.Properties()));

    // Electric Wrench
    public static final DeferredItem<Item> WRENCH_ELC = ITEMS.register("item_tool_wrench_elc",() -> new mio_icif_wrench_elc(new Item.Properties()));

    // Electric TreeTap
    public static final DeferredItem<Item> TREETAP_ELC = ITEMS.register("item_tool_treetap_elc",() -> new mio_icif_treetap_elc(new Item.Properties()));

    // OD Scanner
    public static final DeferredItem<Item> OD_SCANNER = ITEMS.register("item_tool_od_scanner",() -> new mio_icif_od_scanner(new Item.Properties()));

    // OV Scanner
    public static final DeferredItem<Item> OV_SCANNER = ITEMS.register("item_tool_ov_scanner",() -> new mio_icif_ov_scanner(new Item.Properties()));

    // Nano Saber
    public static final DeferredItem<Item> NANO_SABER = ITEMS.register("item_tool_nanosaber",() -> new mio_icif_nanosaber(new Item.Properties()));

    // Plasma Launcher
    public static final DeferredItem<Item> PLASMA_LAUNCHER = ITEMS.register("item_tool_plasma_launcher",() -> new mio_icif_plasma_launcher(new Item.Properties()));

    // METS 移植武器

    // 量子剑
    public static final DeferredItem<Item> QUANTUM_SWORD = ITEMS.register("item_tool_quantum_sword", () -> new mio_icif_quantum_sword(new Item.Properties()));

    // 电力激光镭射步枪
    public static final DeferredItem<Item> ELECTRIC_RIFLE = ITEMS.register("item_tool_electric_rifle", () -> new mio_icif_electric_rifle(new Item.Properties()));

    // 进阶电力激光镭射步枪
    public static final DeferredItem<Item> ADVANCED_ELECTRIC_RIFLE = ITEMS.register("item_tool_advanced_electric_rifle", () -> new mio_icif_advanced_electric_rifle(new Item.Properties()));

    // 铱制刺刀战术激光镭射步枪
    public static final DeferredItem<Item> TACTICAL_LASER_RIFLE = ITEMS.register("item_tool_tactical_laser_rifle", () -> new mio_icif_tactical_laser_rifle(new Item.Properties()));

    // 等离子空气炮
    public static final DeferredItem<Item> PLASMA_AIR_CANNON = ITEMS.register("item_tool_plasma_air_cannon", () -> new mio_icif_plasma_air_cannon(new Item.Properties()));

    // 电力等离子电浆发射器
    public static final DeferredItem<Item> ELECTRIC_PLASMA_GUN = ITEMS.register("item_tool_electric_plasma_gun", () -> new mio_icif_electric_plasma_gun(new Item.Properties()));

    // 先进速子裂解枪
    public static final DeferredItem<Item> TACHYON_DISRUPTOR = ITEMS.register("item_tool_tachyon_disruptor", () -> new mio_icif_tachyon_disruptor(new Item.Properties()));

    // 钢制防爆盾
    public static final DeferredItem<Item> STEEL_SHIELD = ITEMS.register("item_tool_steel_shield", () -> new mio_icif_steel_shield(new Item.Properties()));

    // 纳米复合弓
    public static final DeferredItem<Item> NANO_BOW = ITEMS.register("item_tool_nano_bow", () -> new mio_icif_nano_bow(new Item.Properties()));

    // 火箭筒
    public static final DeferredItem<Item> ROCKET_LAUNCHER = ITEMS.register("item_tool_rocket_launcher", () -> new mio_icif_rocket_launcher(new Item.Properties()));

    // 火箭弹
    public static final DeferredItem<Item> ROCKET = ITEMS.register("item_rocket", () -> new mio_icif_rocket(new Item.Properties().stacksTo(64)));

    // EU Meter
    public static final DeferredItem<Item> EU_METER = ITEMS.register("item_tool_meter",() -> new mio_icif_eu_meter(new Item.Properties()));

    // Windmeter
    public static final DeferredItem<Item> WINDMETER = ITEMS.register("item_tool_windmeter",() -> new mio_icif_windmeter(new Item.Properties()));

    // METS 移植电力工具

    // 电力营养供应器
    public static final DeferredItem<Item> ELECTRIC_NUTRITION_SUPPLY = ITEMS.register("item_electric_nutrition_supply", () -> new mio_icif_electric_nutrition_supply());

    // 电力生命维护仪
    public static final DeferredItem<Item> ELECTRIC_FIRST_AID_LIFE_SUPPORT = ITEMS.register("item_electric_first_aid_life_support", () -> new mio_icif_electric_first_aid_life_support());

    // 电力伤害吸收仪
    public static final DeferredItem<Item> ELECTRIC_FORCE_FIELD_GENERATOR = ITEMS.register("item_electric_force_field_generator", () -> new mio_icif_electric_force_field_generator());

    // 电动鱼竿
    public static final DeferredItem<Item> ELECTRIC_FISHING_ROD = ITEMS.register("item_electric_fishing_rod", () -> new mio_icif_electric_fishing_rod());

    // 电力光源产生器
    public static final DeferredItem<Item> ELECTRIC_LIGHTER = ITEMS.register("item_electric_lighter", () -> new mio_icif_electric_lighter());

    // 无线管理器
    public static final DeferredItem<Item> ELECTRIC_WIRELESS_MANAGER = ITEMS.register("item_electric_wireless_manager", () -> new mio_icif_electric_wireless_manager());

    // 地磁探测器
    public static final DeferredItem<Item> GEOMAGNETIC_DETECTOR = ITEMS.register("item_geomagnetic_detector", () -> new mio_icif_geomagnetic_detector());

    //CarryEvent
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


}
