package com.miophas.singularity_iteration.common.registry;

import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;

import com.miophas.singularity_iteration.common.item.component.FuelRodDurability;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class mio_icif_data_components {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BatteryEnergy>> BATTERY_ENERGY =
        com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents.BATTERY_ENERGY;

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuelRodDurability>> FUEL_ROD_DURABILITY =
        DATA_COMPONENTS.registerComponentType("fuel_rod_durability",
            builder -> builder.persistent(FuelRodDurability.CODEC).networkSynchronized(FuelRodDurability.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReactorComponentData>> REACTOR_COMPONENT_DATA =
        DATA_COMPONENTS.registerComponentType("reactor_component_data",
            builder -> builder.persistent(ReactorComponentData.CODEC).networkSynchronized(ReactorComponentData.STREAM_CODEC)
        );

    /** Upgrade units installed in a quantum suit piece (0.1.7.33). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.miophas.singularity_iteration.common.suit.InstalledModules>> SUIT_MODULES =
        DATA_COMPONENTS.registerComponentType("suit_modules",
            builder -> builder.persistent(com.miophas.singularity_iteration.common.suit.InstalledModules.CODEC)
                .networkSynchronized(com.miophas.singularity_iteration.common.suit.InstalledModules.STREAM_CODEC));
}
