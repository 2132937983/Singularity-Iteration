package com.miophas.singularity_iteration.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration file.
 * It contains the settings for the futures machine and other features. */
@SuppressWarnings("null")
public class Singularity_Iteration_Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== Futures machine settings ====================
    private static final ModConfigSpec.Builder FUTURE_BUILDER = BUILDER.push("FutureMarket");

    // Futures commodities moved to datapacks in 0.1.7.32: data/<namespace>/si_futures/*.json
    // (see FutureCommodityLoader). The old "commodities" list here is ignored and removed.

    // Maximum number of items that a player can trade each day.
public static final ModConfigSpec.IntValue FUTURE_MAX_DAILY_ITEMS = FUTURE_BUILDER
            .comment("Maximum number of items traded each day", "The total number of items that a player can buy or sell each day")
            .defineInRange("maxDailyItems", 100, 1, 10000);

    // Daily trade limit. This is an alias of FUTURE_MAX_DAILY_ITEMS.
public static final ModConfigSpec.IntValue FUTURE_DAILY_LIMIT = FUTURE_MAX_DAILY_ITEMS;

    // 每页显示货品数量
    public static final ModConfigSpec.IntValue FUTURE_ITEMS_PER_PAGE = FUTURE_BUILDER
            .comment("每页显示货品数量")
            .defineInRange("itemsPerPage", 5, 1, 20);

    // Energy that each trade uses.
    public static final ModConfigSpec.IntValue FUTURE_ENERGY_PER_TRADE = FUTURE_BUILDER
            .comment("Energy that each trade uses (EU)")
            .defineInRange("energyPerTrade", 10, 0, 10000);

    // Energy capacity of the futures machine.
public static final ModConfigSpec.IntValue FUTURE_ENERGY_CAPACITY = FUTURE_BUILDER
            .comment("Energy capacity of the futures machine (EU)")
            .defineInRange("energyCapacity", 10000, 1000, 1000000);

    // Maximum energy input of the futures machine.
public static final ModConfigSpec.IntValue FUTURE_MAX_RECEIVE = FUTURE_BUILDER
            .comment("Maximum energy input of the futures machine (EU/t)")
            .defineInRange("maxReceive", 100, 1, 10000);

    // Minimum price multiplier, relative to the base price.
public static final ModConfigSpec.DoubleValue FUTURE_MIN_PRICE_MULTIPLIER = FUTURE_BUILDER
            .comment("Minimum price multiplier", "The lowest price, as a multiple of the base price", "Example: 0.1 sets the lowest price to 10% of the base price")
            .defineInRange("minPriceMultiplier", 0.1, 0.01, 1.0);

    // Maximum price multiplier, relative to the base price.
