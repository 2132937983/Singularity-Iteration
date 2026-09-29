package com.miophas.singularity_iteration.core.runtime.capability;


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
        var component = stack.get(com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents.BATTERY_ENERGY.get());
        return component != null ? component.energy() : 0L;
    }

    static void setCharge(ItemStack stack, long charge, long maxCharge) {
        long clamped = Math.max(0L, Math.min(charge, maxCharge));
        stack.set(com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents.BATTERY_ENERGY.get(),
            new com.miophas.singularity_iteration.core.api.item.BatteryEnergy(clamped, maxCharge));
    }
}
