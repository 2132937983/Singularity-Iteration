// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.logistics;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrations of the logistics additions (0.1.8.0): the Ejector Pipe. */
@SuppressWarnings("null")
public final class LogisticsRegistry {
    private LogisticsRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredBlock<EjectorPipeBlock> EJECTOR = BLOCKS.register("pipe/block_pipe_ejector",
        () -> new EjectorPipeBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F)
            .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> EJECTOR_ITEM = ITEMS.register("pipe/block_pipe_ejector",
        () -> new BlockItem(EJECTOR.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EjectorPipeBlockEntity>> EJECTOR_ENTITY =
        BLOCK_ENTITIES.register("pipe_ejector", () -> BlockEntityType.Builder.of(EjectorPipeBlockEntity::new, EJECTOR.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(LogisticsRegistry::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EJECTOR_ENTITY.get(), EjectorPipeBlockEntity::itemCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EJECTOR_ENTITY.get(), EjectorPipeBlockEntity::fluidCapability);
    }
}
