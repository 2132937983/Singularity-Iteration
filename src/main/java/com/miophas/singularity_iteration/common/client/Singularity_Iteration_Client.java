package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.registry.mio_icif_items;
import com.miophas.singularity_iteration.common.client.item.DynamicCellColorProvider;
import com.miophas.singularity_iteration.common.client.item.DynamicCellFluidProperty;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.client.item.BatteryEnergyProperty;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.client.item.NanoSaberActiveProperty;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_items_tools;
import com.miophas.singularity_iteration.common.client.render.mio_icif_EnergyBulletRenderer;
import com.miophas.singularity_iteration.common.client.render.mio_icif_IC_TNT_Renderer;
import com.miophas.singularity_iteration.common.client.render.mio_icif_LaserBullet;
import com.miophas.singularity_iteration.common.client.render.mio_icif_PlasmaBullet;
import com.miophas.singularity_iteration.common.client.render.mio_icif_Nuke_Renderer;
import com.miophas.singularity_iteration.common.client.render.mio_icif_RotorRender;
import com.miophas.singularity_iteration.common.client.render.mio_icif_WaterRotorRender;
import com.miophas.singularity_iteration.common.client.render.mio_icif_dynamite_renderer;
import com.miophas.singularity_iteration.common.client.render.mio_icif_sticky_dynamite_renderer;
import com.miophas.singularity_iteration.common.client.render.mio_icif_RocketRenderer;
import com.miophas.singularity_iteration.common.client.render.entity.mio_icif_boat_renderer;
import com.miophas.singularity_iteration.common.entity.mio_icif_entities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Singularity_Iteration.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings({"null"})
public class Singularity_Iteration_Client {
    public Singularity_Iteration_Client(ModContainer container, net.neoforged.bus.api.IEventBus modEventBus) {
        com.miophas.singularity_iteration.common.client.screen.mio_icif_screens.register(modEventBus);
        // 0.1.8.0: the manual is a code-built GuideME guide (reactor simulator tag); only with GuideME loaded
        if (net.neoforged.fml.ModList.get().isLoaded("guideme")) {
            com.miophas.singularity_iteration.common.client.guide.GuideMeIntegration.register();
        }
        // 0.1.8.0: Create ponder scenes; only with the Ponder library (bundled in Create 6)
        if (net.neoforged.fml.ModList.get().isLoaded("ponder")) {
            com.miophas.singularity_iteration.common.client.ponder.PonderIntegration.register();
        }
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.SPEC);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Singularity_Iteration.LOGGER.debug("HELLO FROM CLIENT SETUP");
        // 0.1.9.1: registry lookup for the machine contents of crafting remainders on the client side
        com.miophas.singularity_iteration.common.block.MachineContents.clientLookup =
            () -> Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
        
