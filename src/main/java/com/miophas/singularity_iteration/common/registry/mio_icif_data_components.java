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

    /** 0.1.10.28 (X4): the data of a written ID card of the exchange (absent = blank card). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.miophas.singularity_iteration.common.exchange.IdCardData>> ID_CARD =
        DATA_COMPONENTS.registerComponentType("id_card",
            builder -> builder.persistent(com.miophas.singularity_iteration.common.exchange.IdCardData.CODEC)
                .networkSynchronized(com.miophas.singularity_iteration.common.exchange.IdCardData.STREAM_CODEC));

    /** Lattice densifier level of a nano suit piece (0.1.8.5): each level adds 2 lattice points (LP). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SUIT_LATTICE =
        DATA_COMPONENTS.registerComponentType("suit_lattice",
            builder -> builder.persistent(com.mojang.serialization.Codec.intRange(0, 8))
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Units of a suit piece that are installed as Mk.II (0.1.8.6); same id format as {@link #SUIT_MODULES}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.miophas.singularity_iteration.common.suit.InstalledModules>> SUIT_MK2 =
        DATA_COMPONENTS.registerComponentType("suit_mk2",
            builder -> builder.persistent(com.miophas.singularity_iteration.common.suit.InstalledModules.CODEC)
                .networkSynchronized(com.miophas.singularity_iteration.common.suit.InstalledModules.STREAM_CODEC));

    /** Expansion bay level of a quantum suit piece (0.1.10.0): each level adds one unit slot. Absent = 0 (the pre-0.1.10.0 count). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SUIT_BAYS =
        DATA_COMPONENTS.registerComponentType("suit_bays",
            builder -> builder.persistent(com.mojang.serialization.Codec.intRange(0, 4))
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /**
     * Entanglement links of a quantum chestplate (0.1.10.1): the ids of the linked synergies (a built-in
     * id such as "predictive_guidance", or an addon "namespace:path"). Absent = no link.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> SUIT_LINKS =
        DATA_COMPONENTS.registerComponentType("suit_links",
            builder -> builder.persistent(com.mojang.serialization.Codec.STRING.listOf())
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list(16))));

    /** Frame core in the core socket of a suit helmet or chestplate (0.1.9.0): the core id. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SUIT_CORE =
        DATA_COMPONENTS.registerComponentType("suit_core",
            builder -> builder.persistent(com.mojang.serialization.Codec.STRING)
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** Colour palette of a suit piece (0.1.8.0): primary plate colour and accent light colour. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.miophas.singularity_iteration.common.suit.SuitPalette>> SUIT_PALETTE =
        DATA_COMPONENTS.registerComponentType("suit_palette",
            builder -> builder.persistent(com.miophas.singularity_iteration.common.suit.SuitPalette.CODEC)
                .networkSynchronized(com.miophas.singularity_iteration.common.suit.SuitPalette.STREAM_CODEC));
}
