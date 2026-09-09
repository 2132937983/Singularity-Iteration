package com.singularity_iteration.mio_icif.util;

import com.singularity_iteration.mio_icif.Items.Armor.hazmat.*;
import com.singularity_iteration.mio_icif.api.item.IElectricArmorItem;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class RadiationProtectionUtil {

    private static final int QUANTUM_ARMOR_TIER = 6;

    public static boolean isWearingFullHazmat(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);

        return helmet.getItem() instanceof mio_icif_hazmat_helmet &&
               chestplate.getItem() instanceof mio_icif_hazmat_chestplate &&
               leggings.getItem() instanceof mio_icif_hazmat_leggings &&
               boots.getItem() instanceof mio_icif_hazmat_boots;
    }

    public static boolean isWearingFullQuantumSet(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        var itemApi = MioIcifAPI.instance().getItemAPI();
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !itemApi.isElectricArmor(stack)) {
                return false;
            }
            if (!(stack.getItem() instanceof IElectricArmorItem armor) || armor.getArmorTier() != QUANTUM_ARMOR_TIER) {
                return false;
            }
        }
        return true;
    }

    public static int getRadiationProtectionLevel(LivingEntity entity) {
        if (isWearingFullHazmat(entity)) {
            return 4;
        }
        if (isWearingFullQuantumSet(entity)) {
            return 4;
        }
        return 0;
    }

    public static float calculateRadiationDamage(LivingEntity entity, float baseDamage) {
        if (isWearingFullHazmat(entity)) {
            return 0.0F;
        }
        if (isWearingFullQuantumSet(entity)) {
            return 0.0F;
        }

        int protectionLevel = getRadiationProtectionLevel(entity);
        float damageReduction = Math.min(1.0F, protectionLevel * 0.25F);
        return baseDamage * (1.0F - damageReduction);
    }
}