package com.miophas.singularity_iteration.core.api.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for radiation protection equipment.
 * <p>
 * This is the equivalent of {@code IHazmatLike} in IC2 for 1.12.2.
 * An item that implements this interface can give radiation protection.
 */
public interface IHazmatLike {

    /**
     * Checks if this equipment gives radiation protection.
     *
     * @param entity     the entity that wears the equipment
     * @param slot       the equipment slot
     * @param stack      the item stack
     * @return true if the equipment gives radiation protection
     */
    boolean providesHazmatProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack);

    /**
     * Checks if the entity wears a full set of radiation protection equipment.
     * <p>
     * This is a static shortcut. It checks all armor slots for radiation protection equipment.
     * Note: the current code only rejects {@link IHazmatLike} items that give no protection.
     * Empty slots and other items do not cause a false result.
     *
     * @param entity the entity to check; can be null (then the result is false)
     * @return true if the entity wears a full set of radiation protection equipment
     */
    static boolean hasCompleteHazmat(LivingEntity entity) {
        if (entity == null) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof IHazmatLike hazmat && !hazmat.providesHazmatProtection(entity, slot, stack)) {
                return false;
            }
        }
        return true;
    }
}