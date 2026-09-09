package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_helmet_nano extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 1000000;
    public static final int NIGHT_VISION_ENERGY_COST = 5;
    public static final int NIGHT_VISION_DURATION = 300;

    public mio_icif_helmet_nano(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "nano", MAX_ENERGY, 0, 5);
    }

    @Override
    public long getEnergyPerDamage() {
        return 5000;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? 0.15F : 0.0F;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.HEAD, "night_vision", "tooltip.mio_icif.armor.feature_night_vision")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet != stack || isEmpty(stack)) {
            // IC2原版行为：取下头盔时，不移除夜视效果，让效果自然过期
            return;
        }

        if (ArmorFeatureToggle.isEnabled(stack, "night_vision")) {
            if (consumeEnergy(stack, NIGHT_VISION_ENERGY_COST)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false, false));
            }
            // IC2原版行为：能量不足或夜视关闭时，不移除夜视效果，让效果自然过期
        }
    }

}