public static final ModConfigSpec.DoubleValue FUTURE_MAX_PRICE_MULTIPLIER = FUTURE_BUILDER
            .comment("Maximum price multiplier", "The highest price, as a multiple of the base price", "Example: 10.0 sets the highest price to 10 times the base price")
            .defineInRange("maxPriceMultiplier", 10.0, 1.0, 100.0);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder FUTURE_POP = BUILDER.pop();
    // ==================== End of futures machine settings ====================

    // ==================== Nuclear explosion settings ====================
    private static final ModConfigSpec.Builder NUCLEAR_BUILDER = BUILDER.push("NuclearExplosion");

    // 核爆地形毁伤总开关；已存在的污染地形与辐射区不会被回滚。
    public static final ModConfigSpec.BooleanValue ENABLE_NUCLEAR_EXPLOSION = NUCLEAR_BUILDER
            .comment("是否启用核弹及核反应堆的核爆地形毁伤",
                     "false：新爆炸退化为局部普通爆炸，仅移除装置自身，不破坏周围地形",
                     "已排队的压力毁伤与辐射转化暂停；重新开启后继续，已有污染及辐射区不回滚")
            .define("enableNuclearExplosion", true);
    public static final ModConfigSpec.DoubleValue NUKE_EXPLOSION_MULTIPLIER = NUCLEAR_BUILDER
            .comment("核弹当量倍率，只影响核弹，不影响反应堆及采矿镭射枪",
                     "倍率作用于原装药当量；保留当量 1000000、毁伤半径 2000 的上限",
                     "0：禁用核弹的核爆部分，退化为不破坏周围地形的局部普通爆炸")
            .defineInRange("nukeExplosionMultiplier", 1.0, 0.0, 16.0);

    public static final ModConfigSpec.DoubleValue REACTOR_EXPLOSION_MULTIPLIER = NUCLEAR_BUILDER
            .comment("反应堆爆炸倍率，只影响反应堆；基础半径为 8 + 4 × 有效燃料单元数",
                     "先应用倍率与隔板减免，再受 reactorExplosionPowerLimit 限制；0 暂停事故及高温环境效果",
                     "已提交事故固定保存当时威力，调整非零倍率不会重算该次事故")
            .defineInRange("reactorExplosionMultiplier", 1.0, 0.0, 16.0);
    public static final ModConfigSpec.IntValue REACTOR_EXPLOSION_POWER_LIMIT = NUCLEAR_BUILDER
            .comment("反应堆爆炸威力上限；最大水平半径为此值的两倍",
                     "默认 45 对应最大半径 90 格；0 暂停事故及高温环境效果，已提交事故不重算")
            .defineInRange("reactorExplosionPowerLimit", 45, 0, 1000);

    // 蘑菇云粒子数量倍率
    public static final ModConfigSpec.BooleanValue NUCLEAR_FLASH = NUCLEAR_BUILDER
            .comment("Enable transient nuclear explosion screen flash")
            .define("nuclearExplosionFlash", true);
    public static final ModConfigSpec.BooleanValue NUCLEAR_CAMERA_SHAKE = NUCLEAR_BUILDER
            .comment("Enable transient shock-front camera shake")
            .define("nuclearExplosionCameraShake", true);

    public static final ModConfigSpec.DoubleValue MUSHROOM_CLOUD_PARTICLE_MULTIPLIER = NUCLEAR_BUILDER
            .comment("Nuclear cloud density: 1.0 = 768 parcels, 0 disables the cloud",
                     "Capped at 1536 parcels per cloud, four simultaneous clouds per client")
            .defineInRange("mushroomCloudParticleMultiplier", 1.0, 0.0, 10.0);

    // 采矿镭射枪是否启用超高能爆破模式
    public static final ModConfigSpec.BooleanValue ENABLE_LASER_NUCLEAR_EXPLOSIVE = NUCLEAR_BUILDER
            .comment("采矿镭射枪是否启用超高能爆破模式",
                    "true = 启用超高能爆破模式，玩家可以切换到该模式并使用镭射枪产生极强爆炸",
                    "false = 禁止该模式发射，已有该模式弹丸也不会引爆",
                    "超高能爆破模式会产生100倍TNT当量的爆炸，具有极大的破坏力")
            .define("enableLaserNuclearExplosive", false);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder NUCLEAR_POP = BUILDER.pop();
    // ==================== End of nuclear explosion settings ====================

    // ==================== Terrain transformer settings ====================
    private static final ModConfigSpec.Builder TERRA_BUILDER = BUILDER.push("TerraELC");

    // Number of blocks that the terrain transformer processes each tick.
public static final ModConfigSpec.IntValue TERRA_BLOCKS_PER_TICK = TERRA_BUILDER
            .comment("Number of blocks that the terrain transformer processes each tick",
                    "A larger value makes the conversion faster, but it can cause lag",
                    "Recommended maximum: 500")
            .defineInRange("blocksPerTick", 100, 1, 1000);

    // Range (radius) of the terrain transformer.
