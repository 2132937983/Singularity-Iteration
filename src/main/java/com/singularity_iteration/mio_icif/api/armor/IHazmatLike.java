package com.singularity_iteration.mio_icif.api.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 防辐射装备接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IHazmatLike}。
 * 实现此接口的物品可以提供防辐射保护。
 */
public interface IHazmatLike {

    /**
     * 检查此装备是否提供防辐射保护。
     *
     * @param entity     穿着实体
     * @param slot       装备槽位
     * @param stack      物品堆
     * @return 如果提供防辐射保护则返回 true
     */
    boolean providesHazmatProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack);

    /**
     * 检查实体是否穿着了完整的防辐射装备。
     * <p>
     * 便捷静态方法，检查所有装甲槽位是否都有防辐射装备。
     *
     * @param entity 要检查的实体
     * @return 如果穿着了完整防辐射装备则返回 true
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