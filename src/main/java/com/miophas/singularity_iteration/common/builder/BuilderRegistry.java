// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
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

/** Registrations of the Blueprint Builder system (0.1.8.0); {@link #register} runs from the mod constructor. */
@SuppressWarnings("null")
public final class BuilderRegistry {
    private BuilderRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Singularity_Iteration.MOD_ID);

    public static final DeferredBlock<BlueprintBuilderBlock> BUILDER = BLOCKS.register("producer/block_blueprint_builder",
        () -> new BlueprintBuilderBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 30.0F)
            .sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> BUILDER_ITEM = ITEMS.register("producer/block_blueprint_builder",
        () -> new com.miophas.singularity_iteration.common.block.MachineBlockItem(BUILDER.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredBlock<MaterialInterfaceBlock> MATERIAL_INTERFACE = BLOCKS.register("producer/block_material_interface",
        () -> new MaterialInterfaceBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 12.0F)
            .sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> MATERIAL_INTERFACE_ITEM = ITEMS.register("producer/block_material_interface",
        () -> new com.miophas.singularity_iteration.common.block.MachineBlockItem(MATERIAL_INTERFACE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintBuilderBlockEntity>> BUILDER_ENTITY =
        BLOCK_ENTITIES.register("blueprint_builder", () -> BlockEntityType.Builder.of(BlueprintBuilderBlockEntity::new, BUILDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaterialInterfaceBlockEntity>> MATERIAL_INTERFACE_ENTITY =
        BLOCK_ENTITIES.register("material_interface", () -> BlockEntityType.Builder.of(MaterialInterfaceBlockEntity::new, MATERIAL_INTERFACE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintBuilderMenu>> BUILDER_MENU = MENUS.register("blueprint_builder",
        () -> new MenuType<>((IContainerFactory<BlueprintBuilderMenu>) BlueprintBuilderMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(BuilderRegistry::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EUApi.SIDED, BUILDER_ENTITY.get(), (entity, side) ->
            entity instanceof AbstractEnergyBlockEntity energy ? energy.getEnergyStorageCapability(side) : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BUILDER_ENTITY.get(), (entity, side) -> entity.getItemHandlerCapability(side));
        // the interface exposes its own 27 slots (pipes, hoppers, RTS Building linked storage, ...)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MATERIAL_INTERFACE_ENTITY.get(), (entity, side) -> entity.handler());
    }
}
