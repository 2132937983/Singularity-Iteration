package com.singularity_iteration.mio_icif;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.mio_icif_items;
import com.singularity_iteration.mio_icif.Items.Cell.DynamicCellColorProvider;
import com.singularity_iteration.mio_icif.Items.Cell.DynamicCellFluidProperty;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Normal.BatteryEnergyProperty;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Tools.NanoSaberActiveProperty;
import com.singularity_iteration.mio_icif.Items.Tools.PlasmaLauncherActiveProperty;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.Render.mio_icif_EnergyBulletRenderer;
import com.singularity_iteration.mio_icif.Render.mio_icif_IC_TNT_Renderer;
import com.singularity_iteration.mio_icif.Render.mio_icif_LaserBullet;
import com.singularity_iteration.mio_icif.Render.mio_icif_PlasmaBullet;
import com.singularity_iteration.mio_icif.Render.mio_icif_Nuke_Renderer;
import com.singularity_iteration.mio_icif.Render.mio_icif_RotorRender;
import com.singularity_iteration.mio_icif.Render.mio_icif_WaterRotorRender;
import com.singularity_iteration.mio_icif.Render.mio_icif_dynamite_renderer;
import com.singularity_iteration.mio_icif.Render.mio_icif_sticky_dynamite_renderer;
import com.singularity_iteration.mio_icif.Render.mio_icif_RocketRenderer;
import com.singularity_iteration.mio_icif.entity.boat.mio_icif_boat_renderer;
import com.singularity_iteration.mio_icif.entity.mio_icif_entities;
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
    public Singularity_Iteration_Client(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Singularity_Iteration.LOGGER.info("HELLO FROM CLIENT SETUP");
        Singularity_Iteration.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        
        // 注册电池能量属性，用于动态纹理切�
    ResourceLocation energyProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "energy_level");
        BatteryEnergyProperty propertyFunction = new BatteryEnergyProperty();
        
        // 注册普通电池系�
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
                Class<?> curiosIntegrationClass = Class.forName("com.singularity_iteration.mio_icif.integration.CuriosIntegration");
                java.lang.reflect.Field lifeSupportRingField = curiosIntegrationClass.getField("TRINKET_LIFE_SUPPORT_RING");
                net.neoforged.neoforge.registries.DeferredItem<?> lifeSupportRing = (net.neoforged.neoforge.registries.DeferredItem<?>) lifeSupportRingField.get(null);
                Class<?> lifeSupportRingPropertyClass = Class.forName("com.singularity_iteration.mio_icif.integration.curios.LifeSupportRingProperty");
                @SuppressWarnings("deprecation")
                ItemPropertyFunction lifeSupportRingProperty = (ItemPropertyFunction) lifeSupportRingPropertyClass.getDeclaredConstructor().newInstance();
                ItemProperties.register(lifeSupportRing.get(), ringEnergyProperty, lifeSupportRingProperty);
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.error("Failed to register Life Support Ring property", e);
            }
        }

        Singularity_Iteration.LOGGER.info("Battery energy property registered successfully");

        // 注册纳米剑激活属性，用于开关状态纹理切�
    ResourceLocation nanoSaberActiveProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nanosaber_active");
        ItemProperties.register(mio_icif_items_tools.NANO_SABER.get(), nanoSaberActiveProperty, new NanoSaberActiveProperty());
        Singularity_Iteration.LOGGER.info("Nano Saber active property registered successfully");

        // 注册等离子射线枪激活属性，用于开关状态纹理切�
    ResourceLocation plasmaActiveProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "plasma_active");
        ItemProperties.register(mio_icif_items_tools.PLASMA_LAUNCHER.get(), plasmaActiveProperty, new PlasmaLauncherActiveProperty());
        Singularity_Iteration.LOGGER.info("Plasma Launcher active property registered successfully");

        ResourceLocation cellTypeProperty = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "cell_type");
        ItemProperties.register(mio_icif_cells.CELL_EMPTY.get(), cellTypeProperty, new DynamicCellFluidProperty());
        Singularity_Iteration.LOGGER.info("Dynamic cell fluid property registered successfully");
    }
    
    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        // 注册橡胶树树叶颜色处理器，使其根据生物群系显示颜�
    event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            if (level == null || pos == null) {
                // 默认颜色
                return FoliageColor.getDefaultColor();
            }
            // 获取生物群系�?foliage 颜色
            return BiomeColors.getAverageFoliageColor(level, pos);
        }, mio_icif_blocks.BLOCK_RUBBER_LEAF.get());
        Singularity_Iteration.LOGGER.info("Rubber leaves block color handler registered");
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // 注册橡胶树树叶物品颜色处理器
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            // 物品栏中显示默认 foliage 颜色
            return FoliageColor.getDefaultColor();
        }, mio_icif_blocks.BLOCK_RUBBER_LEAF.get().asItem());
        Singularity_Iteration.LOGGER.info("Rubber leaves item color handler registered");

        event.register(new DynamicCellColorProvider(), mio_icif_cells.CELL_EMPTY.get());
        Singularity_Iteration.LOGGER.info("Dynamic cell color handler registered");
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // 注册生物气体流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Biogas fluid client extensions registered");

        // 注册热水流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Hotwater fluid client extensions registered");

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
        Singularity_Iteration.LOGGER.info("Biomass fluid client extensions registered");

        // 注册建筑泡沫流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Construction foam fluid client extensions registered");

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
        Singularity_Iteration.LOGGER.info("Coolant fluid client extensions registered");

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
        Singularity_Iteration.LOGGER.info("Distilled water fluid client extensions registered");

        // 注册热冷却液流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Hot coolant fluid client extensions registered");

        // 注册熔岩流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Pahoehoe lava fluid client extensions registered");

        // 注册蒸汽流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Steam fluid client extensions registered");

        // 注册过热蒸汽流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Superheated steam fluid client extensions registered");

        // 注册UU物质流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("UU matter fluid client extensions registered");

        // 注册压缩空气流体客户端扩�
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
        Singularity_Iteration.LOGGER.info("Air fluid client extensions registered");

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
        Singularity_Iteration.LOGGER.info("Crude oil fluid client extensions registered");

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
        Singularity_Iteration.LOGGER.info("Diesel oil fluid client extensions registered");

        // 注册青铜剑客户端扩展 - 设置第三人称手臂姿势为BLOCK（剑的握持姿势）
        event.registerItem(new IClientItemExtensions() {
            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return HumanoidModel.ArmPose.BLOCK;
            }
        }, mio_icif_items.TOOL_BRONZE_SWORD.get());
        Singularity_Iteration.LOGGER.info("Bronze sword client extensions registered");

        // 注册纳米剑客户端扩展 - 设置第三人称手臂姿势为BLOCK（剑的握持姿势）
        event.registerItem(new IClientItemExtensions() {
            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return HumanoidModel.ArmPose.BLOCK;
            }
        }, mio_icif_items_tools.NANO_SABER.get());
        Singularity_Iteration.LOGGER.info("Nano saber client extensions registered");

        // 注册钢制防爆盾客户端扩展 - 设置盾牌渲染器和举盾姿势
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new com.singularity_iteration.mio_icif.Render.mio_icif_ShieldRenderer();
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
        Singularity_Iteration.LOGGER.info("Steel shield client extensions registered");

        // 注册盾牌的 blocking 属性（用于模型切换）
        ItemProperties.register(
            mio_icif_items_tools.STEEL_SHIELD.get(),
            ResourceLocation.withDefaultNamespace("blocking"),
            (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && ItemStack.matches(entity.getUseItem(), stack) ? 1.0F : 0.0F
        );
        Singularity_Iteration.LOGGER.info("Steel shield blocking property registered");

        // 注册纳米复合弓的 pull 属性（用于拉弓纹理切换）
        ItemProperties.register(
            mio_icif_items_tools.NANO_BOW.get(),
            ResourceLocation.withDefaultNamespace("pull"),
            (stack, level, entity, seed) -> {
                if (entity == null) {
                    return 0.0F;
                } else {
                    return entity.getUseItem().getItem() != mio_icif_items_tools.NANO_BOW.get() ? 0.0F
                            : (float) (entity.getUseItem().getItem().getUseDuration(stack, entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                }
            }
        );
        Singularity_Iteration.LOGGER.info("Nano bow pull property registered");

        // 注册纳米复合弓的 pulling 属性（用于拉弓状态切换）
        ItemProperties.register(
            mio_icif_items_tools.NANO_BOW.get(),
            ResourceLocation.withDefaultNamespace("pulling"),
            (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
        Singularity_Iteration.LOGGER.info("Nano bow pulling property registered");

    }



    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册风力动能发生机转子渲染器
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_RotorRender::new
        );
        Singularity_Iteration.LOGGER.info("Wind kinetic generator rotor renderer registered");

        // 注册水力动能发生机转子渲染器
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.WATER_KINETIC_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_WaterRotorRender::new
        );
        Singularity_Iteration.LOGGER.info("Water kinetic generator rotor renderer registered");

        // 注册作物方块实体渲染�
    event.registerBlockEntityRenderer(
            mio_icif_block_entities.CROP_ENTITY_TYPE.get(),
            com.singularity_iteration.mio_icif.client.render.CropEntityRenderer::new
        );
        Singularity_Iteration.LOGGER.info("Crop entity renderer registered");

        // 注册激光子弹实体渲染器
        event.registerEntityRenderer(
            mio_icif_entities.LASER_BULLET.get(),
            mio_icif_LaserBullet::new
        );
        Singularity_Iteration.LOGGER.info("Laser bullet entity renderer registered");

        // 注册等离子子弹实体渲染器
        event.registerEntityRenderer(
            mio_icif_entities.PLASMA_BULLET.get(),
            mio_icif_PlasmaBullet::new
        );
        Singularity_Iteration.LOGGER.info("Plasma bullet entity renderer registered");

        // 注册工业TNT点燃实体渲染�
    event.registerEntityRenderer(
            mio_icif_entities.IC_TNT_PRIMED.get(),
            mio_icif_IC_TNT_Renderer::new
        );
        Singularity_Iteration.LOGGER.info("IC TNT primed entity renderer registered");

        // 注册核弹点燃实体渲染�
    event.registerEntityRenderer(
            mio_icif_entities.NUKE_PRIMED.get(),
            mio_icif_Nuke_Renderer::new
        );
        Singularity_Iteration.LOGGER.info("Nuke primed entity renderer registered");

        // 注册建筑泡沫方块实体渲染器（用于遮蔽器伪装纹理）
        event.registerBlockEntityRenderer(
            mio_icif_block_entities.FOAM_ENTITY_TYPE.get(),
            com.singularity_iteration.mio_icif.client.render.FoamEntityRenderer::new
        );
        Singularity_Iteration.LOGGER.info("Foam entity renderer registered");

        // 注册电线方块实体渲染器（用于泡沫化电线的遮蔽器伪装纹理渲染）
        com.singularity_iteration.mio_icif.client.render.WireDisguiseRenderer.registerAll(event);
        Singularity_Iteration.LOGGER.info("Wire disguise renderer registered");

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
        Singularity_Iteration.LOGGER.info("Boat layer definitions registered");
    }

}