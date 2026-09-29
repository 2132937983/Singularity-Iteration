package com.miophas.singularity_iteration.common.util;

import com.miophas.singularity_iteration.common.item.armor.hazmat.*;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_chestplate_advanced_quantum;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_chestplate_heavy_quantum;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_ultimate_solar_helmet;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class RadiationProtectionUtil {

    private static final int QUANTUM_ARMOR_TIER = 6;

    private RadiationProtectionUtil() {
    }

    /**
     * Quantum identity is independent of transfer tier: the two quantum chest
     * upgrades retain their declared tiers. Every piece must match its actual
     * equipment slot. Solar helmets use the separate radiation rule below.
     */
    public static boolean isQuantumSetPiece(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()
            || !(stack.getItem() instanceof ArmorItem armor)
            || armor.getEquipmentSlot() != slot
            || !(stack.getItem() instanceof IElectricArmorItem electric)) {
            return false;
        }
        return electric.getArmorTier() == QUANTUM_ARMOR_TIER
            || (slot == EquipmentSlot.CHEST
                && (armor instanceof mio_icif_chestplate_advanced_quantum
                    || armor instanceof mio_icif_chestplate_heavy_quantum));
    }

    public static boolean isWearingFullHazmat(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof mio_icif_hazmat_helmet
            && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof mio_icif_hazmat_chestplate
            && player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof mio_icif_hazmat_leggings
            && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof mio_icif_hazmat_boots;
    }

    public static boolean isWearingFullQuantumSet(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        var itemApi = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !itemApi.isElectricArmor(stack) || !isQuantumSetPiece(stack, slot)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The hybrid solar helmet is not a quantum-set component. It can still
     * replace the quantum helmet for radiation protection when the remaining
     * three armour slots contain quantum pieces, including quantum chest upgrades.
     */
    public static boolean isWearingUltimateSolarQuantumBody(LivingEntity entity) {
        if (!(entity instanceof Player player)
            || !(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof mio_icif_ultimate_solar_helmet)) {
            return false;
        }

        var itemApi = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !itemApi.isElectricArmor(stack) || !isQuantumSetPiece(stack, slot)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRadiationProtected(LivingEntity entity) {
        return isWearingFullHazmat(entity)
            || isWearingFullQuantumSet(entity)
            || isWearingUltimateSolarQuantumBody(entity);
    }

    public static int getRadiationProtectionLevel(LivingEntity entity) {
        return isRadiationProtected(entity) ? 4 : 0;
    }

    public static float calculateRadiationDamage(LivingEntity entity, float baseDamage) {
        if (isRadiationProtected(entity)) {
            return 0.0F;
        }

        int protectionLevel = getRadiationProtectionLevel(entity);
        float damageReduction = Math.min(1.0F, protectionLevel * 0.25F);
        return baseDamage * (1.0F - damageReduction);
    }
}
