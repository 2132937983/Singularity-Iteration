// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.armor.IMetalArmor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 青铜胸甲
 * 比锝链甲稝强的普通护???
 */
@SuppressWarnings("null")
public class mio_icif_chestplate_bronze extends ArmorItem implements IMetalArmor {

    private static final ResourceLocation BRONZE_ARMOR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/bronze_1.png");

    public mio_icif_chestplate_bronze(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
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

