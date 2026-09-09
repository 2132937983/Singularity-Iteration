package com.singularity_iteration.mio_icif;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置文件
 * 包含期货机等功能的配方? */
@SuppressWarnings("null")
public class Singularity_Iteration_Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 期货机配方?====================
    private static final ModConfigSpec.Builder FUTURE_BUILDER = BUILDER.push("FutureMarket");

    // 期货机货品种类配置（格式：物�?ID，基础价格，波动率，种类）
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FUTURE_COMMODITIES = FUTURE_BUILDER
            .comment("期货机货品种类配方?",
                    "格式：物�?ID，基础价格，波动率，种�?",
                    "例如：minecraft:coal,10,0.3,mineral 表示煤炭，基础价格 10，波动率 30%，种类为矿产",
                    "波动率范围：0.0-1.0，表示价格每天可能波动的百分配?",
                    "可选种类：mineral(矿产), agriculture(农产�?, wood(木材), food(食物), other(其他)")
            .defineListAllowEmpty("commodities", 
                    List.of(
                            // 矿产�
                        "minecraft:copper_ingot,120,0.12,mineral",
                            "minecraft:iron_ingot,200,0.10,mineral",
                            "minecraft:gold_ingot,400,0.18,mineral",
                            "minecraft:diamond,800,0.22,mineral",
                            "minecraft:emerald,600,0.15,mineral",
                            "minecraft:lapis_lazuli,80,0.08,mineral",
                            "minecraft:redstone,60,0.06,mineral",
                            "minecraft:coal,40,0.05,mineral",
                            "minecraft:raw_iron,160,0.10,mineral",
                            "minecraft:raw_copper,100,0.12,mineral",
                            "minecraft:raw_gold,320,0.18,mineral",
                            "minecraft:ancient_debris,2000,0.28,mineral",
                            "minecraft:netherite_ingot,4000,0.30,mineral",
                            // 农产品类
                            "minecraft:wheat,30,0.05,agriculture",
                            "minecraft:carrot,20,0.04,agriculture",
                            "minecraft:potato,20,0.04,agriculture",
                            "minecraft:beetroot,20,0.04,agriculture",
                            "minecraft:pumpkin,40,0.06,agriculture",
                            "minecraft:melon_slice,10,0.03,agriculture",
                            "minecraft:sugar_cane,10,0.03,agriculture",
                            "minecraft:cactus,10,0.03,agriculture",
                            "minecraft:bamboo,10,0.03,agriculture",
                            "minecraft:cocoa_beans,30,0.05,agriculture",
                            "minecraft:sweet_berries,20,0.04,agriculture",
                            "minecraft:glow_berries,50,0.10,agriculture",
                            "minecraft:nether_wart,40,0.08,agriculture",
                            // 木材料
                        "minecraft:oak_log,20,0.03,wood",
                            "minecraft:spruce_log,20,0.03,wood",
                            "minecraft:birch_log,20,0.03,wood",
                            "minecraft:jungle_log,20,0.03,wood",
                            "minecraft:acacia_log,20,0.03,wood",
                            "minecraft:dark_oak_log,20,0.03,wood",
                            "minecraft:mangrove_log,20,0.03,wood",
                            "minecraft:cherry_log,30,0.05,wood",
                            "minecraft:crimson_stem,40,0.06,wood",
                            "minecraft:warped_stem,40,0.06,wood",
                            // 食物�
                        "minecraft:apple,30,0.04,food",
                            "minecraft:bread,50,0.05,food",
                            "minecraft:cooked_porkchop,70,0.06,food",
                            "minecraft:cooked_beef,80,0.06,food",
                            "minecraft:cooked_chicken,60,0.06,food",
                            "minecraft:cooked_mutton,70,0.06,food",
                            "minecraft:cooked_rabbit,50,0.05,food",
                            "minecraft:cooked_cod,50,0.05,food",
                            "minecraft:cooked_salmon,60,0.06,food",
                            "minecraft:cookie,20,0.04,food",
                            "minecraft:pumpkin_pie,60,0.06,food",
                            "minecraft:mushroom_stew,50,0.05,food",
                            "minecraft:rabbit_stew,80,0.07,food",
                            "minecraft:beetroot_soup,50,0.05,food",
                            "minecraft:honey_bottle,60,0.06,food",
                            "minecraft:cake,120,0.08,food",
                            "minecraft:golden_apple,1500,0.15,food",
                            "minecraft:enchanted_golden_apple,8000,0.20,food"
                    ), 
                    () -> "minecraft:coal,40,0.05,mineral", 
                    Singularity_Iteration_Config::validateCommodityConfig);

    // 每日最大交易物品数据
