package com.miophas.singularity_iteration.core.platform.neoforge;

import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Core-owned battery state, retaining its existing persistent registry identity. */
public final class CoreDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "mio_icif");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BatteryEnergy>> BATTERY_ENERGY =
        DATA_COMPONENTS.registerComponentType("battery_energy",
            builder -> builder.persistent(BatteryEnergy.CODEC).networkSynchronized(BatteryEnergy.STREAM_CODEC));
    private CoreDataComponents() {}
}
