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

    // 0.1.10.25 (X1): buying and selling have separate limits. Buying from the market burns coins and is limited by the
    // market's stock; selling is limited by the sell depth (below). Before, both shared "maxDailyItems" (100 items a day).
    public static final ModConfigSpec.IntValue FUTURE_BUY_ITEMS_PER_DAY = FUTURE_BUILDER
            .comment("Items a player can buy from the market each day, all commodities together (0.1.10.25). 0 = no limit (the market's stock limits it).",
                     "Before 0.1.10.25 \"maxDailyItems\" (100) counted buying and selling together.")
            .defineInRange("buyItemsPerDay", 0, 0, 1_000_000);

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

    // 0.1.10.20 (M1): the market quotes two prices, and buys only a limited amount of each commodity per day.
    public static final ModConfigSpec.DoubleValue FUTURE_SPREAD = FUTURE_BUILDER
            .comment("Bid-ask spread as a fraction of the price (0.1.10.20)",
                     "A purchase costs price x (1 + spread / 2); a sale pays price x (1 - spread / 2).",
                     "Before 0.1.10.20 both were the price (0), so buying low and selling after the price came back made coins without risk.")
            .defineInRange("spread", 0.06, 0.0, 0.5);
    // 0.1.10.25 (X1): the sell depth replaces "systemBuyPerDay" (256 items of a commodity a day for all players, 30 % fall).
    public static final ModConfigSpec.IntValue EXCHANGE_SELL_DEPTH_PLAYER = FUTURE_BUILDER
            .comment("Personal sell depth in items (0.1.10.25): each item a player sold of a commodity today lowers that player's bid by the factor exp(-1/depth).",
                     "One player can never take more than price x 0.97 / (1/player + 1/server) coins of one commodity in a day. A datapack commodity can set its own depth.")
            .defineInRange("sellDepthPlayer", 256, 1, 1_000_000);
    public static final ModConfigSpec.IntValue EXCHANGE_SELL_DEPTH_SERVER = FUTURE_BUILDER
            .comment("Server sell depth in items (0.1.10.25): every item sold of a commodity by anyone, not yet exported, lowers all bids by exp(-1/depth).",
                     "All players together (any number of accounts) can take at most price x 0.97 x depth coins of one commodity on the first day,",
                     "and about a quarter of that every day while the export share is 0.5.")
            .defineInRange("sellDepthServer", 1024, 1, 10_000_000);
    public static final ModConfigSpec.DoubleValue EXCHANGE_SELL_EXPORT = FUTURE_BUILDER
            .comment("Share of the sell pressure that the market passes on (exports) each game day (0.1.10.25); the bids recover by this share each day")
            .defineInRange("sellExportPerDay", 0.5, 0.05, 1.0);
    public static final ModConfigSpec.IntValue EXCHANGE_SELL_ALLOWANCE = FUTURE_BUILDER
            .comment("Coins a player can receive from the market for sales each day (0.1.10.25); unused coins carry over up to sellAllowanceDays days. 0 = no limit.",
                     "Coins from trades between players do not count.")
            .defineInRange("sellAllowancePerDay", 50_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue EXCHANGE_SELL_ALLOWANCE_DAYS = FUTURE_BUILDER
            .comment("Days of sellAllowancePerDay that a player can keep (0.1.10.25)")
            .defineInRange("sellAllowanceDays", 3, 1, 30);
    public static final ModConfigSpec.BooleanValue EXCHANGE_SELL_ALLOWANCE_SINGLEPLAYER = FUTURE_BUILDER
            .comment("Use sellAllowancePerDay in single player as well (0.1.10.25). Off by default: the depths alone limit a single player.")
            .define("sellAllowanceInSingleplayer", false);

    // 0.1.10.21 (M2-M5): the SI exchange.
    public static final ModConfigSpec.DoubleValue EXCHANGE_FEE = FUTURE_BUILDER
            .comment("Fee on trades between players, as a fraction of the price; the seller pays it and it is destroyed (0.1.10.21)")
            .defineInRange("exchangeFee", 0.01, 0.0, 0.2);
    public static final ModConfigSpec.IntValue EXCHANGE_IMPORT_PER_DAY = FUTURE_BUILDER
            .comment("Items of one commodity that the market can import each day when its treasury is empty (0.1.10.21)",
                     "The market sells its treasury stock (the items it bought) first. 0 = the market sells only what it owns.")
            .defineInRange("importPerDay", 64, 0, 100_000);
    public static final ModConfigSpec.DoubleValue EXCHANGE_IMPORT_PREMIUM = FUTURE_BUILDER
            .comment("Extra price of imported items over the purchase price (0.1.10.21)")
            .defineInRange("importPremium", 0.1, 0.0, 2.0);
    public static final ModConfigSpec.IntValue EXCHANGE_LISTING_REQUESTERS = FUTURE_BUILDER
            .comment("Different players whose commissions for an item are needed to list it on the exchange (multiplayer, 0.1.10.21)")
            .defineInRange("listingRequesters", 3, 1, 100);
    public static final ModConfigSpec.IntValue EXCHANGE_LISTING_FULFILLED = FUTURE_BUILDER
            .comment("Delivered commissions for an item within 7 days that list it (single player: on different days, 0.1.10.21)")
            .defineInRange("listingFulfilled", 3, 1, 100);
    public static final ModConfigSpec.IntValue EXCHANGE_LISTING_MIN_PLAY_MINUTES = FUTURE_BUILDER
            .comment("Play time a requester needs before their commissions count as demand for a listing (0.1.10.21)")
            .defineInRange("listingMinPlayMinutes", 120, 0, 100_000);
    public static final ModConfigSpec.IntValue EXCHANGE_UU_DEPTH = FUTURE_BUILDER
            .comment("Crafting depth from which a player listing is traded in UU-matter instead of coins (0.1.10.21)",
                     "Depth 0: no recipe makes the item; each processing step adds 1. 0 = always UU-matter, 99 = always coins.")
            .defineInRange("uuDepth", 3, 0, 99);
    // 0.1.10.26 (X3): the system bounties in three tiers replace "systemCommissionsPerDay" (3 market commissions a day, the
    // reward 1.15 x the ask paid in new coins). A datapack can set all of these in data/<ns>/si_exchange/economy.json; a
    // value changed here wins over the datapack.
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_MIN = FUTURE_BUILDER
            .comment("NORMAL bounties the board keeps open: at least this many on a day (0.1.10.26). The number for a day is drawn from bountyNormalMin..bountyNormalMax.")
            .defineInRange("bountyNormalMin", 3, 0, 20);
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_MAX = FUTURE_BUILDER
            .comment("NORMAL bounties the board keeps open: at most this many on a day (0.1.10.26)")
            .defineInRange("bountyNormalMax", 5, 0, 20);
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_VALUE = FUTURE_BUILDER
            .comment("Value in coins of a generated NORMAL request (0.1.10.26): each one asks for value x 0.5..1.5 at the market's ask,",
                     "bountyNormalMinQuantity..bountyNormalMaxQuantity items (a commodity outside that range is not asked for).")
            .defineInRange("bountyNormalValue", 4000, 10, 100_000_000);
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_DAYS = FUTURE_BUILDER
            .comment("Days a NORMAL bounty stays open (0.1.10.26)")
            .defineInRange("bountyNormalDays", 3, 1, 30);
    public static final ModConfigSpec.DoubleValue BOUNTY_NORMAL_MULTIPLIER = FUTURE_BUILDER
            .comment("Reward of a NORMAL bounty as a multiple of the value of what it asks for (0.1.10.26)")
            .defineInRange("bountyNormalMultiplier", 1.15, 0.1, 10.0);
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_MIN_QUANTITY = FUTURE_BUILDER
            .comment("Fewest items a generated NORMAL request asks for (0.1.10.26)")
            .defineInRange("bountyNormalMinQuantity", 16, 1, 100_000);
    public static final ModConfigSpec.IntValue BOUNTY_NORMAL_MAX_QUANTITY = FUTURE_BUILDER
            .comment("Most items a generated NORMAL request asks for (0.1.10.26)")
            .defineInRange("bountyNormalMaxQuantity", 256, 1, 100_000);
    public static final ModConfigSpec.IntValue BOUNTY_ADVANCED_MIN = FUTURE_BUILDER
            .comment("ADVANCED bounties posted in a market week (7 days): at least (0.1.10.26). They come from the datapack tables (si_exchange/bounties).")
            .defineInRange("bountyAdvancedPerWeekMin", 2, 0, 21);
    public static final ModConfigSpec.IntValue BOUNTY_ADVANCED_MAX = FUTURE_BUILDER
            .comment("ADVANCED bounties posted in a market week: at most (0.1.10.26)")
            .defineInRange("bountyAdvancedPerWeekMax", 3, 0, 21);
    public static final ModConfigSpec.IntValue BOUNTY_ADVANCED_DAYS = FUTURE_BUILDER
            .comment("Days an ADVANCED bounty stays open (0.1.10.26)")
            .defineInRange("bountyAdvancedDays", 7, 1, 60);
    public static final ModConfigSpec.DoubleValue BOUNTY_ADVANCED_MULTIPLIER = FUTURE_BUILDER
            .comment("Reward of an ADVANCED bounty as a multiple of the value of what it asks for (0.1.10.26)")
            .defineInRange("bountyAdvancedMultiplier", 1.3, 0.1, 10.0);
    public static final ModConfigSpec.DoubleValue BOUNTY_ADVANCED_SHARE = FUTURE_BUILDER
            .comment("Share of an ADVANCED bounty's value one player may deliver in multiplayer, until its last day (0.1.10.26). 1 = no limit.")
            .defineInRange("bountyAdvancedPlayerShare", 0.5, 0.05, 1.0);
    public static final ModConfigSpec.DoubleValue BOUNTY_SPECIAL_CHANCE = FUTURE_BUILDER
            .comment("Chance per market day that a SPECIAL bounty is posted (0.1.10.26)")
            .defineInRange("bountySpecialChancePerDay", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.IntValue BOUNTY_SPECIAL_MAX_OPEN = FUTURE_BUILDER
            .comment("SPECIAL bounties open at the same time, at most (0.1.10.26)")
            .defineInRange("bountySpecialMaxOpen", 1, 0, 8);
    public static final ModConfigSpec.IntValue BOUNTY_SPECIAL_DAYS = FUTURE_BUILDER
            .comment("Days a SPECIAL bounty stays open (0.1.10.26)")
            .defineInRange("bountySpecialDays", 7, 1, 60);
    public static final ModConfigSpec.IntValue BOUNTY_ISSUE_NORMAL = FUTURE_BUILDER
            .comment("Items that NORMAL bounties may create as rewards in a market week (reward source \"issue\", counted when posted; 0.1.10.26)")
            .defineInRange("bountyIssuePerWeekNormal", 0, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BOUNTY_ISSUE_ADVANCED = FUTURE_BUILDER
            .comment("Items that ADVANCED bounties may create as rewards in a market week (0.1.10.26)")
            .defineInRange("bountyIssuePerWeekAdvanced", 16, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BOUNTY_ISSUE_SPECIAL = FUTURE_BUILDER
            .comment("Items that SPECIAL bounties may create as rewards in a market week (0.1.10.26)")
            .defineInRange("bountyIssuePerWeekSpecial", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BOUNTY_UU_PER_WEEK = FUTURE_BUILDER
            .comment("UU-matter (mB) that bounties of all tiers may pay as rewards in a market week (0.1.10.26)")
            .defineInRange("bountyUuPerWeek", 4000, 0, 1_000_000_000);
    public static final ModConfigSpec.IntValue BOUNTY_TREASURY_LOW = FUTURE_BUILDER
            .comment("A generated NORMAL request asks only for commodities of which the market owns fewer than this many items (0.1.10.26)")
            .defineInRange("bountyTreasuryLow", 64, 0, 1_000_000);
    // 0.1.10.34: the pace of the bounties
    public static final ModConfigSpec.BooleanValue BOUNTY_RANDOM_DAYS = FUTURE_BUILDER
            .comment("Bounties stay open a random number of days by their rarity (common 3-7, uncommon 2-5, rare 1-4, legendary 1-3; at least",
                     "1 + log2(value / 4,000) days for a large one); fewer than 4 days pay a rush bonus of 5 % a day (0.1.10.34).",
                     "Off: the fixed days of the tiers (bountyNormalDays, bountyAdvancedDays, bountySpecialDays). A table's duration_days always wins.")
            .define("bountyRandomDays", true);
    public static final ModConfigSpec.IntValue BOUNTY_MIN_DAYS = FUTURE_BUILDER
            .comment("Fewest days a bounty with random days stays open (0.1.10.34)")
            .defineInRange("bountyMinDays", 1, 1, 60);
    public static final ModConfigSpec.IntValue BOUNTY_MAX_DAYS = FUTURE_BUILDER
            .comment("Most days a bounty with random days stays open (0.1.10.34)")
            .defineInRange("bountyMaxDays", 7, 1, 60);
    public static final ModConfigSpec.IntValue BOUNTY_ACCEPT_MAX = FUTURE_BUILDER
            .comment("NORMAL and ADVANCED bounties one player may claim at the same time (0.1.10.34). Only the claimant and their team deliver to a claimed bounty.")
            .defineInRange("bountyAcceptMax", 1, 1, 16);
    // 0.1.10.35: commissions and orders in hours of the server running, the treasury reserve, player shops
    public static final ModConfigSpec.IntValue COMMISSION_DEFAULT_HOURS = FUTURE_BUILDER
            .comment("Hours a new commission lasts unless the poster picks others (0.1.10.35). The clock is the world's game time: it",
                     "runs while the server runs and does not move with /time set.")
            .defineInRange("commissionDefaultHours", 24, 1, 168);
    public static final ModConfigSpec.IntValue COMMISSION_MAX_HOURS = FUTURE_BUILDER
            .comment("Most hours a commission may last (0.1.10.35)")
            .defineInRange("commissionMaxHours", 168, 1, 168);
    public static final ModConfigSpec.IntValue ORDER_HOURS = FUTURE_BUILDER
            .comment("Hours a resting order of the order book lasts (0.1.10.35; before: 7 market days)")
            .defineInRange("orderHours", 72, 1, 168);
    public static final ModConfigSpec.IntValue RESERVE_WEEKS = FUTURE_BUILDER
            .comment("Treasury reserve (0.1.10.35): fees go into a coin reserve instead of being destroyed, and bounty coin rewards come",
                     "from it first. Its cap is this many market weeks of NORMAL bounty rewards; above the cap fees are destroyed.",
                     "0 = no reserve (every fee destroyed, as before).")
            .defineInRange("reserveWeeks", 4, 0, 52);
    public static final ModConfigSpec.BooleanValue SHOPS_ENABLED = FUTURE_BUILDER
            .comment("Player shops on the commission board (0.1.10.35)")
            .define("shopsEnabled", true);
    public static final ModConfigSpec.IntValue SHOP_MAX_OFFERS = FUTURE_BUILDER
            .comment("Offers of one player's shop (0.1.10.35)")
            .defineInRange("shopMaxOffers", 12, 1, 54);
    public static final ModConfigSpec.IntValue SHOP_STACKS = FUTURE_BUILDER
            .comment("Storage of one shop in stacks: the goods in stock and the items customers gave (0.1.10.35)")
            .defineInRange("shopStacks", 54, 9, 270);
    public static final ModConfigSpec.IntValue SHOP_OFFER_FEE = FUTURE_BUILDER
            .comment("Coins a new shop offer costs; they go to the treasury reserve (0.1.10.35)")
            .defineInRange("shopOfferFee", 50, 0, 1_000_000);
    public static final ModConfigSpec.IntValue SHOP_UPKEEP = FUTURE_BUILDER
            .comment("Coins each shop offer costs a market week, from the owner's coin account; an offer whose upkeep is not paid pauses (0.1.10.35)")
            .defineInRange("shopUpkeepPerWeek", 10, 0, 1_000_000);
    public static final ModConfigSpec.IntValue SHOP_IDLE_DAYS = FUTURE_BUILDER
            .comment("A shop whose owner was not online on this many days on which the server ran is idle: listed last, still trades (0.1.10.35)")
            .defineInRange("shopIdleDays", 7, 1, 365);
    public static final ModConfigSpec.IntValue SHOP_CLOSE_DAYS = FUTURE_BUILDER
            .comment("A shop whose owner was not online on this many days on which the server ran closes; its goods, coins and proceeds",
                     "go to the owner's mailbox (0.1.10.35)")
            .defineInRange("shopCloseDays", 30, 1, 3650);

    // 0.1.10.28 (X4): the coin <-> UU board, the scrap sink, the stabiliser
    public static final ModConfigSpec.BooleanValue EXCHANGE_UU_BOARD = FUTURE_BUILDER
            .comment("Players trade UU-matter for coins on an order book (lots of 100 mB; 0.1.10.28). The market never buys or sells UU-matter.")
            .define("uuBoard", true);
    public static final ModConfigSpec.DoubleValue EXCHANGE_SCRAP_THRESHOLD = FUTURE_BUILDER
            .comment("Scrap sink (0.1.10.28): with the option, the items an instant sale leaves are recycled (the SI recycler's rate, scrap 1/8)",
                     "when the next bid is below this share of the bid without pressure (and below 1 coin in any case). 0 = no sink.")
            .defineInRange("scrapThreshold", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue EXCHANGE_STABILIZER = FUTURE_BUILDER
            .comment("Stabiliser (0.1.10.28): when the net coins the market created in the last 7 days pass the target, the sell depths and the",
                     "daily allowance shrink step by step (they grow back below 80 % of the target)")
            .define("stabilizerEnabled", false);
    public static final ModConfigSpec.IntValue EXCHANGE_STABILIZER_PER_PLAYER = FUTURE_BUILDER
            .comment("Stabiliser target: net coins a week per player who sold to the market in the last 7 days (0.1.10.28)")
            .defineInRange("stabilizerTargetPerPlayerWeek", 100_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue EXCHANGE_STABILIZER_PERCENT = FUTURE_BUILDER
            .comment("Stabiliser target: net coins a week in percent of the money supply (the net coins since the record began), when that is more (0.1.10.28)")
            .defineInRange("stabilizerTargetPercentPerWeek", 2.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue EXCHANGE_STABILIZER_STEP = FUTURE_BUILDER
            .comment("Stabiliser: the change of the factor per market day (0.1.10.28)")
            .defineInRange("stabilizerStep", 0.05, 0.001, 0.5);
    public static final ModConfigSpec.DoubleValue EXCHANGE_STABILIZER_MIN = FUTURE_BUILDER
            .comment("Stabiliser: the lowest factor of the sell depths and the allowance (0.1.10.28)")
            .defineInRange("stabilizerMinFactor", 0.5, 0.05, 1.0);

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
    // 0.1.10.37：核爆把深层矿石转为深层铱矿石的概率
    public static final ModConfigSpec.DoubleValue NUKE_IRIDIUM_ORE_CHANCE = NUCLEAR_BUILDER
            .comment("核弹与反应堆核爆把爆炸范围内每个深层矿石（#mio_icif:nuke_transmutable）转为深层铱矿石的概率",
                     "默认 0.0002（0.02%）；1 为全部转换，0 关闭；每次爆炸保存爆炸时的值，调整后不影响已发生的爆炸",
                     "每个位置的结果由爆炸与坐标决定，存档重进后继续处理不会重新抽取")
            .defineInRange("nukeIridiumOreChance", 0.0002, 0.0, 1.0);

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
                    "fair = 确定性均等分配（智能电网）：负载与变压器优先于储电设备；同一阶段按最大最小公平（注水）分配，需求小的设备先被满足，大需求设备不能独占；为下游机器供电的储电设备在优先阶段获得其输出额度（0.1.10.12）。默认值。",
                    "fair = deterministic fair share (smart grid). Loads and transformers first, storage last. Within a stage max-min fair (water-filling): small demands are met first, one huge demand cannot take everything; storage that feeds machines gets its output rate in the first stage (0.1.10.12). Default.",
                    "legacy = 0.1.7.x 的 IC2 式数据包优先级分配 / the IC2-style packet priority walk of 0.1.7.x")
            .define("distributionMode", "fair", value -> "fair".equals(value) || "legacy".equals(value));

    public static final ModConfigSpec.ConfigValue<String> ENERGY_NET_LINE_LOSS = ENERGY_NET_BUILDER
            .comment("线路损耗模型（仅 fair 模式）/ Line loss model (fair mode only)",
                    "lossless = 无线损。默认值。/ No cable loss. Default.",
                    "proportional = 损耗 = 能量 × 电缆损耗 / 数据包电压；电压越高损耗比例越低。",
                    "proportional = loss = energy * cable loss / packet voltage. A higher voltage has a lower loss fraction.")
            .define("lineLossModel", "lossless", value -> "lossless".equals(value) || "proportional".equals(value));

    // 0.1.10.22 (G3): order in which the fair distribution takes energy from the sources.
    public static final ModConfigSpec.ConfigValue<String> ENERGY_NET_DISPATCH_MODE = ENERGY_NET_BUILDER
            .comment("电源调度次序（仅 fair 模式）/ Dispatch order of the sources (fair mode only, 0.1.10.22)",
                    "merit = 经济次序：先用必须运行的电源（太阳能、风力、水力、核能等），再用燃料发电机，最后用储能；燃料发电机仍为储能充电。默认值。",
                    "merit = merit order: must-run sources (solar, wind, water, nuclear...) first, then fuel generators, storage last; fuel still charges storage. Default.",
                    "economic = 经济调度：必须运行的电源 → 储能 → 燃料发电机；燃料不为储能充电（储能只由盈余充电）。",
                    "economic = economic dispatch: must-run sources, then storage, then fuel; fuel never charges storage (storage charges from surplus only).",
                    "rotation = 0.1.10.21 之前：所有发电机轮流，储能最后 / before 0.1.10.22: all generators take turns, storage last")
            .define("dispatchMode", "merit", value -> "merit".equals(value) || "economic".equals(value) || "rotation".equals(value));

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

    // ==================== 0.1.10.7: performance ====================
    private static final ModConfigSpec.Builder PERFORMANCE_BUILDER = BUILDER
        .comment("Performance (0.1.10.7).")
        .push("Performance");
    public static final ModConfigSpec.BooleanValue AUTOMATION_PACING = PERFORMANCE_BUILDER
        .comment("Item ejector / pulling upgrades (0.1.10.8: also the fluid ones): after a run that moved nothing, wait 4, 8, 16, then 20 ticks",
                 "before the next run (a change of the machine's inventory or tanks runs them at once). false = run every tick, as before 0.1.10.7.",
                 "物品弹出／抽入升级（0.1.10.8 起也包括流体升级）：一次未移动任何东西后，依次等待 4、8、16、20 tick 再运行（机器物品栏或储罐变化时立即运行）。false = 每 tick 运行（0.1.10.7 之前的行为）。")
        .define("automationPacing", true);
    public static final ModConfigSpec.BooleanValue MACHINE_SLEEP = PERFORMANCE_BUILDER
        .comment("Idle sleep (0.1.10.8): a macerator, electric furnace, extractor, compressor, recycler or metal former that stood idle",
                 "for 1 second skips its recipe checks until its inventory, settings or stored energy change (at the latest 1 second later).",
                 "false = check every tick, as before 0.1.10.8.",
                 "空闲休眠（0.1.10.8）：粉碎机、电炉、提取机、压缩机、回收机、金属成型机空闲 1 秒后，在物品栏、设置或储电变化之前（最迟 1 秒后）不再检查配方。",
                 "false = 每 tick 检查（0.1.10.8 之前的行为）。")
        .define("machineSleep", true);
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder PERFORMANCE_POP = BUILDER.pop();

    // ==================== 0.1.10.11: chunk loaders ====================
    private static final ModConfigSpec.Builder CHUNK_LOADER_BUILDER = BUILDER
        .comment("Chunk loaders (0.1.10.11): server rules. A loader that stops for one of these rules also lets its own chunk unload;",
                 "it starts again by itself when the reason is gone (owner logs in, admin re-enables it, switch on again, budget freed).",
                 "Admins: /mio_icif chunkloaders (list, player, disable, enable, disable_all).",
                 "区块加载器（0.1.10.11）：服务器规则。因规则停止的加载器也会让自身所在区块卸载；原因消除后（所有者上线、管理员重新启用、开关重新打开、额度释放）自动恢复。",
                 "管理员命令：/mio_icif chunkloaders（list、player、disable、enable、disable_all）。")
        .translation("mio_icif.configuration.chunk_loaders").push("ChunkLoaders");
    public static final ModConfigSpec.BooleanValue CHUNK_LOADERS_ENABLED = CHUNK_LOADER_BUILDER
        .comment("false = every SI chunk loader releases its chunks (selections are kept). 关闭后所有 SI 区块加载器释放区块（保留选择）。")
        .translation("mio_icif.configuration.chunk_loaders.enabled").define("enabled", true);
    public static final ModConfigSpec.IntValue CHUNK_LOADERS_MAX_PER_LOADER = CHUNK_LOADER_BUILDER
        .comment("Chunks one loader may load (1-25). Over the limit it keeps its own chunk, then the nearest selected ones.",
                 "单个加载器可加载的区块数（1-25）。超出时优先保留自身区块，其次是最近的已选区块。")
        .translation("mio_icif.configuration.chunk_loaders.maxChunksPerLoader").defineInRange("maxChunksPerLoader", 25, 1, 25);
    public static final ModConfigSpec.IntValue CHUNK_LOADERS_MAX_PER_PLAYER = CHUNK_LOADER_BUILDER
        .comment("Chunks all loaders of one player may load together, in all dimensions. 0 = no limit. First come, first served.",
                 "同一玩家所有加载器（所有维度）合计可加载的区块数。0 = 不限。先到先得。")
        .translation("mio_icif.configuration.chunk_loaders.maxChunksPerPlayer").defineInRange("maxChunksPerPlayer", 0, 0, 1_000_000);
    public static final ModConfigSpec.IntValue CHUNK_LOADERS_MAX_LOADERS_PER_PLAYER = CHUNK_LOADER_BUILDER
        .comment("Active loaders per player. 0 = no limit. 每名玩家同时工作的加载器数。0 = 不限。")
        .translation("mio_icif.configuration.chunk_loaders.maxLoadersPerPlayer").defineInRange("maxLoadersPerPlayer", 0, 0, 100_000);
    public static final ModConfigSpec.IntValue CHUNK_LOADERS_OFFLINE_MINUTES = CHUNK_LOADER_BUILDER
        .comment("Minutes a loader keeps working after its owner logged off. -1 = always (as before 0.1.10.11), 0 = stop at once.",
                 "Loaders placed before 0.1.10.11 have no owner and are not affected.",
                 "所有者下线后加载器继续工作的分钟数。-1 = 一直工作（0.1.10.11 之前的行为），0 = 立即停止。0.1.10.11 之前放置的加载器没有所有者，不受影响。")
        .translation("mio_icif.configuration.chunk_loaders.offlineOwnerMinutes").defineInRange("offlineOwnerMinutes", -1, -1, 525_600);
    public static final ModConfigSpec.DoubleValue CHUNK_LOADERS_EU_PER_CHUNK = CHUNK_LOADER_BUILDER
        .comment("EU per loaded chunk and tick. 每个已加载区块每 tick 消耗的 EU。")
        .translation("mio_icif.configuration.chunk_loaders.euPerChunkPerTick").defineInRange("euPerChunkPerTick", 1.0, 0.0, 1_000_000.0);
    public static final ModConfigSpec.BooleanValue CHUNK_LOADERS_RANDOM_TICKS = CHUNK_LOADER_BUILDER
        .comment("true = loaded chunks get random ticks and natural mob spawning as if a player stood there (as before 0.1.10.11).",
                 "false = machines, block entities and entities keep running, but no random ticks (crops, leaves, fire) and no natural spawning: much cheaper.",
                 "true = 已加载区块像有玩家在场一样进行随机刻和自然生成（0.1.10.11 之前的行为）。",
                 "false = 机器、方块实体和实体照常运行，但没有随机刻（作物、树叶、火）和自然生成：开销小得多。")
        .translation("mio_icif.configuration.chunk_loaders.randomTicks").define("randomTicks", true);
    public static final ModConfigSpec.BooleanValue CHUNK_LOADERS_OWNER_ONLY_EDIT = CHUNK_LOADER_BUILDER
        .comment("true = only the owner (and operators) may change the chunk selection. true = 仅所有者（及管理员）可修改区块选择。")
        .translation("mio_icif.configuration.chunk_loaders.ownerOnlyEdit").define("ownerOnlyEdit", false);
    @SuppressWarnings("unused")
    private static final ModConfigSpec.Builder CHUNK_LOADER_POP = BUILDER.pop();

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
