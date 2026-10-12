package com.miophas.singularity_iteration.common.item.armor;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * 量子护甲自定义盔甲材料
 */
public class MioIcifArmorMaterials {

    // 量子装甲材料：9点盔甲，3点韧性，10%击退抗性
    public static final Holder<ArmorMaterial> QUANTUM = create(
        "quantum",
        Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, 3);
            map.put(ArmorItem.Type.CHESTPLATE, 8);
            map.put(ArmorItem.Type.LEGGINGS, 6);
            map.put(ArmorItem.Type.BOOTS, 2);
        }),
        25,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "quantum"))),
        3.0F,
        0.1F
    );

    // 重型量子胸甲材料：9点盔甲，3点韧性，10%击退抗性
    public static final Holder<ArmorMaterial> HEAVY_QUANTUM = create(
        "heavy_quantum",
        Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, 0);
            map.put(ArmorItem.Type.CHESTPLATE, 9);
            map.put(ArmorItem.Type.LEGGINGS, 0);
            map.put(ArmorItem.Type.BOOTS, 0);
        }),
        25,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "heavy_quantum"))),
        3.0F,
        0.1F
    );

    // 进阶量子胸甲材料：9点盔甲，3点韧性，10%击退抗性
    public static final Holder<ArmorMaterial> ADVANCED_QUANTUM = create(
        "advanced_quantum",
        Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, 0);
            map.put(ArmorItem.Type.CHESTPLATE, 9);
            map.put(ArmorItem.Type.LEGGINGS, 0);
            map.put(ArmorItem.Type.BOOTS, 0);
        }),
        25,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "advanced_quantum"))),
        3.0F,
        0.1F
    );

    // 实用护甲材料（太阳能头盔等）：0 护甲值，对应 IC2 ItemArmorUtility 的"无防御"
    public static final Holder<ArmorMaterial> UTILITY = create(
        "utility",
        Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, 0);
            map.put(ArmorItem.Type.CHESTPLATE, 0);
            map.put(ArmorItem.Type.LEGGINGS, 0);
            map.put(ArmorItem.Type.BOOTS, 0);
        }),
        10,
        SoundEvents.ARMOR_EQUIP_LEATHER,
        () -> Ingredient.EMPTY,
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "utility"))),
        0.0F,
        0.0F
    );

    /**
     * 0.1.8.0: nano suit material. The values are those of vanilla diamond (the nano suit used
     * ArmorMaterials.DIAMOND before). It has two layers for the suit palette: layer 0 draws the
     * plates (primary colour), layer 1 draws the light strips (accent colour). See SuitPalette.
     */
    public static final Holder<ArmorMaterial> NANO = create(
        "nano",
        Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, 3);
            map.put(ArmorItem.Type.CHESTPLATE, 8);
            map.put(ArmorItem.Type.LEGGINGS, 6);
            map.put(ArmorItem.Type.BOOTS, 3);
            map.put(ArmorItem.Type.BODY, 11);
        }),
        10,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        () -> Ingredient.of(net.minecraft.world.item.Items.DIAMOND),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "nano")),
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_accent"))),
        2.0F,
        0.0F
    );

    /** True for the accent layer (layer 1) of a palette material. */
    public static boolean isAccentLayer(ArmorMaterial.Layer layer) {
        return layer != null && layer.texture(false).getPath().contains("_accent");
    }

    private static Holder<ArmorMaterial> create(String name, EnumMap<ArmorItem.Type, Integer> defense, int enchantmentValue, Holder<SoundEvent> sound, Supplier<Ingredient> repairIngredient, List<ArmorMaterial.Layer> layers, float toughness, float knockbackResistance) {
        ArmorMaterial material = new ArmorMaterial(
            defense,
            enchantmentValue,
            sound,
            repairIngredient,
            layers,
            toughness,
            knockbackResistance
        );
        return Holder.direct(material);
    }
}