        // Register the battery energy property. The battery texture changes with the energy level.
    ResourceLocation energyProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "energy_level");
        BatteryEnergyProperty propertyFunction = new BatteryEnergyProperty();
        
        // Register the standard battery series.
    ItemProperties.register(mio_icif_normal.BAT_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册高级电池系列
        ItemProperties.register(mio_icif_normal.ADVBAT_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册三级电池系列
        ItemProperties.register(mio_icif_normal.CHARGEBAT_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册四级电池系列
        ItemProperties.register(mio_icif_normal.ADVCHARGEBAT_0.get(), energyProperty, propertyFunction);
        
        // 注册五级电池（水晶电池）系列
        ItemProperties.register(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册六级电池（羊驼水晶电池）系列
        ItemProperties.register(mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册能量水晶系列
        ItemProperties.register(mio_icif_normal.CRYSTAL_LEV0.get(), energyProperty, propertyFunction);
        
        // 注册蓝波顿水晶系列
        ItemProperties.register(mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get(), energyProperty, propertyFunction);

        // 注册METS移植电池系列
        ItemProperties.register(mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get(), energyProperty, propertyFunction);
        ItemProperties.register(mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get(), energyProperty, propertyFunction);
        ItemProperties.register(mio_icif_normal.LITHIUM_BATTERY.get(), energyProperty, propertyFunction);
        ItemProperties.register(mio_icif_normal.ADV_LITHIUM_BATTERY.get(), energyProperty, propertyFunction);
        ItemProperties.register(mio_icif_normal.THORIUM_BATTERY.get(), energyProperty, propertyFunction);

        // 注册METS移植饰品模型属性
        ResourceLocation ringEnergyProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "ring_energy_value");
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            try {
                Class<?> curiosIntegrationClass = Class.forName("com.miophas.singularity_iteration.common.integration.CuriosIntegration");
                java.lang.reflect.Field lifeSupportRingField = curiosIntegrationClass.getField("TRINKET_LIFE_SUPPORT_RING");
                net.neoforged.neoforge.registries.DeferredItem<?> lifeSupportRing = (net.neoforged.neoforge.registries.DeferredItem<?>) lifeSupportRingField.get(null);
                Class<?> lifeSupportRingPropertyClass = Class.forName("com.miophas.singularity_iteration.common.client.input.LifeSupportRingProperty");
                @SuppressWarnings("deprecation")
                ItemPropertyFunction lifeSupportRingProperty = (ItemPropertyFunction) lifeSupportRingPropertyClass.getDeclaredConstructor().newInstance();
                ItemProperties.register(lifeSupportRing.get(), ringEnergyProperty, lifeSupportRingProperty);
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.error("Failed to register Life Support Ring property", e);
            }
        }

        Singularity_Iteration.LOGGER.debug("Battery energy property registered successfully");

        // Register the nano saber active property. The texture changes between the on state and the off state.
    ResourceLocation nanoSaberActiveProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nanosaber_active");
        ItemProperties.register(mio_icif_items_tools.NANO_SABER.get(), nanoSaberActiveProperty, new NanoSaberActiveProperty());
        Singularity_Iteration.LOGGER.debug("Nano Saber active property registered successfully");
        // 0.1.10.4: the quantum sword's 3D model and icon turn red in hyper mode
        ItemProperties.register(mio_icif_items_tools.QUANTUM_SWORD.get(), ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_sword_hyper"),
            (stack, level, entity, seed) -> com.miophas.singularity_iteration.common.item.tools.mio_icif_quantum_sword.isHyperState(stack) ? 1.0F : 0.0F);

        ResourceLocation cellTypeProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "cell_type");
        ItemProperties.register(mio_icif_cells.CELL_EMPTY.get(), cellTypeProperty, new DynamicCellFluidProperty());
        Singularity_Iteration.LOGGER.debug("Dynamic cell fluid property registered successfully");
    }
    
    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 && level != null && pos != null
                && level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.common.blockentity.mio_icif_wire wire
                ? wire.getCableColor().getTextureDiffuseColor() : 0xFF1D1D21,
            mio_icif_blocks.WIRE_ISOLATION_LV.get(), mio_icif_blocks.WIRE_ISOLATION_MV.get(),
            mio_icif_blocks.WIRE_ISOLATION_HV.get(), mio_icif_blocks.WIRE_ISOLATION_HV_2.get(),
            mio_icif_blocks.WIRE_ISOLATION_EV.get(), mio_icif_blocks.WIRE_ISOLATION_EV_2.get(),
            mio_icif_blocks.WIRE_ISOLATION_EV_3.get(), mio_icif_blocks.WIRE_IV.get());
        event.register((state, level, pos, tint) -> tint == 0
            ? ((com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock) state.getBlock()).grade().color() : -1,
            mio_icif_blocks.FLUID_PIPE_BRONZE.get(), mio_icif_blocks.FLUID_PIPE_STEEL.get());
        // Register the rubber leaf color handler. The leaves use the foliage color of the biome.
    event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            if (level == null || pos == null) {
                // 默认颜色
                return FoliageColor.getDefaultColor();
            }
            // Use the average foliage color of the biome.
            return BiomeColors.getAverageFoliageColor(level, pos);
        }, mio_icif_blocks.BLOCK_RUBBER_LEAF.get());
        Singularity_Iteration.LOGGER.debug("Rubber leaves block color handler registered");
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Cable items use precolored item/generated textures. Their layer0 has
        // tint index 0 too, so registering the block's color-band tint here
        // would multiply the entire inventory icon by black.
        for (var entry : mio_icif_blocks.FLUID_PIPE_ITEMS) {
            event.register((stack, tint) -> tint == 0
                ? ((com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock) entry.get().getBlock()).grade().color() : -1,
                entry.get());
        }
        // 注册橡胶树树叶物品颜色处理器
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            // 物品栏中显示默认 foliage 颜色
            return FoliageColor.getDefaultColor();
        }, mio_icif_blocks.BLOCK_RUBBER_LEAF.get().asItem());
        Singularity_Iteration.LOGGER.debug("Rubber leaves item color handler registered");

        event.register(new DynamicCellColorProvider(), mio_icif_cells.CELL_EMPTY.get());
        Singularity_Iteration.LOGGER.debug("Dynamic cell color handler registered");
    }

    @SubscribeEvent
    static void registerReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
            com.miophas.singularity_iteration.common.client.render.BakedArmorModel.invalidate();
            com.miophas.singularity_iteration.common.client.render.CoreAttachmentModel.invalidate();
        });
    }

    /**
     * Blockbench armor: nano suit (0.1.7.34), quantum suit (0.1.8.0), and since 0.1.8.3 the
     * advanced quantum chestplate, the heavy quantum (gravity) chestplate and the four solar
     * helmets. One baked geometry per piece, shared by all wearers. The held and dropped item
     * draw the same 3D model; the inventory shows the flat icon (item model "gui" perspective).
     */
    private static void registerNanoArmor(RegisterClientExtensionsEvent event) {
        String[] names = {"nano_helmet", "nano_chestplate", "nano_leggings", "nano_boots",
            "quantum_helmet", "quantum_chestplate", "quantum_leggings", "quantum_boots",
            "advanced_quantum_chestplate", "heavy_quantum_chestplate",
            "solar_helmet", "advanced_solar_helmet", "hybrid_solar_helmet", "ultimate_solar_helmet"};
        net.minecraft.world.item.Item[] items = {
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_NANO_HELMET.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_NANO_BOOTS.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_ADVANCED_QUANTUM_CHESTPLATE.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_HEAVY_QUANTUM_CHESTPLATE.get(),
            com.miophas.singularity_iteration.common.item.normal.mio_icif_normal.SOLAR_HELMET.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_ADVANCED_SOLAR_HELMET.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_HYBRID_SOLAR_HELMET.get(),
            com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors.ARMOR_ULTIMATE_SOLAR_HELMET.get()};
        for (int i = 0; i < names.length; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_models/" + names[i] + ".json");
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/models/armor/" + names[i] + ".png");
            event.registerItem(new IClientItemExtensions() {
                private com.miophas.singularity_iteration.common.client.render.BakedArmorModel model;
                private com.miophas.singularity_iteration.common.client.render.BakedArmorItemRenderer icon;
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (icon == null) icon = new com.miophas.singularity_iteration.common.client.render.BakedArmorItemRenderer(id, texture);
                    return icon;
                }
                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot, HumanoidModel<?> original) {
                    if (model == null) model = new com.miophas.singularity_iteration.common.client.render.BakedArmorModel(id);
                    return model;
                }
                /** 0.1.8.0 suit palette: layer 0 = plates (primary), layer 1 = light strips (accent) or skipped. */
                @Override
                public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, net.minecraft.world.item.ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
                    return com.miophas.singularity_iteration.common.suit.SuitPalette.tint(stack, layerIdx);
                }
            }, items[i]);
        }
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        registerNanoArmor(event);
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.parse("mio_icif:block/fluids/weed_ex_still"); }
            @Override public ResourceLocation getFlowingTexture() { return getStillTexture(); }
        }, mio_icif_fluids.WEED_EX_TYPE.get());
        // Register the client extensions for the biogas fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.BIOGAS_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.BIOGAS_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.BIOGAS_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Biogas fluid client extensions registered");

        // Register the client extensions for the hot water fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.HOTWATER_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.HOTWATER_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.HOTWATER_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Hotwater fluid client extensions registered");

        // 注册生物质流体客户端扩展
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.BIOMASS_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.BIOMASS_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.BIOMASS_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Biomass fluid client extensions registered");

        // Register the client extensions for the construction foam fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.CONSTRUCTIONFOAM_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.CONSTRUCTIONFOAM_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.CONSTRUCTIONFOAM_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Construction foam fluid client extensions registered");

        // 注册冷却液流体客户端扩展
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.COOLANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.COOLANT_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.COOLANT_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Coolant fluid client extensions registered");

        // 注册蒸馏水流体客户端扩展
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.DISTILLEDWATER_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.DISTILLEDWATER_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.DISTILLEDWATER_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Distilled water fluid client extensions registered");

        // Register the client extensions for the hot coolant fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.HOTCOOLANT_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.HOTCOOLANT_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.HOTCOOLANT_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Hot coolant fluid client extensions registered");

        // Register the client extensions for the pahoehoe lava fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.PAHOEHOELAVA_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.PAHOEHOELAVA_STILL_TEXTURE;
            }
        }, mio_icif_fluids.PAHOEHOELAVA_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Pahoehoe lava fluid client extensions registered");

        // Register the client extensions for the steam fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.STEAM_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.STEAM_STILL_TEXTURE;
            }
        }, mio_icif_fluids.STEAM_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Steam fluid client extensions registered");

        // Register the client extensions for the superheated steam fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.SUPERHEATEDSTEAM_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.SUPERHEATEDSTEAM_STILL_TEXTURE;
            }
        }, mio_icif_fluids.SUPERHEATEDSTEAM_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Superheated steam fluid client extensions registered");

        // Register the client extensions for the UU matter fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.UUMATTER_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.UUMATTER_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.UUMATTER_TYPE.get());
        Singularity_Iteration.LOGGER.debug("UU matter fluid client extensions registered");

        // Register the client extensions for the compressed air fluid.
    event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.AIR_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.AIR_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.AIR_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Air fluid client extensions registered");

        // 注册原油流体客户端扩展
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.CRUDEOIL_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.CRUDEOIL_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.CRUDEOIL_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Crude oil fluid client extensions registered");

        // 注册柴油流体客户端扩展
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return mio_icif_fluids.DIESELOIL_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return mio_icif_fluids.DIESELOIL_FLOWING_TEXTURE;
            }
        }, mio_icif_fluids.DIESELOIL_TYPE.get());
        Singularity_Iteration.LOGGER.debug("Diesel oil fluid client extensions registered");

        // Every two-handed gun: low-ready carry, shouldered aim when firing, first-person recoil.
        // The item models' hand transforms are anchored on each gun's grip (0.1.7.29).
        event.registerItem(com.miophas.singularity_iteration.common.client.item.LaserGunPose.INSTANCE,
            mio_icif_items_tools.TOOL_LASER_MINER.get(),
            mio_icif_items_tools.ELECTRIC_RIFLE.get(),
            mio_icif_items_tools.ADVANCED_ELECTRIC_RIFLE.get(),
            mio_icif_items_tools.TACTICAL_LASER_RIFLE.get(),
            mio_icif_items_tools.TACHYON_DISRUPTOR.get(),
            mio_icif_items_tools.ELECTRIC_PLASMA_GUN.get(),
            mio_icif_items_tools.PLASMA_AIR_CANNON.get(),
            mio_icif_items_tools.ROCKET_LAUNCHER.get());

        // 注册青铜剑客户端扩展 - 设置第三人称手臂姿势为BLOCK（剑的握持姿势）
        event.registerItem(new IClientItemExtensions() {
            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return HumanoidModel.ArmPose.BLOCK;
            }
        }, mio_icif_items.TOOL_BRONZE_SWORD.get());
        Singularity_Iteration.LOGGER.debug("Bronze sword client extensions registered");

        // 注册纳米剑客户端扩展 - 设置第三人称手臂姿势为BLOCK（剑的握持姿势）
        event.registerItem(new IClientItemExtensions() {
            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return HumanoidModel.ArmPose.BLOCK;
            }

            /** 0.1.8.5 HF sync (nano myofiber + active saber): the blade trembles at high frequency in first person. */
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm, ItemStack itemInHand, float partialTick,
                                                   float equipProcess, float swingProcess) {
                if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.nanoHfJitter()
                        && com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.hfJitter(player, itemInHand)) {
                    long n = System.nanoTime() / 9_000_000L;           // ~110 Hz pattern, stable within a frame
                    float jx = ((n * 0x9E3779B1L >>> 7) % 1000) / 1000F - 0.5F, jy = ((n * 0x85EBCA77L >>> 9) % 1000) / 1000F - 0.5F;
                    // 0.1.8.6: stronger sync, harder tremble (levels 1..3)
                    float amp = 0.7F + 0.3F * com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.hfLevel();
                    poseStack.translate(jx * 0.006F * amp, jy * 0.006F * amp, 0);
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(jx * 0.6F * amp));
                }
                return false;
            }
        }, mio_icif_items_tools.NANO_SABER.get());
        Singularity_Iteration.LOGGER.debug("Nano saber client extensions registered");

        // 注册钢制防爆盾客户端扩展 - 设置盾牌渲染器和举盾姿势
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new com.miophas.singularity_iteration.common.client.render.mio_icif_ShieldRenderer();
            }

            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (entityLiving.isUsingItem() && entityLiving.getUsedItemHand() == hand) {
                    return HumanoidModel.ArmPose.BLOCK;
                }
                return null;
            }
        }, mio_icif_items_tools.STEEL_SHIELD.get());
        Singularity_Iteration.LOGGER.debug("Steel shield client extensions registered");

        // 注册盾牌的 blocking 属性（用于模型切换）
        ItemProperties.register(
            mio_icif_items_tools.STEEL_SHIELD.get(),
            ResourceLocation.withDefaultNamespace("blocking"),
            (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && ItemStack.matches(entity.getUseItem(), stack) ? 1.0F : 0.0F
        );
        Singularity_Iteration.LOGGER.debug("Steel shield blocking property registered");

        // Nano bow draw state (0.1.8.3). The bow is an electric item: every energy sync replaces the
        // held stack, so the old identity test (getUseItem() == stack) flipped between the drawn and
        // the idle model while charging (the bow flickered). The test is now by item, and "pull" is
        // the arrow charge (full at the same tick as the shot power), not the vanilla 20-tick draw.
        ItemProperties.register(
            mio_icif_items_tools.NANO_BOW.get(),
            ResourceLocation.withDefaultNamespace("pull"),
            (stack, level, entity, seed) -> isDrawingNanoBow(entity, stack)
                ? com.miophas.singularity_iteration.common.item.tools.mio_icif_nano_bow.drawProgress(entity.getTicksUsingItem()) : 0.0F
        );
        ItemProperties.register(
            mio_icif_items_tools.NANO_BOW.get(),
            ResourceLocation.withDefaultNamespace("pulling"),
            (stack, level, entity, seed) -> isDrawingNanoBow(entity, stack) ? 1.0F : 0.0F
        );
        Singularity_Iteration.LOGGER.debug("Nano bow draw properties registered");

    }



    /** True while {@code entity} draws a nano bow and {@code stack} is that bow (same item, any stack copy). */
    private static boolean isDrawingNanoBow(@Nullable LivingEntity entity, ItemStack stack) {
        if (entity == null || !entity.isUsingItem()) return false;
        ItemStack used = entity.getUseItem();
        return used.is(mio_icif_items_tools.NANO_BOW.get()) && (used == stack || ItemStack.isSameItem(used, stack));
    }

    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 0.1.8.0: resin collector fluid animation (tray level and stream)
        event.registerBlockEntityRenderer(mio_icif_block_entities.RESIN_COLLECTOR.get(),
            com.miophas.singularity_iteration.common.client.render.ResinCollectorRenderer::new);
        // 注册风力动能发生机转子渲染器
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_RotorRender::new
        );
        Singularity_Iteration.LOGGER.debug("Wind kinetic generator rotor renderer registered");

        // 注册水力动能发生机转子渲染器
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_WaterRotorRender::new
        );
        Singularity_Iteration.LOGGER.debug("Water kinetic generator rotor renderer registered");

        // Register the crop block entity renderer.
    event.registerBlockEntityRenderer(
            mio_icif_block_entities.CROP_ENTITY_TYPE.get(),
            com.miophas.singularity_iteration.common.client.render.CropEntityRenderer::new
        );
        Singularity_Iteration.LOGGER.debug("Crop entity renderer registered");

        // 注册激光子弹实体渲染器
        event.registerEntityRenderer(
            mio_icif_entities.LASER_BULLET.get(),
            mio_icif_LaserBullet::new
        );
        Singularity_Iteration.LOGGER.debug("Laser bullet entity renderer registered");

        // 注册等离子子弹实体渲染器
        event.registerEntityRenderer(
            mio_icif_entities.PLASMA_BULLET.get(),
            mio_icif_PlasmaBullet::new
        );
        Singularity_Iteration.LOGGER.debug("Plasma bullet entity renderer registered");

        // Register the renderer for the primed industrial TNT entity.
    event.registerEntityRenderer(
            mio_icif_entities.IC_TNT_PRIMED.get(),
            mio_icif_IC_TNT_Renderer::new
        );
        Singularity_Iteration.LOGGER.debug("IC TNT primed entity renderer registered");

        // Register the renderer for the primed nuke entity.
    event.registerEntityRenderer(
            mio_icif_entities.NUKE_PRIMED.get(),
            mio_icif_Nuke_Renderer::new
        );
        Singularity_Iteration.LOGGER.debug("Nuke primed entity renderer registered");

        // 注册建筑泡沫方块实体渲染器（用于遮蔽器伪装纹理）
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.FOAM_ENTITY_TYPE.get(),
            com.miophas.singularity_iteration.common.client.render.FoamEntityRenderer::new
        );
        Singularity_Iteration.LOGGER.debug("Foam entity renderer registered");

        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIRELESS_POWER_TRANSMISSION_NODE.get(),
            com.miophas.singularity_iteration.common.client.render.WirelessTransmissionNodeRenderer::new
        );
        Singularity_Iteration.LOGGER.debug("Wireless transmission node renderer registered");

        // 注册基础/高级采矿机工作范围边界渲染器
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.MINER_ELC_ENTITY_TYPE.get(),
            com.miophas.singularity_iteration.common.client.render.mio_icif_MinerRangeRenderer.provider()
        );
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.ADVANCED_MINER_ELC_ENTITY_TYPE.get(),
            com.miophas.singularity_iteration.common.client.render.mio_icif_MinerRangeRenderer.provider()
        );
        Singularity_Iteration.LOGGER.debug("Miner range renderer registered");

        // 注册电线方块实体渲染器（用于泡沫化电线的遮蔽器伪装纹理渲染）
        com.miophas.singularity_iteration.common.client.render.WireDisguiseRenderer.registerAll(event);
        Singularity_Iteration.LOGGER.debug("Wire disguise renderer registered");

        event.registerEntityRenderer(
            mio_icif_entities.ELECTRIC_BOAT.get(),
            context -> new mio_icif_boat_renderer(context, mio_icif_boat_renderer.ELECTRIC_BOAT_LAYER)
        );

        event.registerEntityRenderer(
            mio_icif_entities.RUBBER_BOAT.get(),
            context -> new mio_icif_boat_renderer(context, mio_icif_boat_renderer.RUBBER_BOAT_LAYER)
        );

        event.registerEntityRenderer(
            mio_icif_entities.CARBON_BOAT.get(),
            context -> new mio_icif_boat_renderer(context, mio_icif_boat_renderer.CARBON_BOAT_LAYER)
        );

        event.registerEntityRenderer(
            mio_icif_entities.DYNAMITE_ENTITY.get(),
            mio_icif_dynamite_renderer::new
        );

        event.registerEntityRenderer(
            mio_icif_entities.STICKY_DYNAMITE_ENTITY.get(),
            mio_icif_sticky_dynamite_renderer::new
        );

        event.registerEntityRenderer(
            mio_icif_entities.ENERGY_BULLET.get(),
            mio_icif_EnergyBulletRenderer::new
        );

        event.registerEntityRenderer(
            mio_icif_entities.ROCKET_ENTITY.get(),
            mio_icif_RocketRenderer::new
        );
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(mio_icif_boat_renderer.CARBON_BOAT_LAYER, net.minecraft.client.model.BoatModel::createBodyModel);
        event.registerLayerDefinition(mio_icif_boat_renderer.RUBBER_BOAT_LAYER, net.minecraft.client.model.BoatModel::createBodyModel);
        event.registerLayerDefinition(mio_icif_boat_renderer.ELECTRIC_BOAT_LAYER, net.minecraft.client.model.BoatModel::createBodyModel);
        Singularity_Iteration.LOGGER.debug("Boat layer definitions registered");
    }

}
