// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations of the quantum suit upgrade system; {@link #register} runs in the mod constructor. */
@SuppressWarnings("null")
public final class SuitRegistry {
    private SuitRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Singularity_Iteration.MOD_ID);

    public static final DeferredBlock<QuantumModStationBlock> STATION = BLOCKS.register("producer/block_quantum_modification_station",
        () -> new QuantumModStationBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 30.0F)
            .sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 6)));
    public static final DeferredItem<BlockItem> STATION_ITEM = ITEMS.register("producer/block_quantum_modification_station",
        () -> new BlockItem(STATION.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumModStationBlockEntity>> STATION_ENTITY =
        BLOCK_ENTITIES.register("quantum_modification_station",
            () -> BlockEntityType.Builder.of(QuantumModStationBlockEntity::new, STATION.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<QuantumModStationMenu>> STATION_MENU = MENUS.register("quantum_modification_station_menu",
        () -> new MenuType<>((IContainerFactory<QuantumModStationMenu>) QuantumModStationMenu::new, FeatureFlags.VANILLA_SET));

    private static final Map<SuitModuleType, DeferredItem<SuitModuleItem>> UNITS = new EnumMap<>(SuitModuleType.class);
    static {
        for (SuitModuleType type : SuitModuleType.values()) {
            UNITS.put(type, ITEMS.register("module/item_module_" + type.id(), () -> new SuitModuleItem(type, new Item.Properties())));
        }
    }

    public static SuitModuleItem unitItem(SuitModuleType type) {
        return UNITS.get(type).get();
    }

    public static Iterable<DeferredItem<SuitModuleItem>> units() {
        return UNITS.values();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(SuitRegistry::capabilities);
        SuitModules.bootstrap();
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EUApi.SIDED, STATION_ENTITY.get(), (entity, side) ->
            entity instanceof AbstractEnergyBlockEntity energy ? energy.getEnergyStorageCapability(side) : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STATION_ENTITY.get(), (entity, side) ->
            entity.getItemHandlerCapability(side));
    }
}
