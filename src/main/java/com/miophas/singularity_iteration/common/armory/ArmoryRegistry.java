package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

/** All registrations of the Armory system in one place; {@link #register} is called from the mod constructor. */
@SuppressWarnings("null")
public final class ArmoryRegistry {
    private ArmoryRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Singularity_Iteration.MOD_ID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
        DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Singularity_Iteration.MOD_ID);

    /** Thruster exhaust behind flying suit pieces (glowing, fades blue). */
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> THRUST =
        PARTICLES.register("armory_thrust", () -> new net.minecraft.core.particles.SimpleParticleType(false));
    /** White-hot shock-diamond core right at the nozzle. */
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> CORE =
        PARTICLES.register("armory_core", () -> new net.minecraft.core.particles.SimpleParticleType(true));
    /** Thin compressed-air vapour trail left behind a flying piece. */
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> TRAIL =
        PARTICLES.register("armory_trail", () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static final DeferredBlock<ArmoryBlock> ARMORY = BLOCKS.register("producer/block_armory",
        () -> new ArmoryBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 30.0F)
            .sound(mio_icif_sounds.getMachineSoundType()).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ARMORY_ITEM = ITEMS.register("producer/block_armory",
        () -> new BlockItem(ARMORY.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredBlock<ArmorShowcaseBlock> SHOWCASE = BLOCKS.register("producer/block_armor_showcase",
        () -> new ArmorShowcaseBlock(Block.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 12.0F)
            .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion().lightLevel(state -> 7)));
    public static final DeferredItem<BlockItem> SHOWCASE_ITEM = ITEMS.register("producer/block_armor_showcase",
        () -> new BlockItem(SHOWCASE.get(), new Item.Properties()));

    public static final DeferredItem<ArmoryRemoteItem> REMOTE = ITEMS.register("normal/item_armory_remote",
        () -> new ArmoryRemoteItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ArmoryConnectorItem> CONNECTOR = ITEMS.register("normal/item_armory_connector",
        () -> new ArmoryConnectorItem(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_armory>> ARMORY_ENTITY =
        BLOCK_ENTITIES.register("armory", () -> BlockEntityType.Builder.of(mio_icif_armory::new, ARMORY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmorShowcaseBlockEntity>> SHOWCASE_ENTITY =
        BLOCK_ENTITIES.register("armor_showcase", () -> BlockEntityType.Builder.of(ArmorShowcaseBlockEntity::new, SHOWCASE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ArmoryMenu>> ARMORY_MENU = MENUS.register("armory_menu",
        () -> new MenuType<>((IContainerFactory<ArmoryMenu>) ArmoryMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<EntityType<?>, EntityType<ArmoryPieceEntity>> PIECE_ENTITY = ENTITIES.register("armory_piece",
        () -> EntityType.Builder.<ArmoryPieceEntity>of(ArmoryPieceEntity::new, MobCategory.MISC)
            .sized(0.6F, 0.6F).clientTrackingRange(10).updateInterval(5).fireImmune().noSummon().build("armory_piece"));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        ArmoryComponents.COMPONENTS.register(modBus);
        modBus.addListener(ArmoryRegistry::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EUApi.SIDED, ARMORY_ENTITY.get(), (entity, side) ->
            entity instanceof AbstractEnergyBlockEntity energy ? energy.getEnergyStorageCapability(side) : null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARMORY_ENTITY.get(), (entity, side) ->
            entity.getItemHandlerCapability(side));
        ArmoryMana.register(event, ARMORY_ENTITY.get());
    }
}
