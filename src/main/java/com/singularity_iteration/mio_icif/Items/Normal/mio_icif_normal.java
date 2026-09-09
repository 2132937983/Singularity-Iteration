package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_solar_helmet;
import com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem;
import com.singularity_iteration.mio_icif.Items.Lathe.LatheTurningBlanksItem;
import com.singularity_iteration.mio_icif.Items.Build.CFSprayerItem;
import com.singularity_iteration.mio_icif.Items.Build.ObscuratorItem;
import com.singularity_iteration.mio_icif.Items.Build.PainterItem;
import com.singularity_iteration.mio_icif.Items.Tools.CropAnalyzerItem;
import com.singularity_iteration.mio_icif.Items.Tools.WeedingTrowelItem;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_frequency_transmitter;
import com.singularity_iteration.mio_icif.Items.Crop.Enriched.RichSeedItem;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.entity.boat.mio_icif_boat_item;
import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_dynamite_item;
import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_remote_item;
import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_sticky_dynamite_item;
import com.singularity_iteration.mio_icif.entity.mio_icif_entities;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


@SuppressWarnings("null")
public class mio_icif_normal {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 配件与其他

    // CF喷枪 - 建筑泡沫喷枪
    public static final DeferredItem<CFSprayerItem> CF_SPRAYER = ITEMS.register("build/item_cf_sprayer", () -> new CFSprayerItem(new Item.Properties()));

    // 遮蔽器
    public static final DeferredItem<ObscuratorItem> OBSCURATOR = ITEMS.register("build/item_obscurator", () -> new ObscuratorItem(new Item.Properties()));

    // 刷子（基础白色刷子）
    public static final DeferredItem<PainterItem> PAINTER = ITEMS.register("build/item_painter", () -> new PainterItem(PainterItem.PainterColor.WHITE, new Item.Properties()));

