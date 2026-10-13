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
    /** 0.1.8.6: Mk.II items of the nano units ("module/item_module_<id>_mk2"). */
    private static final Map<SuitModuleType, DeferredItem<SuitModuleItem>> UNITS_MK2 = new EnumMap<>(SuitModuleType.class);
    static {
        for (SuitModuleType type : SuitModuleType.values()) {
            UNITS.put(type, ITEMS.register("module/item_module_" + type.id(), () -> new SuitModuleItem(type, new Item.Properties())));
        }
        for (SuitModuleType type : SuitModuleType.values()) {
            if (type.hasMk2()) UNITS_MK2.put(type, ITEMS.register("module/item_module_" + type.id() + "_mk2",
                () -> new SuitModuleItem(type, 2, new Item.Properties())));
        }
    }

    public static SuitModuleItem unitItem(SuitModuleType type) {
        return UNITS.get(type).get();
    }

    /** The item of {@code type} at {@code tier} (Mk.I when the unit has no Mk.II). */
    public static SuitModuleItem unitItem(SuitModuleType type, int tier) {
        DeferredItem<SuitModuleItem> mk2 = tier >= 2 ? UNITS_MK2.get(type) : null;
        return mk2 != null ? mk2.get() : unitItem(type);
    }

    /** Every unit item: Mk.I in enum order, then Mk.II. */
    public static Iterable<DeferredItem<SuitModuleItem>> units() {
        java.util.List<DeferredItem<SuitModuleItem>> all = new java.util.ArrayList<>(UNITS.values());
        all.addAll(UNITS_MK2.values());
        return all;
    }

    // ---------------------------------------------------------------- nano suit (0.1.8.5)

    /** Lattice densifier: +2 LP for a nano piece in the station. */
    public static final DeferredItem<com.miophas.singularity_iteration.common.suit.nano.LatticeDensifierItem> LATTICE_DENSIFIER =
        ITEMS.register("module/item_nano_lattice_densifier",
            () -> new com.miophas.singularity_iteration.common.suit.nano.LatticeDensifierItem(new Item.Properties()));

    public static final DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Singularity_Iteration.MOD_ID);

    /** Player attachment: nano core (reserve, heat, burst keys); saved and copied on death. */
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<com.miophas.singularity_iteration.common.suit.nano.NanoCoreState>> NANO_CORE =
        ATTACHMENTS.register("nano_core", () -> net.neoforged.neoforge.attachment.AttachmentType
            .builder(com.miophas.singularity_iteration.common.suit.nano.NanoCoreState::new)
            .serialize(com.miophas.singularity_iteration.common.suit.nano.NanoCoreState.CODEC).copyOnDeath().build());

    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Singularity_Iteration.MOD_ID);

    /** EMP stun: no target, no walking, creeper fuse falls back. */
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, com.miophas.singularity_iteration.common.suit.nano.EmpStunEffect> EMP_STUN =
        EFFECTS.register("emp_stun", com.miophas.singularity_iteration.common.suit.nano.EmpStunEffect::new);

    // ---------------------------------------------------------------- frame cores (0.1.9.0)

    private static final Map<com.miophas.singularity_iteration.common.suit.core.FrameCore, DeferredItem<com.miophas.singularity_iteration.common.suit.core.FrameCoreItem>> CORES = new EnumMap<>(com.miophas.singularity_iteration.common.suit.core.FrameCore.class);
    static {
        for (com.miophas.singularity_iteration.common.suit.core.FrameCore core : com.miophas.singularity_iteration.common.suit.core.FrameCore.values()) {
            CORES.put(core, ITEMS.register(core.itemPath(), () -> new com.miophas.singularity_iteration.common.suit.core.FrameCoreItem(core, new Item.Properties())));
        }
    }

    public static com.miophas.singularity_iteration.common.suit.core.FrameCoreItem coreItem(com.miophas.singularity_iteration.common.suit.core.FrameCore core) {
        return CORES.get(core).get();
    }

    public static Iterable<DeferredItem<com.miophas.singularity_iteration.common.suit.core.FrameCoreItem>> cores() {
        return CORES.values();
    }

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);
    /** One legacy shell in the crafting grid becomes its conversion. */
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,
        net.minecraft.world.item.crafting.RecipeSerializer<com.miophas.singularity_iteration.common.suit.core.LegacyConversionRecipe>> LEGACY_CONVERSION =
        SERIALIZERS.register("legacy_armor_conversion",
            () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(com.miophas.singularity_iteration.common.suit.core.LegacyConversionRecipe::new));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ATTACHMENTS.register(modBus);
        EFFECTS.register(modBus);
        SERIALIZERS.register(modBus);
        com.miophas.singularity_iteration.common.suit.core.FrameCores.bootstrap();
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
