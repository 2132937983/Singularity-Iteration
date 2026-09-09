package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.api.item.IDivingMaskItem;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 潜水面具 (Diving Mask)
 * 基于METS DivingMask重构
 * - 最大电量: 100,000 EU (Tier 2)
 * - 充电速率: 128 EU/t
 * - 水下时消耗能量恢复氧气
 */
@SuppressWarnings("null")
public class mio_icif_diving_mask extends mio_icif_armor_elc implements IDivingMaskItem {

    public static final int MAX_ENERGY = 100000;
    public static final int CHARGE_RATE = 128;
    public static final int ENERGY_PER_TICK = 15;
    public static final int ARMOR_TIER = 2;

    public mio_icif_diving_mask(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "diving_mask", CHARGE_RATE, ENERGY_PER_TICK, ARMOR_TIER);
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.HEAD, "oxygen_restore", "tooltip.mio_icif.armor.feature_oxygen_restore")
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
            return;
        }

        // 在水中且氧气不足时消耗能量恢复氧气
        if (player.isUnderWater() && ArmorFeatureToggle.isEnabled(stack, "oxygen_restore")) {
            int airSupply = player.getAirSupply();
            int maxAir = player.getMaxAirSupply();

            if (airSupply < maxAir && consumeEnergy(stack, ENERGY_PER_TICK)) {
                player.setAirSupply(Math.min(maxAir, airSupply + 1));
            }
        }
    }
}