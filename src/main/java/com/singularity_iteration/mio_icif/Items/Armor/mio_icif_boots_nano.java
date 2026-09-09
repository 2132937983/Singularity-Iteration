package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings({"null", "deprecation"})
public class mio_icif_boots_nano extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 1000000;

    public static final int FREE_FALL_DISTANCE = 4;
    public static final int FULL_ABSORB_FALL_DISTANCE = 12;
    public static final int FALL_ENERGY_PER_BLOCK = 5000;

    public mio_icif_boots_nano(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties, MAX_ENERGY, 0, "nano", MAX_ENERGY, 0, 5);
    }

    @Override
    public long getEnergyPerDamage() {
        return 5000;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? 0.15F : 0.0F;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }
}