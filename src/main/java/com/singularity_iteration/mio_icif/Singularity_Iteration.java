package com.singularity_iteration.mio_icif;

import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_items_armors;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactors;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_capacities;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_tool_menus;
import com.singularity_iteration.mio_icif.Screen.mio_icif_screens;
import com.singularity_iteration.mio_icif.effect.mio_icif_effects;
import com.singularity_iteration.mio_icif.energy.grid.GridEventHandler;
import com.singularity_iteration.mio_icif.network.mio_icif_Network;
import com.singularity_iteration.mio_icif.particle.mio_icif_Particles;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import com.singularity_iteration.mio_icif.recipe.mio_icif_IngredientTypes;
import com.singularity_iteration.mio_icif.uu.UuIndex;
import com.singularity_iteration.mio_icif.uu.UuScanValues;
import com.singularity_iteration.mio_icif.world.feature.WorldGeneration;
import com.singularity_iteration.mio_icif.world.feature.mio_icif_foliage_placers;
import com.singularity_iteration.mio_icif.world.feature.mio_icif_tree_decorators;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_iridium_driller;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.Items.Lathe.mio_icif_lathe_items;
import com.singularity_iteration.mio_icif.Items.mio_icif_creativeTabs;
import com.singularity_iteration.mio_icif.entity.mio_icif_entities;
import com.singularity_iteration.mio_icif.entity.villager.mio_icif_villagers;

import org.slf4j.Logger;
import com.singularity_iteration.mio_icif.Items.mio_icif_items;
import com.mojang.logging.LogUtils;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import java.nio.file.Path;
import java.util.Set;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class Singularity_Iteration {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mio_icif";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Singularity_Iteration(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        // Register items
        mio_icif_items.register(modEventBus);

        // Register lathe items
        mio_icif_lathe_items.register(modEventBus);

        // Register resources
        mio_icif_resources.register(modEventBus);
        
        // Register tools
        mio_icif_items_tools.register(modEventBus);

        // Register trinkets (Curios API integration) - only if Curios is loaded
        // Use reflection to avoid loading CuriosIntegration class when Curios is not installed
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            try {
                Class<?> curiosIntegrationClass = Class.forName("com.singularity_iteration.mio_icif.integration.CuriosIntegration");
                java.lang.reflect.Method registerMethod = curiosIntegrationClass.getMethod("register", net.neoforged.bus.api.IEventBus.class);
                registerMethod.invoke(null, modEventBus);
            } catch (Exception e) {
                LOGGER.error("Failed to register Curios integration", e);
            }
        }

        // Register normal items
        mio_icif_normal.register(modEventBus);

        // Register cells
        mio_icif_cells.register(modEventBus);

        // Register armors
        mio_icif_items_armors.register(modEventBus);

        // Register reactor items
        mio_icif_reactors.register(modEventBus);

        // Register blocks
        mio_icif_blocks.register(modEventBus);
        


        // Register sounds
        mio_icif_sounds.register(modEventBus);
        
        // Register block entities
        mio_icif_block_entities.register(modEventBus);

        // Register menus
        com.singularity_iteration.mio_icif.Menu.mio_icif_menus.register(modEventBus);

        // Register tool menus (for tools like OD scanner)
        mio_icif_tool_menus.register(modEventBus);

        // Register GUI screens
        mio_icif_screens.register(modEventBus);

        // Register creative tabs
        mio_icif_creativeTabs.register(modEventBus);

        // Register custom data components (e.g. BatteryEnergy for stackable crystals)
        com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components.DATA_COMPONENTS.register(modEventBus);

        // Register our mod's ConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Singularity_Iteration_Config.SPEC);
        
        // Register the gatherData method on the mod event bus
        // modEventBus.addListener(this::gatherData);

        // Register energy capabilities on the mod event bus
        mio_icif_capacities.register(modEventBus);

        // Register recipes
        mio_icif_ModRecipes.register(modEventBus);

        // Register custom ingredient types
        mio_icif_IngredientTypes.register(modEventBus);

        // Register tree decorators
        mio_icif_tree_decorators.register(modEventBus);

        // Register foliage placers
        mio_icif_foliage_placers.register(modEventBus);

        // Register fluids
        mio_icif_fluids.register(modEventBus);

        // Register entities
        mio_icif_entities.register(modEventBus);

        // Register network packets
        mio_icif_Network.init(modEventBus);

        // Register villagers
        mio_icif_villagers.register(modEventBus);

        // Register particles
        mio_icif_Particles.register(modEventBus);

        // Register effects
        mio_icif_effects.register(modEventBus);

        // 注册作物注册事件处理�

        modEventBus.register(com.singularity_iteration.mio_icif.event.CropRegistryEvent.class);

        // 初始化电网系统（IC2 风格 Node/Grid 架构）
        GridEventHandler.init();

        // 初始化 AE2 兼容层
        com.singularity_iteration.mio_icif.integration.ae2.Ae2Plugin.init();

        // 注册告示牌方块到 BlockEntityType
        modEventBus.addListener(this::onBlockEntityTypeAddBlocks);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(mio_icif_iridium_driller::onGetEnchantmentLevel);

        // 注册废料箱的发射器行�
