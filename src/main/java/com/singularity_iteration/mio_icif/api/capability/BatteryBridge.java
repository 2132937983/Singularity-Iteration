package com.singularity_iteration.mio_icif.api.capability;

import net.minecraft.world.item.ItemStack;

/**
 * 电池内部类型的集中适配层。
 *
 * <p>{@code MioIcifCapabilitiesImpl.BatteryItemImpl} 通过此类访问内部电池组件，
 * 而非直接引用 {@code Items.Normal.BatteryEnergy} 或 {@code Items.Normal.mio_icif_data_components}。
 *
 * <p>此类为包级私有，不对外暴露。
 */
final class BatteryBridge {

    private BatteryBridge() {}

    static long getCharge(ItemStack stack) {
        var component = stack.get(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components.BATTERY_ENERGY.get());
        return component != null ? component.energy() : 0L;
    }

    static void setCharge(ItemStack stack, long charge, long maxCharge) {
        long clamped = Math.max(0L, Math.min(charge, maxCharge));
        stack.set(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components.BATTERY_ENERGY.get(),
            new com.singularity_iteration.mio_icif.Items.Normal.BatteryEnergy(clamped, maxCharge));
    }
}
