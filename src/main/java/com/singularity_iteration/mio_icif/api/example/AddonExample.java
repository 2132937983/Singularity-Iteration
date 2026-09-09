package com.singularity_iteration.mio_icif.api.example;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.IEnergyNetAPI;
import com.singularity_iteration.mio_icif.api.machine.builder.IElectricMachineBuilder;
import com.singularity_iteration.mio_icif.api.machine.builder.IGeneratorBuilder;
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeRegistrationAPI;
import com.singularity_iteration.mio_icif.api.registry.IMioIcifRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

/**
 * 附属模组开发示例
 *
 * <p>这个类展示了如何使用 mio_icif API 来创建自定义内容。
 * 这只是一个示例，不会实际注册任何内容。
 */
public class AddonExample {

    /**
     * 示例：创建自定义电力机器
     */
    public static void createCustomMachine() {
        MioIcifAPI api = MioIcifAPI.instance();
        IMachineBuilderAPI builder = api.getMachineBuilderAPI();

        // 使用 Builder 模式创建机器
        IElectricMachineBuilder machineBuilder = builder.createElectricMachineBuilder()
            .setName("super_compressor")
            .setTranslationKey("block.mymod.super_compressor")
            .useDefaultsForTier(api.getEnergyNetAPI().getCableTier("hv"))
            .setProcessTime(50)
            .setRecipeType(api.getRecipeAPI().getCompressorRecipeType())
            .useStandardLayout(2, 2, true, 4)
            .setSupportsUpgrades(true)
            .onWorkStart((world, pos) -> {
                // 自定义工作开始逻辑
                System.out.println("Super Compressor started at " + pos);
            })
            .onWorkComplete((world, pos, output) -> {
                // 自定义工作完成逻辑
                System.out.println("Super Compressor completed: " + output);
            });

        // 获取机器配置（用于在模组初始化阶段注册 BlockEntityType）
        IMachineBuilderAPI.MachineConfiguration config = machineBuilder.getConfiguration();
        System.out.println("Machine config: " + config.name() + ", slots: " + config.getTotalSlots());

        // 构建并注册机器定义（注意：BlockEntityType 为 null，需要自行注册）
        machineBuilder.buildAndRegister("mymod");
    }

    /**
     * 示例：创建自定义发电机
     */
    public static void createCustomGenerator() {
        MioIcifAPI api = MioIcifAPI.instance();
        IMachineBuilderAPI builder = api.getMachineBuilderAPI();

        builder.createGeneratorBuilder()
            .setName("quantum_solar")
            .setGenerationRate(8192)  // 8192 EU/t
            .setEnergyCapacity(10000000)
            .setCableTier(api.getEnergyNetAPI().getCableTier("iv"))
            .setGeneratorType(IGeneratorBuilder.GeneratorType.SOLAR)
            .setRequiresSky(true)
            .setDayOnly(false)  // 日夜都工作
            .setBaseEfficiency(100.0)
            .addBatterySlot(0)
            .onGenerate((world, pos, amount) -> {
                // 自定义发电逻辑
            })
            .buildAndRegister("mymod");
    }

    /**
     * 示例：注册自定义配方
     */
    public static void registerCustomRecipes() {
        MioIcifAPI api = MioIcifAPI.instance();
        IRecipeRegistrationAPI recipeAPI = api.getRecipeRegistrationAPI();

        // 注册压缩机配方
        recipeAPI.registerCompressorRecipe(
            ResourceLocation.fromNamespaceAndPath("mymod", "compress_diamond"),
            Ingredient.of(Items.DIAMOND),
            new ItemStack(Items.DIAMOND_BLOCK),
            100
        );

        // 注册提取机配方
        recipeAPI.registerExtractorRecipe(
            ResourceLocation.fromNamespaceAndPath("mymod", "extract_lapis"),
            Ingredient.of(Items.LAPIS_LAZULI),
            new ItemStack(Items.LAPIS_LAZULI, 2),
            150
        );

        // 注意：运行时注册的配方需要在 ServerStartedEvent 中同步到 RecipeManager
        // 这确保了新注册的配方能够被游戏正确识别和使用
        recipeAPI.syncToRecipeManager();
    }

    /**
     * 示例：查询电网状态
     */
    public static void queryEnergyNet(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        MioIcifAPI api = MioIcifAPI.instance();
        IEnergyNetAPI energyApi = api.getEnergyNetAPI();

        // 检查位置是否有电网节点
        if (energyApi.hasEnergyTile(world, pos)) {
            var stats = energyApi.getNodeStats(world, pos);
            if (stats != null) {
                System.out.println("Energy at " + pos + ": " + stats);
            }

            // 获取连接方向
            var connections = energyApi.getConnections(world, pos);
            System.out.println("Connections: " + connections);
        }
    }

    /**
     * 示例：使用注册表 API 查询内容
     */
    public static void queryRegistries() {
        MioIcifAPI api = MioIcifAPI.instance();
        IMioIcifRegistries registries = api.getRegistries();

        // 获取所有电池
        var batteries = registries.getItemsByCategory(IMioIcifRegistries.ItemCategory.BATTERY);
        System.out.println("Batteries: " + batteries.size());

        // 获取所有电缆
        var cables = registries.getBlocksByCategory(IMioIcifRegistries.BlockCategory.CABLE);
        System.out.println("Cables: " + cables.size());

        // 获取特定方块
        var hull = registries.getBlock("machine_hull_basic");
        System.out.println("Basic Hull: " + hull);

        // 获取所有以 "circuit" 开头的物品
        var circuits = registries.getItemsByPrefix("circuit");
        System.out.println("Circuits: " + circuits.size());
    }

    /**
     * 示例：使用能力系统
     */
    public static void useCapabilities(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
        MioIcifAPI api = MioIcifAPI.instance();

        // 创建 EU 存储能力实例
        var euStorage = api.getCapabilities().createEUStorage(
            100000,  // 容量
            512,     // 最大接收
            512,     // 最大提取
            api.getEnergyNetAPI().getCableTier("mv")
        );

        System.out.println("Created EU Storage: " + euStorage.getStored() + " / " + euStorage.getCapacity());

        // 从世界中的方块获取 EU 存储能力
        var blockEuStorage = world.getCapability(
            com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities.EU_STORAGE_BLOCK,
            pos,
            null
        );

        if (blockEuStorage != null) {
            long stored = blockEuStorage.getStored();
            long capacity = blockEuStorage.getCapacity();
            System.out.println("Energy: " + stored + " / " + capacity);
        }
    }

    /**
     * 示例：使用等级默认模板
     */
    public static void useTierDefaults() {
        MioIcifAPI api = MioIcifAPI.instance();
        IMachineBuilderAPI builder = api.getMachineBuilderAPI();

        // 获取 MV 等级的默认参数
        var defaults = builder.getDefaultsForTier(api.getEnergyNetAPI().getCableTier("mv"));
        System.out.println("MV Capacity: " + defaults.getDefaultCapacity());
        System.out.println("MV Energy/t: " + defaults.getDefaultEnergyPerTick());
        System.out.println("MV Process Time: " + defaults.getDefaultProcessTime());
        System.out.println("MV Upgrade Slots: " + defaults.getDefaultUpgradeSlots());
    }
}