com.singularity_iteration.mio_icif.Items.Normal.mio_icif_scrapbox.registerDispenserBehavior();
        com.singularity_iteration.mio_icif.Items.Normal.mio_icif_thorium_scrapbox.registerDispenserBehavior();

        // 注册炸药和粘性炸药的发射器行�
com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_dynamite_item.registerDispenserBehavior();
        com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_sticky_dynamite_item.registerDispenserBehavior();

        // 注册流体单元的发射器行为
        com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cell.registerDispenserBehaviors();

        // 注册橡胶木系列方块的燃烧属性
        registerRubberWoodFlammability();
    }

    private void registerRubberWoodFlammability() {
        FireBlock fireBlock = (FireBlock) Blocks.FIRE;

        // 原木系列 - 与橡木原木相同的燃烧属性 (encouragement: 5, flammability: 5)
        fireBlock.setFlammable(mio_icif_blocks.BLOCK_RUBBER_TREE.get(), 5, 5);
        fireBlock.setFlammable(mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get(), 5, 5);
        // 有胶橡胶木 - 与普通橡胶木相同的燃烧属性
        fireBlock.setFlammable(mio_icif_blocks.BLOCK_HAVE_RUB_WOOD.get(), 5, 5);

        // 树叶 - 与橡木树叶相同的燃烧属性 (encouragement: 30, flammability: 60)
        fireBlock.setFlammable(mio_icif_blocks.BLOCK_RUBBER_LEAF.get(), 30, 60);

        // 木板系列 - 与橡木相同的燃烧属性 (encouragement: 5, flammability: 20)
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_PLANKS.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_STAIRS.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_SLAB.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_FENCE.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_FENCE_GATE.get(), 5, 20);

        // 门、活板门、压力板、按钮 - 相同的燃烧属性
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_DOOR.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_TRAPDOOR.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_PRESSURE_PLATE.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_BUTTON.get(), 5, 20);

        // 告示牌 - 相同的燃烧属性
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_SIGN.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_WALL_SIGN.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_HANGING_SIGN.get(), 5, 20);
        fireBlock.setFlammable(mio_icif_blocks.RUBBER_WALL_HANGING_SIGN.get(), 5, 20);

        LOGGER.info("Registered rubber wood flammability");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting for Singularity Iteration");

        // 初始化UU物质记录系统
        MinecraftServer server = event.getServer();
        if (server != null) {
            // 获取已加载的overworld
            var overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld != null) {
                // 加载UU扫描值配置
                UuScanValues scanValues = new UuScanValues();
                scanValues.loadDefaultValues();

                // 从配置文件目录加载用户自定义值
                Path configDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("config").resolve(MOD_ID);
                scanValues.loadFromFile(configDir);

                // 初始化UU Index（配置值会在init内部先加载到图中）
                UuIndex.INSTANCE.init(overworld, scanValues);

                LOGGER.info("[UU] UU Matter system initialized.");
            } else {
                LOGGER.warn("[UU] Overworld not available, UU system initialization skipped.");
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Server stopping, cleaning up machine registry");
        // 清空静态机器注册表，防止内存泄漏
        com.singularity_iteration.mio_icif.api.machine.MachineAPIImpl.clearRegistry();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        com.singularity_iteration.mio_icif.command.ToggleCommand.register(event.getDispatcher());
    }

    private void onBlockEntityTypeAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        // 将橡胶木告示牌方块注册到 SIGN BlockEntityType
        event.modify(BlockEntityType.SIGN, 
            mio_icif_blocks.RUBBER_SIGN.get(),
            mio_icif_blocks.RUBBER_WALL_SIGN.get()
        );
        
        // 将橡胶木悬挂告示牌方块注册到 HANGING_SIGN BlockEntityType
        event.modify(BlockEntityType.HANGING_SIGN,
            mio_icif_blocks.RUBBER_HANGING_SIGN.get(),
            mio_icif_blocks.RUBBER_WALL_HANGING_SIGN.get()
        );
        
        LOGGER.info("Registered rubber wood sign blocks to BlockEntityType");
    }

}