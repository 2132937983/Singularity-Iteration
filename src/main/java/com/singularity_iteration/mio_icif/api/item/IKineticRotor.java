package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 动能转子接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IKineticRotor}。
 * 实现此接口的物品可以作为风力/水力发电机的转子，
 * 提供动能转换功能。
 */
public interface IKineticRotor {

    int getDiameter(ItemStack stack);

    ResourceLocation getRotorRenderTexture(ItemStack stack);

    float getEfficiency(ItemStack stack);

    int getMinWindStrength(ItemStack stack);

    int getMaxWindStrength(ItemStack stack);

    boolean isAcceptedType(ItemStack stack, GearboxType type);

    default void damageRotor(ItemStack stack, int amount) {
    }

    default int getDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    default int getMaxDurability() {
        return 0;
    }

    default boolean isWaterCompatible() {
        return false;
    }

    enum GearboxType {
        WATER,
        WIND
    }
}
