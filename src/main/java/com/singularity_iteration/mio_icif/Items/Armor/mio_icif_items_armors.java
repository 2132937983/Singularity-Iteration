package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Armor.hazmat.*;

import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Equipment item registration
 * Used to register all armor items
 */
@SuppressWarnings("null")
public class mio_icif_items_armors {

    // Item registry
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // ==================== 青铜护甲 (Bronze Armor) ====================
    public static final DeferredItem<Item> ARMOR_BRONZE_HELMET = ITEMS.register("armor/item_armor_bronze_helmet", () -> new mio_icif_helmet_bronze(ArmorMaterials.IRON, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<Item> ARMOR_BRONZE_CHESTPLATE = ITEMS.register("armor/item_armor_bronze_chestplate", () -> new mio_icif_chestplate_bronze(ArmorMaterials.IRON, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<Item> ARMOR_BRONZE_LEGGINGS = ITEMS.register("armor/item_armor_bronze_leggings", () -> new mio_icif_leggings_bronze(ArmorMaterials.IRON, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(15))));
    public static final DeferredItem<Item> ARMOR_BRONZE_BOOTS = ITEMS.register("armor/item_armor_bronze_boots", () -> new mio_icif_boots_bronze(ArmorMaterials.IRON, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(15))));

    // ==================== 电力护甲注册位置 ====================

    //item_armor_jetpack_electric
    public static final DeferredItem<Item> ARMOR_JETPACK_ELECTRIC = ITEMS.register("armor/item_armor_jetpack_electric", () -> new mio_icif_chestplate_jetpack_elc(ArmorMaterials.IRON, new Item.Properties()));

    // Battery backpacks
    public static final DeferredItem<Item> ARMOR_BATPACK = ITEMS.register("armor/item_armor_batpack", () -> new mio_icif_chestplate_batpack(ArmorMaterials.IRON, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_ADV_BATPACK = ITEMS.register("armor/item_armor_advbatpack", () -> new mio_icif_chestplate_advbatpack(ArmorMaterials.IRON, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_ENERGYPACK = ITEMS.register("armor/item_armor_energypack", () -> new mio_icif_chestplate_energypack(ArmorMaterials.IRON, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_LAPPACK = ITEMS.register("armor/item_armor_lappack", () -> new mio_icif_chestplate_lappack(ArmorMaterials.IRON, new Item.Properties()));

    // 纳米装甲 (NanoSuit)
    public static final DeferredItem<Item> ARMOR_NANO_HELMET = ITEMS.register("armor/item_armor_nano_helmet", () -> new mio_icif_helmet_nano(ArmorMaterials.DIAMOND, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_NANO_CHESTPLATE = ITEMS.register("armor/item_armor_nano_chestplate", () -> new mio_icif_chestplate_nano(ArmorMaterials.DIAMOND, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_NANO_LEGGINGS = ITEMS.register("armor/item_armor_nano_leggings", () -> new mio_icif_leggings_nano(ArmorMaterials.DIAMOND, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_NANO_BOOTS = ITEMS.register("armor/item_armor_nano_boots", () -> new mio_icif_boots_nano(ArmorMaterials.DIAMOND, new Item.Properties()));

    // 量子装甲 (QuantumSuit)
    public static final DeferredItem<Item> ARMOR_QUANTUM_HELMET = ITEMS.register("armor/item_armor_quantum_helmet", () -> new mio_icif_helmet_quantum(MioIcifArmorMaterials.QUANTUM, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_QUANTUM_CHESTPLATE = ITEMS.register("armor/item_armor_quantum_chestplate", () -> new mio_icif_chestplate_quantum(MioIcifArmorMaterials.QUANTUM, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_QUANTUM_LEGGINGS = ITEMS.register("armor/item_armor_quantum_leggings", () -> new mio_icif_leggings_quantum(MioIcifArmorMaterials.QUANTUM, new Item.Properties()));
    public static final DeferredItem<Item> ARMOR_QUANTUM_BOOTS = ITEMS.register("armor/item_armor_quantum_boots", () -> new mio_icif_boots_quantum(MioIcifArmorMaterials.QUANTUM, new Item.Properties()));

    // Nightvision Goggles
    public static final DeferredItem<Item> ARMOR_NIGHTVISION_GOGGLES = ITEMS.register("armor/item_armor_nightvision_goggles", () -> new mio_icif_nightvision_goggles(ArmorMaterials.IRON, new Item.Properties()));

    // ==================== METS 移植装备 ====================

    // Diving Mask
    public static final DeferredItem<Item> ARMOR_DIVING_MASK = ITEMS.register("armor/item_armor_diving_mask", () -> new mio_icif_diving_mask(ArmorMaterials.IRON, new Item.Properties()));

    // Advanced Electric Jetpack
    public static final DeferredItem<Item> ARMOR_ADVANCED_JETPACK = ITEMS.register("armor/item_armor_advanced_jetpack", () -> new mio_icif_chestplate_advanced_jetpack(ArmorMaterials.IRON, new Item.Properties()));

    // Heavy Quantum Chestplate
    public static final DeferredItem<Item> ARMOR_HEAVY_QUANTUM_CHESTPLATE = ITEMS.register("armor/item_armor_heavy_quantum_chestplate", () -> new mio_icif_chestplate_heavy_quantum(MioIcifArmorMaterials.HEAVY_QUANTUM, new Item.Properties()));

    // Advanced Quantum Chestplate
    public static final DeferredItem<Item> ARMOR_ADVANCED_QUANTUM_CHESTPLATE = ITEMS.register("armor/item_armor_advanced_quantum_chestplate", () -> new mio_icif_chestplate_advanced_quantum(MioIcifArmorMaterials.ADVANCED_QUANTUM, new Item.Properties()));

    // ==================== ASP 太阳能头盔 ====================
    // 高级太阳能头盔
    public static final DeferredItem<Item> ARMOR_ADVANCED_SOLAR_HELMET = ITEMS.register("armor/item_armor_advanced_solar_helmet", () -> new mio_icif_advanced_solar_helmet(ArmorMaterials.DIAMOND, new Item.Properties()));
    // 混合太阳能头盔
    public static final DeferredItem<Item> ARMOR_HYBRID_SOLAR_HELMET = ITEMS.register("armor/item_armor_hybrid_solar_helmet", () -> new mio_icif_hybrid_solar_helmet(ArmorMaterials.DIAMOND, new Item.Properties()));
    // 终极混合太阳能头盔
    public static final DeferredItem<Item> ARMOR_ULTIMATE_SOLAR_HELMET = ITEMS.register("armor/item_armor_ultimate_solar_helmet", () -> new mio_icif_ultimate_solar_helmet(ArmorMaterials.NETHERITE, new Item.Properties()));

    // ==================== Hazmat Suit ====================

    public static final DeferredItem<Item> HAZMAT_HELMET = ITEMS.register("armor/item_armor_hazmat_helmet", () -> new mio_icif_hazmat_helmet(ArmorMaterials.LEATHER, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(5))));
    public static final DeferredItem<Item> HAZMAT_CHESTPLATE = ITEMS.register("armor/item_armor_hazmat_chestplate", () -> new mio_icif_hazmat_chestplate(ArmorMaterials.LEATHER, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(5))));
    public static final DeferredItem<Item> HAZMAT_LEGGINGS = ITEMS.register("armor/item_armor_hazmat_leggings", () -> new mio_icif_hazmat_leggings(ArmorMaterials.LEATHER, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(5))));
    public static final DeferredItem<Item> HAZMAT_BOOTS = ITEMS.register("armor/item_armor_hazmat_boots", () -> new mio_icif_hazmat_boots(ArmorMaterials.LEATHER, new Item.Properties().stacksTo(1).durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(5))));

    // ==================== 注册方法 ====================
    /**
     * 注册所有护甲物品到事件总线
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}