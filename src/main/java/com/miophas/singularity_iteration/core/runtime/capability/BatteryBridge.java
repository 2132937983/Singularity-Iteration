package com.miophas.singularity_iteration.core.runtime.capability;


import net.minecraft.world.item.ItemStack;

/**
 * Central adapter layer for the internal battery types.
 *
 * <p>{@code MioIcifCapabilitiesImpl.BatteryItemImpl} uses this class to access the internal battery components.
 * It does not refer directly to {@code Items.Normal.BatteryEnergy} or {@code Items.Normal.mio_icif_data_components}.
 *
 * <p>This class is package-private. It is not exposed outside the package.
 */
final class BatteryBridge {

    private BatteryBridge() {}

    static long getCharge(ItemStack stack) {
        var component = stack.get(com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents.BATTERY_ENERGY.get());
        return component != null ? component.energy() : 0L;
    }

    static void setCharge(ItemStack stack, long charge, long maxCharge) {
        long clamped = Math.max(0L, Math.min(charge, maxCharge));
        stack.set(com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents.BATTERY_ENERGY.get(),
            new com.miophas.singularity_iteration.core.api.item.BatteryEnergy(clamped, maxCharge));
    }
}