public static final ModConfigSpec.IntValue TERRA_RANGE = TERRA_BUILDER
            .comment("Range of the terrain transformer (radius)",
                    "The radius of the sphere around the machine",
                    "Default: 128 blocks (256 blocks diameter)")
            .defineInRange("range", 128, 16, 256);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder TERRA_POP = BUILDER.pop();
    // ==================== End of terrain transformer settings ====================

    // ==================== Scanner settings ====================
    private static final ModConfigSpec.Builder SCANNER_BUILDER = BUILDER.push("Scanner");

    // UU物质消耗倍率
    public static final ModConfigSpec.DoubleValue SCANNER_UU_MULTIPLIER = SCANNER_BUILDER
            .comment("旧版扫描机 UU 倍率：兼容保留，当前独立 UU 定价与扫描流程不读取此项")
            .defineInRange("uuMultiplier", 1.0, 0.01, 10000.0);

    // EU能量消耗倍率
    public static final ModConfigSpec.DoubleValue SCANNER_EU_MULTIPLIER = SCANNER_BUILDER
            .comment("旧版扫描机 EU 倍率：兼容保留，当前扫描固定为 3300 tick × 256 EU/t，不读取此项")
            .defineInRange("euMultiplier", 1.0, 0.01, 10000.0);

    // Custom UU/EU cost overrides for items.