    // 刷子 - 16种颜色（IC2风格建筑工具）
    public static final DeferredItem<PainterItem> PAINTER_WHITE = ITEMS.register("build/item_painter_white", () -> new PainterItem(PainterItem.PainterColor.WHITE, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_ORANGE = ITEMS.register("build/item_painter_orange", () -> new PainterItem(PainterItem.PainterColor.ORANGE, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_MAGENTA = ITEMS.register("build/item_painter_magenta", () -> new PainterItem(PainterItem.PainterColor.MAGENTA, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_LIGHT_BLUE = ITEMS.register("build/item_painter_light_blue", () -> new PainterItem(PainterItem.PainterColor.LIGHT_BLUE, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_YELLOW = ITEMS.register("build/item_painter_yellow", () -> new PainterItem(PainterItem.PainterColor.YELLOW, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_LIME = ITEMS.register("build/item_painter_lime", () -> new PainterItem(PainterItem.PainterColor.LIME, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_PINK = ITEMS.register("build/item_painter_pink", () -> new PainterItem(PainterItem.PainterColor.PINK, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_GRAY = ITEMS.register("build/item_painter_gray", () -> new PainterItem(PainterItem.PainterColor.GRAY, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_LIGHT_GRAY = ITEMS.register("build/item_painter_light_gray", () -> new PainterItem(PainterItem.PainterColor.LIGHT_GRAY, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_CYAN = ITEMS.register("build/item_painter_cyan", () -> new PainterItem(PainterItem.PainterColor.CYAN, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_PURPLE = ITEMS.register("build/item_painter_purple", () -> new PainterItem(PainterItem.PainterColor.PURPLE, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_BLUE = ITEMS.register("build/item_painter_blue", () -> new PainterItem(PainterItem.PainterColor.BLUE, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_BROWN = ITEMS.register("build/item_painter_brown", () -> new PainterItem(PainterItem.PainterColor.BROWN, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_GREEN = ITEMS.register("build/item_painter_green", () -> new PainterItem(PainterItem.PainterColor.GREEN, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_RED = ITEMS.register("build/item_painter_red", () -> new PainterItem(PainterItem.PainterColor.RED, new Item.Properties()));
    public static final DeferredItem<PainterItem> PAINTER_BLACK = ITEMS.register("build/item_painter_black", () -> new PainterItem(PainterItem.PainterColor.BLACK, new Item.Properties()));

    public static final DeferredItem<Item> CROP_UNKNOWN = ITEMS.register("normal/crop_unknown", () -> new Item(new Item.Properties()));
    public static final DeferredItem<mio_icif_boat_item> ENTITY_COAL_BOAT = ITEMS.register("normal/entity_coal_boat", () -> new mio_icif_boat_item(new Item.Properties(), () -> mio_icif_entities.CARBON_BOAT.get()));
    public static final DeferredItem<mio_icif_boat_item> ENTITY_ELECTRIC_BOAT = ITEMS.register("normal/entity_electric_boat", () -> new mio_icif_boat_item(new Item.Properties(), () -> mio_icif_entities.ELECTRIC_BOAT.get()));
    public static final DeferredItem<mio_icif_boat_item> ENTITY_RUBBER_BOAT = ITEMS.register("normal/entity_rubber_boat", () -> new mio_icif_boat_item(new Item.Properties(), () -> mio_icif_entities.RUBBER_BOAT.get()));

    // 二级电池 (高级RE电池 - 对应原版IC2 advanced_re_battery: 100000EU, 256EU/t, Tier 2, 可堆叠)

    public static final DeferredItem<Item> ADVBAT_LEV0 = ITEMS.register("normal/item_advbat_lev0", () -> new mio_icif_bat(new Item.Properties(), 100000, 100000, "item_advbat", 256, 16)); // 空电状态（0FE，最低）

    // 四级电池 (高级充电RE电池 - 对应原版IC2 advanced_charging_re_battery: 400000EU, 1024EU/t, Tier 2, 不可堆叠)
    public static final DeferredItem<Item> ADVCHARGEBAT_0 = ITEMS.register("normal/item_advchargebat_lev0", () -> new mio_icif_bat(new Item.Properties(), 400000, 400000, "item_advchargebat_lev0", 1024)); // 空电状态（0FE，最低）

    public static final DeferredItem<Item> ADCIRCUIT = ITEMS.register("normal/item_advcircuit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALE = ITEMS.register("normal/item_ale", () -> new BoozeItem(BoozeItem.BoozeType.ALE, new Item.Properties()));
    public static final DeferredItem<Item> BARREL = ITEMS.register("normal/item_barrel", () -> new Item(new Item.Properties()));
    

    
    public static final DeferredItem<Item> BAT_LEV0 = ITEMS.register("normal/item_bat_lev0", () -> new mio_icif_bat(new Item.Properties(), 10000, 10000, "item_bat", 100, 16)); // 空电状态（0FE，最低）
    public static final DeferredItem<Item> BEER = ITEMS.register("normal/item_beer", () -> new BoozeItem(BoozeItem.BoozeType.BEER, new Item.Properties()));
    public static final DeferredItem<Item> BIOCHAFF = ITEMS.register("normal/item_biochaff", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLACK_CELL = ITEMS.register("normal/item_black_cell", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BREW = ITEMS.register("normal/item_brew", () -> new BoozeItem(BoozeItem.BoozeType.BREW, new Item.Properties()));
    public static final DeferredItem<Item> BROKEN_RUBBER_BOAT = ITEMS.register("normal/item_broken_rubber_boat", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAN = ITEMS.register("normal/item_can", () -> new Item(new Item.Properties()));

    // 三级电池 (充电RE电池 - 对应原版IC2 charging_re_battery: 40000EU, 128EU/t, Tier 1, 不可堆叠)

    public static final DeferredItem<Item> CHARGEBAT_LEV0 = ITEMS.register("normal/item_chargebat_lev0", () -> new mio_icif_bat(new Item.Properties(), 40000, 40000, "item_chargebat", 128)); // 空电状态（0FE，最低）
    
    public static final DeferredItem<Item> CIRCUIT = ITEMS.register("normal/item_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<MugDrinkItem> COFFEE_0 = ITEMS.register("normal/item_coffee_0", () -> new MugDrinkItem(MugDrinkItem.MugType.COLD_COFFEE, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<MugDrinkItem> COFFEE_1 = ITEMS.register("normal/item_coffee_1", () -> new MugDrinkItem(MugDrinkItem.MugType.DARK_COFFEE, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<MugDrinkItem> COFFEE_2 = ITEMS.register("normal/item_coffee_2", () -> new MugDrinkItem(MugDrinkItem.MugType.COFFEE, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> COIN = ITEMS.register("normal/item_coin", () -> new mio_icif_coin(new Item.Properties()));
    public static final DeferredItem<Item> CONTAIN_MENT_BOX = ITEMS.register("normal/item_contain_ment_box", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CROP_ANALYZER = ITEMS.register("normal/item_crop_analyzer", () -> new CropAnalyzerItem(new Item.Properties()));

    // 作物种子 - 通用种子，通过NBT数据区分植物类型
    // 空种子袋就是没有NBT数据的作物种子
    public static final DeferredItem<Item> CROP_SEED = ITEMS.register("normal/item_crop_seed", () -> new CropSeedItem(new Item.Properties()));

    // 五级电池 (充电能量水晶 - 对应原版IC2 charging_energy_crystal: 4000000EU, 8192EU/t, Tier 3, 不可堆叠)
    public static final DeferredItem<Item> CRYSTAL_CHARGEBAT_LEV0 = ITEMS.register("normal/item_crystal_chargebat_lev0", () -> new mio_icif_bat(new Item.Properties(), 4000000, 4000000, "item_crystal_chargebat", 8192)); // 空电状态（0FE，最低）

    // 能量水晶 (对应原版IC2 energy_crystal: 1000000EU, 2048EU/t, Tier 3, 可堆叠)
    public static final DeferredItem<Item> CRYSTAL_LEV0 = ITEMS.register("normal/item_crystal_lev0", () -> new mio_icif_bat(new Item.Properties(), 1000000, 1000000, "normal/item_crystal", 2048, 16)); // 空电状态（0FE，最低）


    public static final DeferredItem<Item> DEBUG = ITEMS.register("normal/item_debug", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGONBLOOD = ITEMS.register("normal/item_dragonblood", () -> new Item(new Item.Properties()));
    public static final DeferredItem<mio_icif_dynamite_item> DYNAMITE = ITEMS.register("normal/item_dynamite", () -> new mio_icif_dynamite_item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> EMPTY_CELL_URAN = ITEMS.register("normal/item_empty_cell_uran", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMPTY_FUEL_CAN = ITEMS.register("normal/item_empty_fuel_can", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMPTY_MUG = ITEMS.register("normal/item_empty_mug", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FREQ = ITEMS.register("normal/item_freq", () -> new mio_icif_frequency_transmitter(new Item.Properties()));
    public static final DeferredItem<Item> FUEL_CAN = ITEMS.register("normal/item_fuel_can", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FUEL_ROD = ITEMS.register("normal/item_fuel_rod", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HOPS = ITEMS.register("normal/item_hops", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HYDRANT_CELL = ITEMS.register("normal/item_hydrant_cell", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INDUSTRIAL_DIAMOND = ITEMS.register("normal/item_industrial_diamond", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INTELLIGENCE = ITEMS.register("normal/item_intelligence", () -> new mio_icif_intelligence(new Item.Properties()));
    public static final DeferredItem<Item> INVALID_CROP = ITEMS.register("normal/item_invalid_crop", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MODIFY = ITEMS.register("normal/item_modify", () -> new mio_icif_modify(new Item.Properties()));
    public static final DeferredItem<LatheTurningBlanksItem> IRON_TURNING_BLANKS = ITEMS.register("normal/item_iron_turning_blanks", () -> new LatheTurningBlanksItem(LatheTurningBlanksItem.LatheMaterial.IRON));

    public static final DeferredItem<Item> LAPOTRON_CRYSTAL_LEV0 = ITEMS.register("normal/item_lapotron_crystal_lev0", () -> new mio_icif_bat(new Item.Properties(), 10000000, 10000000, "item_lapotron_crystal", 8192, 16)); // 空电状态（0FE，最低）

    // 六级电池 (充电蓝波顿水晶)

    public static final DeferredItem<Item> LAMACRYSTAL_CHARGEBAT_LEV0 = ITEMS.register("normal/item_lamacrystal_chargebat_lev0", () -> new mio_icif_bat(new Item.Properties(), 40000000, 40000000, "item_lamacrystal_chargebat", 32768)); // 空电状态（0FE，最低）


    public static final DeferredItem<Item> MATTER = ITEMS.register("normal/item_matter", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOX_QUAD_DEPLETE = ITEMS.register("normal/item_mox_quad_deplete", () -> new Item(new Item.Properties()));
    public static final DeferredItem<MugDrinkItem> RED_MUG = ITEMS.register("normal/item_red_mug", () -> new MugDrinkItem(MugDrinkItem.MugType.BLACK_TEA, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<mio_icif_remote_item> REMOTE = ITEMS.register("normal/item_remote", () -> new mio_icif_remote_item(new Item.Properties()));
    public static final DeferredItem<Item> RUM = ITEMS.register("normal/item_rum", () -> new BoozeItem(BoozeItem.BoozeType.RUM, new Item.Properties()));
    public static final DeferredItem<Item> SCRAP = ITEMS.register("normal/item_scrap", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SCRAPBOX = ITEMS.register("normal/item_scrapbox", () -> new mio_icif_scrapbox(new Item.Properties()));
    public static final DeferredItem<Item> SLAG = ITEMS.register("normal/item_slag", () -> new Item(new Item.Properties()));
    public static final DeferredItem<mio_icif_solar_helmet> SOLAR_HELMET = ITEMS.register("normal/item_solar_helmet", () -> new mio_icif_solar_helmet(net.minecraft.world.item.ArmorMaterials.LEATHER, new Item.Properties()));
    public static final DeferredItem<Item> STATIC_BOOTS = ITEMS.register("normal/item_static_boots", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEAM_TURBIN = ITEMS.register("normal/item_steam_turbin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEAM_TURBINE_BLADE = ITEMS.register("normal/item_steam_turbine_blade", () -> new Item(new Item.Properties()));
    public static final DeferredItem<mio_icif_sticky_dynamite_item> STICKY_DYNAMITE = ITEMS.register("normal/item_sticky_dynamite", () -> new mio_icif_sticky_dynamite_item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> STUFF = ITEMS.register("normal/item_stuff", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SU_BAT = ITEMS.register("normal/item_su_bat", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_EMPTY_CAN = ITEMS.register("normal/item_tin_empty_can", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_FILLED_CAN = ITEMS.register("normal/item_tin_filled_can", () -> new Item(new Item.Properties()
        .food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(1)  // 半个鸡腿 = 1点饥饿值
            .saturationModifier(0.5f)  // 饱和度系数
            .build())));
    public static final DeferredItem<Item> TOOL_MFSU_UPGRADE_KIT = ITEMS.register("normal/item_tool_mfsu_upgrade_kit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOOLBOX = ITEMS.register("normal/item_toolbox", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOOLBOX_X = ITEMS.register("normal/item_toolbox_x", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TRITIUM_CELL = ITEMS.register("normal/item_tritium_cell", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> URAN_ENRICHED_CELL = ITEMS.register("normal/item_uran_enriched_cell", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WEED = ITEMS.register("normal/item_weed", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WEEDEX = ITEMS.register("normal/item_weedex", () -> new MatronHerbicideItem(new Item.Properties()));
    public static final DeferredItem<Item> HERBICIDE_MATRON = ITEMS.register("normal/item_herbicide_matron", () -> new MatronHerbicideItem(new Item.Properties()));
    public static final DeferredItem<Item> WEEDING_TROWEL = ITEMS.register("normal/item_weeding_trowel", () -> new WeedingTrowelItem(new Item.Properties()));
    public static final DeferredItem<LatheTurningBlanksItem> WOODEN_TURNING_BLANKS = ITEMS.register("normal/item_wooden_turning_blanks", () -> new LatheTurningBlanksItem(LatheTurningBlanksItem.LatheMaterial.WOOD));
    public static final DeferredItem<Item> YOUNGSTER = ITEMS.register("normal/item_youngster", () -> new BoozeItem(BoozeItem.BoozeType.YOUNGSTER, new Item.Properties()));

    // METS 移植材料
    public static final DeferredItem<Item> METS_SUPER_CIRCUIT = ITEMS.register("normal/item_super_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> METS_LIVING_CIRCUIT = ITEMS.register("normal/item_living_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> METS_LENS = ITEMS.register("normal/item_lens", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> METS_DIAMOND_LENS = ITEMS.register("normal/item_diamond_lens", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> METS_FIELD_GENERATOR = ITEMS.register("normal/item_field_generator", () -> new Item(new Item.Properties()));

    // METS 移植电池
    // 超级兰波顿水晶 - 100,000,000 EU, 8192 EU/t, Tier 5, 不可堆叠
    public static final DeferredItem<Item> SUPER_LAPOTRON_CRYSTAL = ITEMS.register("normal/item_super_lapotron_crystal", () -> new mio_icif_bat(new Item.Properties(), 100000000, 100000000, "item_super_lapotron_crystal", 8192));
    // 充电超级兰波顿水晶 - 400,000,000 EU, 8192 EU/t, Tier 5, 不可堆叠
    public static final DeferredItem<Item> CHARGING_SUPER_LAPOTRON_CRYSTAL = ITEMS.register("normal/item_charging_super_lapotron_crystal", () -> new mio_icif_bat(new Item.Properties(), 400000000, 400000000, "item_charging_super_lapotron_crystal", 8192));
    // 锂电池 - 50,000 EU, 128 EU/t, Tier 2, 可堆叠16
    public static final DeferredItem<Item> LITHIUM_BATTERY = ITEMS.register("normal/item_lithium_battery", () -> new mio_icif_bat(new Item.Properties(), 50000, 50000, "item_lithium_battery", 128, 16));
    // 高级锂电池 - 200,000 EU, 128 EU/t, Tier 2, 可堆叠16
    public static final DeferredItem<Item> ADV_LITHIUM_BATTERY = ITEMS.register("normal/item_adv_lithium_battery", () -> new mio_icif_bat(new Item.Properties(), 200000, 200000, "item_adv_lithium_battery", 128, 16));
    // 钍电池 - Integer.MAX_VALUE EU, 9 EU/t, 自动充电
    public static final DeferredItem<Item> THORIUM_BATTERY = ITEMS.register("normal/item_thorium_battery", () -> new ThoriumBatteryItem());
    // API 测试电池 - 纯 IBatteryItem 接口实现的超级蓝波顿水晶
    public static final DeferredItem<Item> SUPER_LAPOTRON_CRYSTAL_API = ITEMS.register("normal/item_super_lapotron_crystal_api", () -> new SuperLapotronCrystalAPI(new Item.Properties()));

    // 富集作物种子 (支持原版耕地和IC2作物架两种种植方式)
    public static final DeferredItem<Item> SEED_IRON_RICH = ITEMS.register("crop/iron_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_IRON_RICH.get(), "ferru"));
    public static final DeferredItem<Item> SEED_COPPER_RICH = ITEMS.register("crop/copper_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_COPPER_RICH.get(), "cyprium"));
    public static final DeferredItem<Item> SEED_TIN_RICH = ITEMS.register("crop/tin_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_TIN_RICH.get(), "stagnium"));
    public static final DeferredItem<Item> SEED_TITANIUM_RICH = ITEMS.register("crop/titanium_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_TITANIUM_RICH.get(), "titanium"));
    public static final DeferredItem<Item> SEED_LEAD_RICH = ITEMS.register("crop/lead_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_LEAD_RICH.get(), "plumbiscus"));
    public static final DeferredItem<Item> SEED_URANIUM_RICH = ITEMS.register("crop/uranium_rich_seed", () -> new RichSeedItem(new Item.Properties(), mio_icif_blocks.CROP_URANIUM_RICH.get(), "uranium"));

    //CarryEvent
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}