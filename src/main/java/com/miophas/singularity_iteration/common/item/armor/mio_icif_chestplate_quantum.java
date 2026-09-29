// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.runtime.flight.JetpackFlightController;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Retains the SI-declared quantum flight feature using the independently tested controller. */
@SuppressWarnings("deprecation")
public class mio_icif_chestplate_quantum extends mio_icif_armor_elc implements IJetpackItem {
    public static final int MAX_ENERGY = 10_000_000;
    public static final long FLIGHT_COST = 8;
    public static final long HOVER_COST = 7;
    private static final String MODE_KEY = "JetpackMode";

    public mio_icif_chestplate_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "quantum", 12_000, 0, 6);
    }

    @Override public long getEnergyPerDamage() { return 20_000; }
    @Override public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? 0.40F : 0.0F;
    }
    @Override public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(new ArmorFeatureInfo(EquipmentSlot.CHEST, "flight", "tooltip.mio_icif.armor.feature_flight"),
                new ArmorFeatureInfo(EquipmentSlot.CHEST, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
                new ArmorFeatureInfo(EquipmentSlot.CHEST, "jetpack_mode", "tooltip.mio_icif.armor.feature_jetpack_mode"));
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.CHEST) != stack) return;
        if (!level.isClientSide && !isEmpty(stack) && ArmorFeatureToggle.isEnabled(stack, "fire_resistance")) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
        }
        if (ArmorFeatureToggle.isEnabled(stack, "flight") && getMode(stack) != JetpackMode.OFF) {
            JetpackFlightController.tick(player, stack, this, HOVER_COST, 0.8D, 0.6D, -0.6D, true);
        }
    }
    @Override public float getThrust() { return 1.0F; }
    @Override public long getEnergyPerTickFlying() { return FLIGHT_COST; }
    @Override public JetpackMode getMode(ItemStack stack) {
        int mode = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return mode == 1 ? JetpackMode.HOVER : mode == 2 ? JetpackMode.OFF : JetpackMode.FLIGHT;
    }
    @Override public void setMode(ItemStack stack, JetpackMode mode) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, mode == JetpackMode.HOVER ? 1 : mode == JetpackMode.OFF ? 2 : 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}