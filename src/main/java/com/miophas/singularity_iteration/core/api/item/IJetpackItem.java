// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/** Public jetpack contract, independently implemented from the frozen SI ABI. */
public interface IJetpackItem extends IBatteryItem {
    enum JetpackMode {
        OFF,
        NORMAL,
        HOVER,
        FLIGHT
    }

    float getThrust();

    long getEnergyPerTickFlying();

    JetpackMode getMode(ItemStack stack);

    void setMode(ItemStack stack, JetpackMode mode);

    default float getMaxHeight() {
        return 310.0F;
    }

    default boolean hasHeightLimit() {
        return true;
    }

    default float getHoverHeightOffset() {
        return 1.0F;
    }

    default boolean canFlyTick(ItemStack stack) {
        long cost = Math.max(0L, getEnergyPerTickFlying());
        return !stack.isEmpty() && stack.getCount() == 1 && getEnergy(stack) >= cost;
    }

    default boolean consumeFlyTick(ItemStack stack) {
        long cost = Math.max(0L, getEnergyPerTickFlying());
        if (!canFlyTick(stack)) return false;
        if (cost == 0L) return true;
        long extracted = extractEnergy(stack, cost);
        if (extracted == cost) return true;
        if (extracted > 0L) addEnergy(stack, extracted);
        return false;
    }
}
