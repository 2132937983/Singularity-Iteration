package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.armor.IMetalArmor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * «Cš®“°»L
 * ¤ñ??¥ÒµyüLªº´¶³q“°¥Ò
 */
@SuppressWarnings("null")
public class mio_icif_leggings_bronze extends ArmorItem implements IMetalArmor {

    private static final ResourceLocation BRONZE_ARMOR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/bronze_2.png");

    public mio_icif_leggings_bronze(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.LEGGINGS, properties);
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return BRONZE_ARMOR_TEXTURE;
    }

    @Override
    public boolean isMetalArmor(ItemStack stack, Entity entity) {
        return true;
    }
}