public static final ModConfigSpec.IntValue FUTURE_MAX_DAILY_ITEMS = FUTURE_BUILDER
            .comment("每日最大交易物品数据?", "玩家每天最多可以购买或出售的物品总数")
            .defineInRange("maxDailyItems", 100, 1, 10000);

    // 每日交易限制（别名，�?FUTURE_MAX_DAILY_ITEMS 相同步
public static final ModConfigSpec.IntValue FUTURE_DAILY_LIMIT = FUTURE_MAX_DAILY_ITEMS;

    // 每页显示货品数量
    public static final ModConfigSpec.IntValue FUTURE_ITEMS_PER_PAGE = FUTURE_BUILDER
            .comment("每页显示货品数量")
            .defineInRange("itemsPerPage", 5, 1, 20);

    // 每次交易消耗的能量
    public static final ModConfigSpec.IntValue FUTURE_ENERGY_PER_TRADE = FUTURE_BUILDER
            .comment("每次交易消耗的能量（EU）?")
            .defineInRange("energyPerTrade", 10, 0, 10000);

    // 期货机能量容纳
public static final ModConfigSpec.IntValue FUTURE_ENERGY_CAPACITY = FUTURE_BUILDER
            .comment("期货机能量容量（EU）?")
            .defineInRange("energyCapacity", 10000, 1000, 1000000);

    // 期货机最大接收能量
public static final ModConfigSpec.IntValue FUTURE_MAX_RECEIVE = FUTURE_BUILDER
            .comment("期货机最大接收能量（EU/t�?")
            .defineInRange("maxReceive", 100, 1, 10000);

    // 价格最小倍数（相对于基础价格�
public static final ModConfigSpec.DoubleValue FUTURE_MIN_PRICE_MULTIPLIER = FUTURE_BUILDER
            .comment("价格最小倍数", "价格最低可以跌到基础价格的多少�?", "例如 0.1 表示最低为基础价格�?0%")
            .defineInRange("minPriceMultiplier", 0.1, 0.01, 1.0);

    // 价格最大倍数（相对于基础价格�
public static final ModConfigSpec.DoubleValue FUTURE_MAX_PRICE_MULTIPLIER = FUTURE_BUILDER
            .comment("价格最大倍数", "价格最高可以涨到基础价格的多少�?", "例如 10.0 表示最高为基础价格?0?")
            .defineInRange("maxPriceMultiplier", 10.0, 1.0, 100.0);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder FUTURE_POP = BUILDER.pop();
    // ==================== 期货机配置结果?====================

    // ==================== 核爆炸视觉效果配方?====================
    private static final ModConfigSpec.Builder NUCLEAR_BUILDER = BUILDER.push("NuclearExplosion");

    // 是否启用核爆炸（破坏环境�
