package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;

@SuppressWarnings("null")
public class mio_icif_chestplate_nano extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 1000000;

    public mio_icif_chestplate_nano(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "nano", MAX_ENERGY, 0, 5);
    }

    @Override
    public long getEnergyPerDamage() {
        return 5000;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? 0.40F : 0.0F;
    }
}