// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/** Public jetpack contract, independently implemented from the frozen SI ABI. */
public interface IJetpackItem extends IBatteryItem, IArmorModeFeatureItem {

    /** The feature key of the jetpack mode (a mode-type feature). */
    String FEATURE_MODE_KEY = "jetpack_mode";

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
        cost = EnergySaving.apply(stack, cost);
        if (cost == 0L) return true;
        long extracted = extractEnergy(stack, cost);
        if (extracted == cost) return true;
        if (extracted > 0L) addEnergy(stack, extracted);
        return false;
    }

    /**
     * Default mode switch: hover ↔ flight. Any mode other than HOVER switches to HOVER.
     *
     * <p>A jetpack does not need its own {@link IArmorModeFeatureItem} implementation. The GUI, the command and the tooltip
     * all use this default implementation.
     *
     * @return true if the mode changed; false if {@code featureKey} is not {@link #FEATURE_MODE_KEY} or the stack is empty
     */
    @Override
    default boolean cycleMode(ItemStack stack, String featureKey) {
        if (!FEATURE_MODE_KEY.equals(featureKey) || stack.isEmpty()) {
            return false;
        }
        JetpackMode next = getMode(stack) == JetpackMode.HOVER ? JetpackMode.FLIGHT : JetpackMode.HOVER;
        setMode(stack, next);
        return true;
    }
}