public static final ModConfigSpec.ConfigValue<List<? extends String>> SCANNER_CUSTOM_COSTS = SCANNER_BUILDER
            .comment("Legacy scanner cost overrides. This setting is kept for compatibility. The current UU pricing and scan process do not read it.",
                    "Format: itemID = (UU cost, EU cost)",
                    "Example: minecraft:diamond = (10000, 5000000) sets the diamond cost to 10000 mB UU and 5000000 EU",
                    "These entries override the automatically calculated costs")
            .defineListAllowEmpty("customCosts",
                    List.of(
                            // Example entries. The default list is empty. Users add their own entries.
                ),
                    () -> "minecraft:stone = (100, 10000)",
                    Singularity_Iteration_Config::validateCustomCostConfig);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder SCANNER_POP = BUILDER.pop();
    // ==================== End of scanner settings ====================

    // ==================== 电网配置 ====================
    private static final ModConfigSpec.Builder ENERGY_NET_BUILDER = BUILDER.push("EnergyNet");

    // 是否启用不同电压等级导致的过载效果（电线熔毁/机器爆炸）
    public static final ModConfigSpec.BooleanValue ENERGY_NET_ENABLE_VOLTAGE_OVERLOAD = ENERGY_NET_BUILDER
            .comment("是否启用不同电压等级导致的过载效果",
                    "true = 启用过载效果，当电线或机器接收到超过其电压等级的能量时会被熔毁或爆炸",
                    "false = 禁用过载效果，电线和机器不会因电压不匹配而损坏，但仍会正常传输能量",
                    "服务端配置重载后生效；COMMON 配置不会自动同步到客户端")
            .define("enableVoltageOverload", true);

    // 0.1.8.0: native EU distribution algorithm.
    public static final ModConfigSpec.ConfigValue<String> ENERGY_NET_DISTRIBUTION_MODE = ENERGY_NET_BUILDER
            .comment("电网分配算法 / Native EU distribution algorithm",
                    "fair = 确定性均等分配（智能电网）：负载与变压器优先于储电设备；同一阶段的每台设备获得相同的需求满足比例。默认值。",
                    "fair = deterministic fair share (smart grid). Loads and transformers first, storage last. Equal satisfaction ratio. Default.",
                    "legacy = 0.1.7.x 的 IC2 式数据包优先级分配 / the IC2-style packet priority walk of 0.1.7.x")
            .define("distributionMode", "fair", value -> "fair".equals(value) || "legacy".equals(value));

    public static final ModConfigSpec.ConfigValue<String> ENERGY_NET_LINE_LOSS = ENERGY_NET_BUILDER
            .comment("线路损耗模型（仅 fair 模式）/ Line loss model (fair mode only)",
                    "lossless = 无线损。默认值。/ No cable loss. Default.",
                    "proportional = 损耗 = 能量 × 电缆损耗 / 数据包电压；电压越高损耗比例越低。",
                    "proportional = loss = energy * cable loss / packet voltage. A higher voltage has a lower loss fraction.")
            .define("lineLossModel", "lossless", value -> "lossless".equals(value) || "proportional".equals(value));

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder ENERGY_NET_POP = BUILDER.pop();
    // ==================== 电网配置结束 ====================

    // ==================== 采矿机工作范围显示配置 ====================
    private static final ModConfigSpec.Builder MINER_RANGE_BUILDER = BUILDER.push("MinerRange");

    // 是否渲染采矿机工作范围边界
    public static final ModConfigSpec.BooleanValue MINER_RANGE_DISPLAY_ENABLED = MINER_RANGE_BUILDER
            .comment("是否渲染采矿机工作范围边界",
                    "true = 在基础/高级采矿机四周渲染黄黑相间的工作范围垂直边界（类似边境之墙）",
                    "false = 关闭该渲染效果",
                    "边界范围由机器内安装的扫描器决定")
            .define("enableRangeDisplay", true);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder MINER_RANGE_POP = BUILDER.pop();
    // ==================== 采矿机工作范围显示配置结束 ====================

    private static final ModConfigSpec.Builder REPRESSURIZER_BUILDER = BUILDER.push("SteamRepressurizer");
    public static final ModConfigSpec.IntValue REPRESSURIZER_STEAM_OUTPUT = REPRESSURIZER_BUILDER
        .comment("Output ordinary steam (mB) per 10 mB steam + 1 HU; IC2 default: 16")
        .defineInRange("steamPerSteam", 16, 1, 10000);
    public static final ModConfigSpec.IntValue REPRESSURIZER_SUPERHEATED_OUTPUT = REPRESSURIZER_BUILDER
        .comment("Output ordinary steam (mB) per 10 mB superheated steam + 1 HU; IC2 default: 32")
        .defineInRange("steamPerSuperSteam", 32, 1, 10000);
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder REPRESSURIZER_POP = BUILDER.pop();

    // ==================== 0.1.8.4: Blueprint Builder UU supply ====================
    private static final ModConfigSpec.Builder BUILDER_BUILDER = BUILDER.push("BlueprintBuilder");
    public static final ModConfigSpec.BooleanValue BUILDER_REPLICATOR_SUPPLY = BUILDER_BUILDER
        .comment("When no Material Interface has a material, the Blueprint Builder orders it from an idle Replicator",
                 "in range that has the pattern (memory crystal or adjacent Pattern Storage). The Replicator pays the copy",
                 "with its own UU matter and energy at the usual price.")
        .translation("mio_icif.configuration.builder_replicator_supply")
        .define("replicatorSupply", true);
    public static final ModConfigSpec.IntValue BUILDER_REPLICATOR_RANGE = BUILDER_BUILDER
        .comment("Distance in blocks in which the Blueprint Builder finds Replicators.")
        .translation("mio_icif.configuration.builder_replicator_range")
        .defineInRange("replicatorRange", 16, 4, 64);
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder BUILDER_POP = BUILDER.pop();

    // ==================== 0.1.8.4: generator output ====================
    private static final ModConfigSpec.Builder GENERATOR_BUILDER = BUILDER
        .comment("Output multiplier of each EU generator (1.0 = stock EU/t). Fuel burns at the same speed,",
                 "so the multiplier is also the EU per unit of fuel. 0 switches the generator off.")
        .push("GeneratorOutput");
    public static final java.util.Map<String, ModConfigSpec.DoubleValue> GENERATOR_OUTPUT =
        com.miophas.singularity_iteration.common.energy.GeneratorOutput.define(GENERATOR_BUILDER);
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder GENERATOR_POP = BUILDER.pop();

    // ==================== 0.1.8.5: nano suit units ====================
    private static final ModConfigSpec.Builder NANO_BUILDER = BUILDER
        .comment("Nano suit units (0.1.8.5): unit switches, PvP switches, nano core and burst numbers.")
        .push("NanoSuit");
    static {
        com.miophas.singularity_iteration.common.suit.nano.NanoConfig.define(NANO_BUILDER);
    }
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder NANO_POP = BUILDER.pop();

    // ==================== 0.1.9.0: frame cores ====================
    private static final ModConfigSpec.Builder CORE_BUILDER = BUILDER
        .comment("Frame cores (0.1.9.0): core switches and the arc discharge of the advanced propulsion core.")
        .push("FrameCores");
    static {
        com.miophas.singularity_iteration.common.suit.core.CoreConfig.define(CORE_BUILDER);
    }
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder CORE_POP = BUILDER.pop();

    // ==================== 0.1.10.0: quantum suit actions ====================
    private static final ModConfigSpec.Builder QUANTUM_BUILDER = BUILDER
        .comment("Quantum suit (0.1.10.0): action switches, PvP switches, coherence and action numbers, single-player slow motion.")
        .push("QuantumSuit");
    static {
        com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig.define(QUANTUM_BUILDER);
    }
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder QUANTUM_POP = BUILDER.pop();

    static final ModConfigSpec SPEC = BUILDER.build();

    static {
        com.miophas.singularity_iteration.common.energy.GeneratorOutput.bind(SPEC);
        com.miophas.singularity_iteration.common.suit.nano.NanoConfig.bind(SPEC);
        com.miophas.singularity_iteration.common.suit.core.CoreConfig.bind(SPEC);
        com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig.bind(SPEC);
        com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity.OUTPUT_SCALE =
            com.miophas.singularity_iteration.common.energy.GeneratorOutput::multiplier;
    }

    /**
     * Validates the format of a commodity type entry.
     * Format: itemID,base price,volatility,category
     */


    /**
     * Validates one custom cost entry.
     * Format: itemID = (UU cost, EU cost)
     */
    private static boolean validateCustomCostConfig(final Object obj) {
        if (!(obj instanceof String config)) {
            return false;
        }

        // Format: minecraft:item = (1000, 500000)
        String[] parts = config.split("=");
        if (parts.length != 2) {
            return false;
        }

        // Validate the item ID.
        String itemId = parts[0].trim();
        ResourceLocation resourceId=ResourceLocation.tryParse(itemId);
        if (resourceId==null||!BuiltInRegistries.ITEM.containsKey(resourceId)) {
            return false;
        }

        // Validate the cost format (UU, EU).
        String costPart = parts[1].trim();
        if (!costPart.startsWith("(") || !costPart.endsWith(")")) {
            return false;
        }

        String costContent = costPart.substring(1, costPart.length() - 1);
        String[] costs = costContent.split(",");
        if (costs.length != 2) {
            return false;
        }

        try {
            long uuCost = Long.parseLong(costs[0].trim());
            long euCost = Long.parseLong(costs[1].trim());
            if (uuCost < 0 || euCost < 0) return false;
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * Gets the custom cost entries as a map.
     */
    public static java.util.Map<String, CostConfig> getCustomCostConfigs() {
        java.util.Map<String, CostConfig> configs = new java.util.HashMap<>();
        List<? extends String> configStrings = SCANNER_CUSTOM_COSTS.get();

        for (String configStr : configStrings) {
            String[] parts = configStr.split("=");
            if (parts.length == 2) {
                String itemId = parts[0].trim();
                String costPart = parts[1].trim();
                if (costPart.startsWith("(") && costPart.endsWith(")")) {
                    String costContent = costPart.substring(1, costPart.length() - 1);
                    String[] costs = costContent.split(",");
                    if (costs.length == 2) {
                        try {
                            long uuCost = Long.parseLong(costs[0].trim());
                            long euCost = Long.parseLong(costs[1].trim());
                            configs.put(itemId, new CostConfig(uuCost, euCost));
                        } catch (NumberFormatException e) {
                            // Ignore an invalid entry.
                        }
                    }
                }
            }
        }

        return configs;
    }

    /**
     * Data class for one cost entry.
     */
    public static class CostConfig {
        private final long uuCost;
        private final long euCost;

        public CostConfig(long uuCost, long euCost) {
            this.uuCost = uuCost;
            this.euCost = euCost;
        }

        public long getUuCost() {
            return uuCost;
        }

        public long getEuCost() {
            return euCost;
        }
    }
}
