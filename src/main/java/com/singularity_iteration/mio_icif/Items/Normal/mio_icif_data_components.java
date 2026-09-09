package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.Items.DataComponent.FuelRodDurability;
import com.singularity_iteration.mio_icif.Items.DataComponent.ReactorComponentData;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class mio_icif_data_components {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BatteryEnergy>> BATTERY_ENERGY =
        DATA_COMPONENTS.registerComponentType("battery_energy",
            builder -> builder.persistent(BatteryEnergy.CODEC).networkSynchronized(BatteryEnergy.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuelRodDurability>> FUEL_ROD_DURABILITY =
        DATA_COMPONENTS.registerComponentType("fuel_rod_durability",
            builder -> builder.persistent(FuelRodDurability.CODEC).networkSynchronized(FuelRodDurability.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReactorComponentData>> REACTOR_COMPONENT_DATA =
        DATA_COMPONENTS.registerComponentType("reactor_component_data",
            builder -> builder.persistent(ReactorComponentData.CODEC).networkSynchronized(ReactorComponentData.STREAM_CODEC)
        );
}