public static final ModConfigSpec.BooleanValue ENABLE_NUCLEAR_EXPLOSION = NUCLEAR_BUILDER
            .comment("是否启用核爆炸（破坏环境�?",
                    "true = 启用核爆炸，会大规模破坏地形并产生辐射区域?",
                    "false = 禁用核爆炸，仅产生普通机器爆炸效果，不破坏环�?",
                    "此选项同时影响核弹和核反应堆的爆炸")
            .define("enableNuclearExplosion", true);

    // 蘑菇云粒子数量倍率
    public static final ModConfigSpec.DoubleValue MUSHROOM_CLOUD_PARTICLE_MULTIPLIER = NUCLEAR_BUILDER
            .comment("蘑菇云粒子数量倍率",
                    "控制核爆炸蘑菇云效果的粒子数据?",
                    "1.0 = 默认数量�?.5 = 一半数量，2.0 = 双倍数据?",
                    "设置�?可以禁用蘑菇云粒子效果?")
            .defineInRange("mushroomCloudParticleMultiplier", 1.0, 0.0, 10.0);

    // 采矿镭射枪是否启用超高能爆破模式
    public static final ModConfigSpec.BooleanValue ENABLE_LASER_NUCLEAR_EXPLOSIVE = NUCLEAR_BUILDER
            .comment("采矿镭射枪是否启用超高能爆破模式",
                    "true = 启用超高能爆破模式，玩家可以切换到该模式并使用镭射枪产生极强爆炸",
                    "false = 禁用超高能爆破模式，切换模式时跳过该模式",
                    "超高能爆破模式会产生100倍TNT当量的爆炸，具有极大的破坏力")
            .define("enableLaserNuclearExplosive", false);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder NUCLEAR_POP = BUILDER.pop();
    // ==================== 核爆炸视觉效果配置结果?====================

    // ==================== 地形转换机配方?====================
    private static final ModConfigSpec.Builder TERRA_BUILDER = BUILDER.push("TerraELC");

    // 地形转换机每tick处理的方块数据
public static final ModConfigSpec.IntValue TERRA_BLOCKS_PER_TICK = TERRA_BUILDER
            .comment("地形转换机每tick处理的方块数据?",
                    "数值越大转换速度越快，但可能造成卡顿",
                    "建议范围�?0-500")
            .defineInRange("blocksPerTick", 100, 1, 1000);

    // 地形转换机作用范围（半径�
public static final ModConfigSpec.IntValue TERRA_RANGE = TERRA_BUILDER
            .comment("地形转换机作用范围（半径�?",
                    "以机器为中心的球形区域半�?",
                    "默认128格（直径256格）")
            .defineInRange("range", 128, 16, 256);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder TERRA_POP = BUILDER.pop();
    // ==================== 地形转换机配置结果?====================

    // ==================== 扫描机配方?====================
    private static final ModConfigSpec.Builder SCANNER_BUILDER = BUILDER.push("Scanner");

    // UU物质消耗倍率
    public static final ModConfigSpec.DoubleValue SCANNER_UU_MULTIPLIER = SCANNER_BUILDER
            .comment("UU物质消耗倍率", "基础消耗的乘数")
            .defineInRange("uuMultiplier", 1.0, 0.01, 10000.0);

    // EU能量消耗倍率
    public static final ModConfigSpec.DoubleValue SCANNER_EU_MULTIPLIER = SCANNER_BUILDER
            .comment("EU能量消耗倍率", "基础消耗的乘数")
            .defineInRange("euMultiplier", 1.0, 0.01, 10000.0);

    // 自定义物品UU/EU消耗覆�
