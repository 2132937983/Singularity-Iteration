package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Kinetic rotor interface.
 * <p>
 * Equivalent to {@code IKineticRotor} in IC2 1.12.2.
 * An item that implements this interface can be the rotor of a wind or water kinetic generator.
 * The rotor converts wind or water flow into kinetic energy.
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