public static final ModConfigSpec.ConfigValue<List<? extends String>> SCANNER_CUSTOM_COSTS = SCANNER_BUILDER
            .comment("自定义物品UU/EU消耗覆�?",
                    "格式: 物品ID = (UU消耗? EU消耗?",
                    "例如: minecraft:diamond = (10000, 5000000) 表示钻石需�?0000mB UU�?00万EU",
                    "这会覆盖自动计算的消耗�?")
            .defineListAllowEmpty("customCosts",
                    List.of(
                            // 示例配置（默认留空，由用户自定义�
                ),
                    () -> "minecraft:stone = (100, 10000)",
                    Singularity_Iteration_Config::validateCustomCostConfig);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder SCANNER_POP = BUILDER.pop();
    // ==================== 扫描机配置结果?====================

    // ==================== 电网配置 ====================
    private static final ModConfigSpec.Builder ENERGY_NET_BUILDER = BUILDER.push("EnergyNet");

    // 是否启用不同电压等级导致的过载效果（电线熔毁/机器爆炸）
    public static final ModConfigSpec.BooleanValue ENERGY_NET_ENABLE_VOLTAGE_OVERLOAD = ENERGY_NET_BUILDER
            .comment("是否启用不同电压等级导致的过载效果",
                    "true = 启用过载效果，当电线或机器接收到超过其电压等级的能量时会被熔毁或爆炸",
                    "false = 禁用过载效果，电线和机器不会因电压不匹配而损坏，但仍会正常传输能量",
                    "修改后需要重启游戏才能生效")
            .define("enableVoltageOverload", true);

    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder ENERGY_NET_POP = BUILDER.pop();
    // ==================== 电网配置结束 ====================

    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * 验证货品种类配置格式
     * 格式: 物品ID,基础价格,波动作?种类
     */
    private static boolean validateCommodityConfig(final Object obj) {
        if (!(obj instanceof String config)) {
            return false;
        }
        
        String[] parts = config.split(",");
        // 支持3�?个参数（旧格�?个，新格�?个带种类�
    if (parts.length < 3 || parts.length > 4) {
            return false;
        }
        
        // 验证物品ID
        String itemId = parts[0].trim();
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemId))) {
            return false;
        }
        
        // 验证基础价格
        try {
            int basePrice = Integer.parseInt(parts[1].trim());
            if (basePrice < 1) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        
        // 验证波动作
    try {
            float volatility = Float.parseFloat(parts[2].trim());
            if (volatility < 0.0f || volatility > 1.0f) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        
        // 验证种类（如果有�
    if (parts.length == 4) {
            String categoryId = parts[3].trim();
            // 检查是否是有效的种类ID
            boolean validCategory = false;
            for (String id : com.singularity_iteration.mio_icif.future.CommodityCategory.getAllIds()) {
                if (id.equalsIgnoreCase(categoryId)) {
                    validCategory = true;
                    break;
                }
            }
            if (!validCategory) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 获取解析后的货品种类配置
     */
    public static List<CommodityConfig> getCommodityConfigs() {
        List<CommodityConfig> configs = new ArrayList<>();
        List<? extends String> configStrings = FUTURE_COMMODITIES.get();
        
        for (String configStr : configStrings) {
            String[] parts = configStr.split(",");
            if (parts.length >= 3) {
                try {
                    String itemId = parts[0].trim();
                    int basePrice = Integer.parseInt(parts[1].trim());
                    float volatility = Float.parseFloat(parts[2].trim());
                    // 解析种类，默认为other
                    String categoryId = parts.length >= 4 ? parts[3].trim() : "other";
                    configs.add(new CommodityConfig(itemId, basePrice, volatility, categoryId));
                } catch (NumberFormatException e) {
                    // 忽略无效配置
                }
            }
        }
        
        return configs;
    }

    /**
     * 货品种类配置数据�
 */
    public static class CommodityConfig {
        private final String itemId;
        private final int basePrice;
        private final float volatility;
        private final String categoryId;

        public CommodityConfig(String itemId, int basePrice, float volatility, String categoryId) {
            this.itemId = itemId;
            this.basePrice = basePrice;
            this.volatility = volatility;
            this.categoryId = categoryId;
        }

        public String getItemId() {
            return itemId;
        }

        public int getBasePrice() {
            return basePrice;
        }

        public float getVolatility() {
            return volatility;
        }

        public String getCategoryId() {
            return categoryId;
        }
    }

    /**
     * 验证自定义消耗配置格�
 * 格式: 物品ID = (UU消耗? EU消耗?
     */
    private static boolean validateCustomCostConfig(final Object obj) {
        if (!(obj instanceof String config)) {
            return false;
        }

        // 格式: minecraft:item = (1000, 500000)
        String[] parts = config.split("=");
        if (parts.length != 2) {
            return false;
        }

        // 验证物品ID
        String itemId = parts[0].trim();
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemId))) {
            return false;
        }

        // 验证消耗值格�?(UU, EU)
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
     * 获取自定义消耗配置映�
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
                            // 忽略无效配置
                        }
                    }
                }
            }
        }

        return configs;
    }

    /**
     * 消耗配置数据类
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