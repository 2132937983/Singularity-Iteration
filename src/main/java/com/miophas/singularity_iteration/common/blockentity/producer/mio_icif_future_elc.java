package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.miophas.singularity_iteration.common.future.CommodityCategory;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;


/**
 * 期货机方块实体 - 业务逻辑
 * 
 * 职责：
 * 1. 处理所有业务逻辑（价格计算、交易执行、天数更新等）
 * 2. 与数据库层（FutureMarketData）交互，读写数据
 * 3. 通过 ContainerData 将数据同步到客户端 GUI
 * 4. 管理当前状态（选中的货品、交易数量、每日限制等）
 * 
 * 注意：
 * - 所有数据修改必须通过数据库层
 * - 客户端只读取，不修改数据
 */
@SuppressWarnings("null")
public class mio_icif_future_elc extends AbstractEnergyBlockEntity {

    /** This inventory-free consumer uses the same guarded FE input ledger as processing machines. */
    @Override
    public net.neoforged.neoforge.energy.IEnergyStorage scexFeCapability(@org.jetbrains.annotations.Nullable Direction side) {
        return scexFeBridge().port(side);
    }

    // 数据槽位定义
    public static final int DATA_COUNT = 121;  // 增加槽位数量以支持更多商品
public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_CURRENT_DAY = 2;
    public static final int DATA_SELECTED_COMMODITY = 3;
    public static final int DATA_TRADE_QUANTITY = 4;
    public static final int DATA_PLAYER_COINS_LOW = 5;  // 货币数量低16位
public static final int DATA_PLAYER_COINS_HIGH = 6; // 货币数量高16位
public static final int DATA_CURRENT_CATEGORY = 7;
    public static final int DATA_CURRENT_PAGE = 8;
    public static final int DATA_DAILY_VOLUME = 9;
    public static final int DATA_DAILY_LIMIT = 10;
    
    // 价格历史数据起始索引 (11-50, 每个价格占2个int = 40个槽位)
    public static final int DATA_HISTORY_START = 11;
    public static final int DATA_HISTORY_END = 50;
    
    // 货品价格数据（每种货品一个槽位，最终约70种）
    public static final int DATA_PRICES_START = 51;
    public static final int DATA_PRICES_END = 120;

    // ========== 状态变化 ==========
    
    /** 当前选中的货品种类 */
    private CommodityCategory currentCategory = CommodityCategory.MINERAL;
    
    /** 当前页码 */
    private int currentPage = 0;
    
    /** 选中的货品索引（在当前页内） */
    private int selectedCommodityIndex = -1;
    
    /** 交易数量 */
    private int tradeQuantity = 1;
    
    /** 当前日期（本游戏天数） */
    private long currentDay = 0;
    
    /** 今日已交易量 */
    private int dailyTradeVolume = 0;

    // One synchronous trade per machine and per player, including calls into another machine.
    private boolean tradeInProgress;
    private static final java.util.Set<ServerPlayer> TRADING_PLAYERS =
        java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    
 /** 玩家货币存储（用于显示） */
    private long cachedPlayerCoins = 0;
    
 /** 最后选中的物品ID（用于跨刷新存储） */
    @SuppressWarnings("unused")
    private String lastSelectedCommodityId = "";
    
 /** 价格历史存储（用于同步到客户端） */
    private final int[] cachedPriceHistory = new int[FutureMarketData.getHistoryDays()];

    // ========== 构造函数 ==========
    
    public mio_icif_future_elc(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state, type, Singularity_Iteration_Config.FUTURE_ENERGY_CAPACITY.get(), Singularity_Iteration_Config.FUTURE_MAX_RECEIVE.get(), 100, CableTier.LV);
    }

    public mio_icif_future_elc(BlockPos pos, BlockState state) {
        this(mio_icif_block_entities.FUTURE_ELC_ENTITY_TYPE.get(), pos, state);
    }

    // ========== Tick 方法 ==========
    
    /**
     * 方块实体 tick 方法
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_future_elc blockEntity) {
        if (level.isClientSide) return;
        
        // Apply reloaded storage settings on the server thread; capacity shrink preserves owned EU.
        long capacity = Singularity_Iteration_Config.FUTURE_ENERGY_CAPACITY.get();
        long receive = Singularity_Iteration_Config.FUTURE_MAX_RECEIVE.get();
        if (blockEntity.energyStorage.getCapacity() != capacity || blockEntity.energyStorage.getMaxReceive() != receive) {
            blockEntity.energyStorage.setCapacity(capacity);
            blockEntity.energyStorage.setMaxReceive(receive);
            blockEntity.refreshRegistration();
            blockEntity.setChanged();
        }
        // 调用父类tick方法，处理电网充电逻辑
        AbstractEnergyBlockEntity.tick(level, pos, state, blockEntity);
        
        // 检查并执行每日更新
        blockEntity.checkDayUpdate(level);
        
 // 每tick都更新价格历史缓存，确保 GUI 折线图实时显示最新数据
    blockEntity.updatePriceHistoryCache();
    }

    // ========== 数据同步 ==========
    
    @SuppressWarnings("unused")
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> (int) energyStorage.getAmount();
                case DATA_MAX_ENERGY -> (int) energyStorage.getCapacity();
                case DATA_CURRENT_DAY -> (int) (currentDay % 32768); // 限制在short范围内
            case DATA_SELECTED_COMMODITY -> selectedCommodityIndex;
                case DATA_TRADE_QUANTITY -> tradeQuantity;
                case DATA_PLAYER_COINS_LOW -> (int) (cachedPlayerCoins & 0xFFFF); // 低16位
            case DATA_PLAYER_COINS_HIGH -> (int) ((cachedPlayerCoins >> 16) & 0xFFFF); // 高16位
            case DATA_CURRENT_CATEGORY -> currentCategory.ordinal();
                case DATA_CURRENT_PAGE -> currentPage;
                case DATA_DAILY_VOLUME -> Math.min(dailyTradeVolume, 32767); // 限制在short范围内
            case DATA_DAILY_LIMIT -> Math.min(Singularity_Iteration_Config.FUTURE_DAILY_LIMIT.get(), 32767); // 限制在short范围内
            default -> {
                    // 价格历史数据 (11-50) - 每个价格拆分为高16位和低16位
                if (index >= DATA_HISTORY_START && index <= DATA_HISTORY_END) {
                        int dayIndex = (index - DATA_HISTORY_START) / 2;
                        boolean isHigh = (index - DATA_HISTORY_START) % 2 == 0;
                        if (dayIndex < FutureMarketData.getHistoryDays()) {
                            int price = cachedPriceHistory[dayIndex];
                            // 将价格限制在0-65535范围内（无符号short范围）
                        price = Math.max(0, Math.min(price, 65535));
                            // 使用无符号右移，避免负数问题
                            yield isHigh ? (price >>> 16) & 0xFFFF : price & 0xFFFF;
                        }
                    }
                    // 货品价格数据 (50-79) - 限制在short范围内
                if (index >= DATA_PRICES_START && index <= DATA_PRICES_END) {
                        int commodityIndex = index - DATA_PRICES_START;
                        List<FutureCommodity> all = FutureCommodityManager.getCommodities();
                        if (commodityIndex < all.size()) {
                            int price = getCurrentPrice(all.get(commodityIndex));
                            // 将价格限制在0-32767范围内（有符号short正数范围）
                        yield Math.max(0, Math.min(price, 32767));
                        }
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端不应直接设置数据，所有修改通过方法调用
            // 数据通过addDataSlots自动同步到客户端
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ContainerData getContainerData() {
        return data;
    }

    // ========== 业务逻辑 - 价格管理 ==========
    
    /**
     * 获取货品当前价格
     * 优先从数据库获取，如果不存在则使用基础价格
     */
    public int getCurrentPrice(FutureCommodity commodity) {
        if (level == null || level.isClientSide) {
            return commodity.getBasePrice();
        }
        
        FutureMarketData marketData = FutureMarketData.get(level);
        return marketData.getPrice(commodity.getItemId(), commodity.getBasePrice());
    }
    
    /**
     * 设置货品价格（内部使用）
     */
    private void setPrice(FutureCommodity commodity, int price) {
        if (level == null || level.isClientSide) return;
        
        FutureMarketData marketData = FutureMarketData.get(level);
        marketData.setPrice(commodity.getItemId(), price);
    }
    
    /**
     * 计算新价格（基于波动率，带回归机制）
     * 价格会在基础价格附近波动，有向基础价格回归的趋势
     * 如果存在活跃事件，优先使用事件计算价格
 */
    private int calculateNewPrice(FutureCommodity commodity) {
        if (level == null || level.isClientSide) {
            return commodity.getBasePrice();
        }
        
        FutureMarketData marketData = FutureMarketData.get(level);
        int currentPrice = getCurrentPrice(commodity);
        int basePrice = commodity.getBasePrice();
        
        // 检查是否有活跃事件
        FuturePriceEvent activeEvent = marketData.getActiveEvent(commodity.getItemId(), currentDay);
        if (activeEvent != null) {
            int dayOffset = (int) (currentDay - activeEvent.getStartDay());
            return activeEvent.applyEffect(currentPrice, basePrice, dayOffset);
        }
        
        // 没有事件时使用默认计算逻辑
        double volatility = commodity.getVolatility();
        net.minecraft.util.RandomSource random = level != null ? level.random : net.minecraft.util.RandomSource.create();

        // 计算与基础价格的偏差
        double deviation = (double) (currentPrice - basePrice) / basePrice;

        // 回归因子：使用非线性回归，价格偏离越远回归越强
        // 价格暴跌时（负偏差）回归力度更强，加快价格恢复
        // 价格暴涨时（正偏差）回归力度相对较弱，允许价格维持高位
    double baseRegression = 0.15; // 基础回归力度 15%

        // 根据偏差方向调整回归力度：暴跌时回归更快
        double directionMultiplier;
        if (deviation < 0) {
            // 价格低于基础价格（暴跌后）：回归力度增强 50%
            directionMultiplier = 1.5;
        } else {
            // 价格高于基础价格（暴涨后）：回归力度正常
            directionMultiplier = 1.0;
        }

        double extremeMultiplier = Math.max(0, Math.abs(deviation) - 0.5); // 超过0.5倍偏差时开始增强回归
    double regressionFactor = baseRegression * directionMultiplier + extremeMultiplier * 0.2;
        regressionFactor = Math.min(regressionFactor, 0.6); // 最高不超过60%

        // 随机波动
        double randomChange = (random.nextDouble() - 0.5) * 2 * volatility;

        // 综合变化 = 随机波动 - 回归调整
        // 如果价格高于基础价格，回归调整为负，拉低价格
        // 如果价格低于基础价格，回归调整为正，拉高价格
        double totalChange = randomChange - deviation * regressionFactor;
        
        // 计算新价格（基于当前价格变化）
    int newPrice = (int) (currentPrice * (1 + totalChange));
        
        // 确保价格在配置范围内（默认基础价格10%~1000%）
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);
        
        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
    
    /**
     * 随机生成价格事件
     * 每天有概率为某些货品生成事件
     */
    private void generateRandomEvents() {
        if (level == null || level.isClientSide) return;
        
        FutureMarketData marketData = FutureMarketData.get(level);
        net.minecraft.util.RandomSource random = level.random;
        
        // 清理过期事件
        marketData.cleanupExpiredEvents(currentDay);
        
        // 为每个货品检查是否生成事件（20%概率）
    for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            // 如果该货品已有活跃事件，跳过
            if (marketData.getActiveEvent(commodity.getItemId(), currentDay) != null) {
                continue;
            }
            
            // 20% 概率生成事件
            if (random.nextDouble() < 0.2) {
                FuturePriceEvent event = createRandomEvent(commodity, random);
                if (event != null) {
                    marketData.addEvent(commodity.getItemId(), event);
                    // 事件通知已禁用（可在配置中开启）
                }
            }
        }
    }
    
    /**
     * 广播事件消息到所有玩家
 */
    @SuppressWarnings("unused")
    private void broadcastEventMessage(FutureCommodity commodity, FuturePriceEvent event) {
        if (level == null || level.isClientSide) return;
        
        String commodityName = commodity.getDisplayName();
        String eventName = getEventDisplayName(event.getType());
        int duration = event.getDuration();
        String intensityDesc = getIntensityDescription(event.getIntensity());
        
        // 构建消息 - 使用翻译键
    for (Player player : level.players()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.mio_icif.future.event_broadcast",
                commodityName,
                net.minecraft.network.chat.Component.translatable(eventName),
                duration,
                net.minecraft.network.chat.Component.translatable(intensityDesc)
            ));
        }
    }
    
    /**
     * 获取事件显示名称
     */
    private String getEventDisplayName(FuturePriceEvent.EventType type) {
        return switch (type) {
            case RISING_TREND -> "gui.mio_icif.future.event_rising";
            case FALLING_TREND -> "gui.mio_icif.future.event_falling";
            case HIGH_VOLATILITY -> "gui.mio_icif.future.event_high_volatility";
            case LOW_VOLATILITY -> "gui.mio_icif.future.event_low_volatility";
            case PRICE_STABILIZATION -> "gui.mio_icif.future.event_stabilization";
            case SHORT_BUBBLE -> "gui.mio_icif.future.event_short_bubble";
            case LONG_BUBBLE -> "gui.mio_icif.future.event_long_bubble";
            case SHORT_CRASH_REBOUND -> "gui.mio_icif.future.event_short_crash";
            case LONG_CRASH_REBOUND -> "gui.mio_icif.future.event_long_crash";
        };
    }
    
    /**
     * 获取强度描述
     */
    private String getIntensityDescription(double intensity) {
        if (intensity < 0.7) return "gui.mio_icif.future.intensity_weak";
        if (intensity < 1.0) return "gui.mio_icif.future.intensity_medium";
        if (intensity < 1.3) return "gui.mio_icif.future.intensity_strong";
        return "gui.mio_icif.future.intensity_extreme";
    }
    private FuturePriceEvent createRandomEvent(FutureCommodity commodity, net.minecraft.util.RandomSource random) {
        FuturePriceEvent.EventType[] types = FuturePriceEvent.EventType.values();
        FuturePriceEvent.EventType type = types[random.nextInt(types.length)];

        String commodityId = commodity.getItemId();
        long startDay = currentDay;

        // 根据事件类型设置不同的持续时间和强度
        int duration;
        double intensity;

        switch (type) {
            case SHORT_BUBBLE, SHORT_CRASH_REBOUND -> {
        // 短期事件：3-5天，高强化
            duration = 3 + random.nextInt(3);
                intensity = 0.8 + random.nextDouble() * 0.7; // 0.8 - 1.5
            }
            case LONG_BUBBLE, LONG_CRASH_REBOUND -> {
                // 长期事件：6-10天，中等强度
                duration = 6 + random.nextInt(5);
                intensity = 0.6 + random.nextDouble() * 0.6; // 0.6 - 1.2
            }
            default -> {
                // 普通事件：3-7天，标准强度
                duration = 3 + random.nextInt(5);
                intensity = 0.5 + random.nextDouble(); // 0.5 - 1.5
            }
        }

        return switch (type) {
            case RISING_TREND -> new RisingTrendEvent(commodityId, startDay, duration, intensity);
            case FALLING_TREND -> new FallingTrendEvent(commodityId, startDay, duration, intensity);
            case HIGH_VOLATILITY -> new HighVolatilityEvent(commodityId, startDay, duration, intensity);
            case LOW_VOLATILITY -> new LowVolatilityEvent(commodityId, startDay, duration, intensity);
            case PRICE_STABILIZATION -> new PriceStabilizationEvent(commodityId, startDay, duration, intensity);
            case SHORT_BUBBLE -> new ShortBubbleEvent(commodityId, startDay, duration, intensity);
            case LONG_BUBBLE -> new LongBubbleEvent(commodityId, startDay, duration, intensity);
            case SHORT_CRASH_REBOUND -> new ShortCrashReboundEvent(commodityId, startDay, duration, intensity);
            case LONG_CRASH_REBOUND -> new LongCrashReboundEvent(commodityId, startDay, duration, intensity);
        };
    }
    
    /**
     * 更新所有货品价格（新的一天）
     */
    public void updateAllPrices() {
        if (tradeInProgress) return;
        if (level == null || level.isClientSide) return;
        
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            if (!commodity.repricesOn(currentDay)) continue;   // datapack period_days: price holds between periods
            int newPrice = calculateNewPrice(commodity);
            setPrice(commodity, newPrice);
        }
        
        setChanged();
        syncToClient();
    }
    
    // ========== 业务逻辑 - 价格历史 ==========
    
    /**
     * 获取货品价格历史（20天）
     * 返回列表：索引0=19天前，索引9=今天
     */
    public List<Integer> getPriceHistory(FutureCommodity commodity) {
        if (commodity == null) {
            return new ArrayList<>();
        }
        
        if (level == null || level.isClientSide) {
 // 客户端：从缓存读取
        List<Integer> history = new ArrayList<>();
            for (int price : cachedPriceHistory) {
                history.add(price);
            }
            return history;
        }
        
        // 服务端：从数据库读取
        FutureMarketData marketData = FutureMarketData.get(level);
        List<Integer> history = marketData.getPriceHistory(commodity.getItemId());
        
        // 确保20天数据
    while (history.size() < FutureMarketData.getHistoryDays()) {
            history.add(0, commodity.getBasePrice());
        }
        
        return history;
    }
    
    /**
 * 更新价格历史存（同步到客户端）
     */
    private void updatePriceHistoryCache() {
        FutureCommodity commodity = getSelectedCommodity();
        if (commodity == null) return;
        
        String commodityId = commodity.getItemId();
        lastSelectedCommodityId = commodityId;
        
        List<Integer> history = getPriceHistory(commodity);
        for (int i = 0; i < Math.min(history.size(), cachedPriceHistory.length); i++) {
            cachedPriceHistory[i] = history.get(i);
        }
        
        setChanged();
        syncToClient();
    }
    
    // ========== 业务逻辑 - 天数更新 ==========
    
    /**
     * 检查并执行每日更新
     * 在游戏每天第 1 tick 调用
     */
    public void checkDayUpdate(Level level) {
        if (tradeInProgress) return;
        if (level.isClientSide) return;
        
        long gameDay = level.getDayTime() / 24000L;
        FutureMarketData marketData = FutureMarketData.get(level);
        
        // 首次运行：初始化
        if (!marketData.isInitialized()) {
            initializeMarket(marketData, gameDay);
            return;
        }
        
        // 始终同步天数，确保与数据库一致
    long marketDay = marketData.getCurrentDay();
        if (currentDay != marketDay) {
            // 如果市场数据的天数与本地不一致，以市场数据为准
        currentDay = marketDay;
        }
        
        // 天数变化：执行每日更新
    if (gameDay != currentDay) {
            performDayUpdate(marketData, gameDay);
        }
    }
    
    /**
     * 初始化市场数据
 */
    private void initializeMarket(FutureMarketData marketData, long gameDay) {
        // 初始化所有货品价格为基础价格
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            String id = commodity.getItemId();
            marketData.setPrice(id, commodity.getBasePrice());
            marketData.initializeCommodityHistory(id, commodity.getBasePrice());
        }
        
        marketData.setCurrentDay(gameDay);
        marketData.setInitialized(true);
        
        currentDay = gameDay;
        dailyTradeVolume = 0;
        
        setChanged();
        syncToClient();
    }
    
    /**
     * 执行每日更新
     */
    private void performDayUpdate(FutureMarketData marketData, long newDay) {
        // 1. 记录今天的价格到历史
        recordTodayPrices(marketData);
        
        // 2. 滚动价格历史
        marketData.rollPriceHistory();
        
        // 3. 生成随机价格事件
        generateRandomEvents();
        
        // 4. 更新所有货品价格
    for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            if (!commodity.repricesOn(newDay)) continue;   // datapack period_days: price holds between periods
            int newPrice = calculateNewPrice(commodity);
            setPrice(commodity, newPrice);
        }
        
        // 5. 记录新价格到历史
        recordTodayPrices(marketData);
        
        // 6. 更新天数和重置限制
    currentDay = newDay;
        marketData.setCurrentDay(newDay);
        dailyTradeVolume = 0;
        
        // 7. 刷新内存缓存
        updatePriceHistoryCache();
        
        setChanged();
        syncToClient();
    }
    
    /**
     * 记录今天所有货品的价格到历史
 */
    private void recordTodayPrices(FutureMarketData marketData) {
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            int price = getCurrentPrice(commodity);
            marketData.recordTodayPrice(commodity.getItemId(), price);
        }
    }
    
    // ========== 业务逻辑 - 交易 ==========
    
    /**
     * 执行购买
     * @param player 玩家
     * @return 是否成功
     */
    public boolean executeBuy(Player player) {
        return executeTrade(player, true);
    }

    public boolean executeSell(Player player) {
        return executeTrade(player, false);
    }

    private boolean liveTradeOwner(ServerLevel world) {
        if (level != world || isRemoved() || !world.getServer().isSameThread()) return false;
        var chunk = world.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntities().get(worldPosition) == this
            && chunk.getBlockState(worldPosition).getBlock() == getBlockState().getBlock()
            && world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(worldPosition));
    }

    private boolean authorized(ServerLevel world, ServerPlayer player, FutureElcMenu menu) {
        return liveTradeOwner(world) && player.level() == world && player.isAlive() && !player.isSpectator()
            && player.containerMenu == menu && menu.getBlockEntity() == this && menu.stillValid(player)
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    private boolean executeTrade(Player player, boolean buying) {
        if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread()
                || !(player instanceof ServerPlayer customer) || tradeInProgress
                || !TRADING_PLAYERS.add(customer)) return false;
        tradeInProgress = true;
        CustomEUEnergyStorage.InternalPayment payment = null;
        boolean committed = false;
        try {
            if (!(customer.containerMenu instanceof FutureElcMenu menu) || !authorized(world, customer, menu)) return false;
            final int quantity = tradeQuantity;
            final int volume = dailyTradeVolume;
            final long day = currentDay;
            final CommodityCategory category = currentCategory;
            final int page = currentPage, selected = selectedCommodityIndex;
            final int fee = Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
            final int limit = Singularity_Iteration_Config.FUTURE_DAILY_LIMIT.get();
            if (quantity < 1 || quantity > 64 || volume < 0 || (long) volume + quantity > limit
                    || fee < 0 || fee > 10000) return false;
            FutureCommodity commodity = getSelectedCommodity();
            if (commodity == null || commodity.getItem() == Items.AIR) return false;
            if (!FutureCommodityManager.isUnlocked(customer, commodity)) return false;   // datapack unlock condition
            final int price = getCurrentPrice(commodity);
            if (price <= 0) return false;
            final long coins = (long) price * quantity;
            final Inventory inventory = customer.getInventory();
            final TradeInventory plan = new TradeInventory(inventory, menu);
            // Zero is explicitly free. Positive action fees bypass public transfer-rate limits.
            if (fee > 0) {
                payment = energyStorage.scexReserveInternal(fee);
                if (payment == null) return false;
            }
            Item currency = mio_icif_normal.COIN.get();
            if (!plan.removeMain(buying ? currency : commodity.getItem(), buying ? coins : quantity)) return false;
            if (!plan.insert(buying ? commodity.getItem() : currency, buying ? quantity : coins)) return false;
            long nextCoins = plan.countMain(currency);
            // Every item stack limit query and component comparison precedes the commit.
            if (!authorized(world, customer, menu) || getSelectedCommodity() != commodity
                    || getCurrentPrice(commodity) != price || currentDay != day || dailyTradeVolume != volume
                    || tradeQuantity != quantity || currentCategory != category || currentPage != page
                    || selectedCommodityIndex != selected || Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get() != fee
                    || Singularity_Iteration_Config.FUTURE_DAILY_LIMIT.get() != limit
                    || customer.getInventory() != inventory || !plan.unchanged(menu)) return false;
            // Component equality may be supplied by a dependency; recheck ownership after it.
            if (!authorized(world, customer, menu) || currentDay != day || dailyTradeVolume != volume
                    || tradeQuantity != quantity || currentCategory != category || currentPage != page
                    || selectedCommodityIndex != selected || getSelectedCommodity() != commodity
                    || getCurrentPrice(commodity) != price || Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get() != fee
                    || Singularity_Iteration_Config.FUTURE_DAILY_LIMIT.get() != limit) return false;
            // No external call is made between settling this exact token and fixed-list assignment.
            // Lists have fixed sizes, and every staged replacement is already non-null.
            if (payment != null && !payment.commit()) throw new IllegalStateException("Future payment settled twice");
            plan.commit();
            dailyTradeVolume = volume + quantity;
            cachedPlayerCoins = nextCoins;
            committed = true;
            // Notification failures cannot turn a completed trade into a retryable failure or refund.
            inventory.setChanged();
            setChanged();
            customer.inventoryMenu.broadcastChanges();
            menu.broadcastChanges();
            syncToClient();
            return true;
        } catch (RuntimeException failure) {
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.warn(
                "Future trade {} at {}: {}", committed ? "committed; notification failed" : "aborted before inventory commit",
                worldPosition, failure.toString());
            return committed;
        } finally {
            try {
                if (payment != null && payment.isPending()) payment.cancel();
            } finally {
                tradeInProgress = false;
                TRADING_PLAYERS.remove(customer);
            }
        }
    }

    /** An isolated inventory plan; prepare may call item queries, commit only assigns vanilla fixed lists. */
    private static final class TradeInventory {
        private final Inventory inventory;
        private final ItemStack[] references = new ItemStack[41];
        private final ItemStack[] before = new ItemStack[41];
        private final ItemStack[] after = new ItemStack[41];
        private final boolean[] changed = new boolean[41];
        private final int selected;
        private final int revision;
        private final ItemStack carriedReference, carried;

        private TradeInventory(Inventory inventory, FutureElcMenu menu) {
            if (inventory.items.size() != 36 || inventory.armor.size() != 4 || inventory.offhand.size() != 1)
                throw new IllegalStateException("Unsupported player inventory layout");
            this.inventory = inventory;
            this.selected = inventory.selected;
            this.revision = inventory.getTimesChanged();
            this.carriedReference = menu.getCarried();
            this.carried = carriedReference.copy();
            for (int i = 0; i < 41; i++) {
                references[i] = actual(i);
                before[i] = references[i].copy();
                after[i] = before[i].copy();
            }
        }

        private ItemStack actual(int slot) {
            return slot < 36 ? inventory.items.get(slot)
                : slot < 40 ? inventory.armor.get(slot - 36) : inventory.offhand.get(0);
        }

        private boolean removeMain(Item item, long amount) {
            long remaining = amount;
            for (int i = 0; i < 36 && remaining > 0; i++) {
                ItemStack stack = after[i];
                // Retain existing trade policy: sale/payment inputs match registered item, including custom components.
                if (!stack.isEmpty() && stack.is(item)) {
                    int take = (int) Math.min(remaining, stack.getCount());
                    stack.shrink(take); changed[i] = true; remaining -= take;
                    if (stack.isEmpty()) after[i] = ItemStack.EMPTY;
                }
            }
            return remaining == 0;
        }

        private boolean insert(Item item, long amount) {
            ItemStack template = new ItemStack(item);
            long remaining = amount;
            // Preserve vanilla's merge preference: selected hotbar, existing offhand, then main slots.
            if (selected >= 0 && selected < 9) remaining = merge(selected, template, remaining);
            remaining = merge(40, template, remaining);
            for (int i = 0; i < 36 && remaining > 0; i++) remaining = merge(i, template, remaining);
            for (int i = 0; i < 36 && remaining > 0; i++) {
                if (!after[i].isEmpty()) continue;
                int maximum = inventory.getMaxStackSize(template);
                if (maximum <= 0) return false;
                int add = (int) Math.min(remaining, maximum);
                after[i] = template.copyWithCount(add);
                after[i].setPopTime(5); changed[i] = true; remaining -= add;
            }
            // No overflow entities: their spawn/drop callbacks cannot join this atomic inventory commit.
            return remaining == 0;
        }

        private long merge(int slot, ItemStack template, long remaining) {
            if (remaining <= 0) return 0;
            ItemStack target = after[slot];
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, template) || !target.isStackable())
                return remaining;
            int maximum = inventory.getMaxStackSize(target);
            long room = (long) maximum - target.getCount();
            if (room <= 0) return remaining;
            int add = (int) Math.min(remaining, room);
            target.grow(add); target.setPopTime(5); changed[slot] = true;
            return remaining - add;
        }

        private long countMain(Item item) {
            long result = 0;
            for (int i = 0; i < 36; i++) if (after[i].is(item)) result += after[i].getCount();
            return result;
        }

        private boolean unchanged(FutureElcMenu menu) {
            if (inventory.items.size() != 36 || inventory.armor.size() != 4 || inventory.offhand.size() != 1
                    || inventory.selected != selected || inventory.getTimesChanged() != revision
                    || menu.getCarried() != carriedReference || !ItemStack.matches(carried, menu.getCarried())) return false;
            for (int i = 0; i < 41; i++)
                if (actual(i) != references[i] || !ItemStack.matches(before[i], actual(i))) return false;
            return true;
        }

        private void commit() {
            for (int i = 0; i < 36; i++) if (changed[i]) inventory.items.set(i, after[i]);
            if (changed[40]) inventory.offhand.set(0, after[40]);
        }
    }

    /**
     * 检查是否有足够能量进行交易
     */
    public boolean hasEnoughEnergy() {
        return energyStorage.getAmount() >= Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
    }
    
    // ========== 业务逻辑 - 玩家货币 ==========
    
    /**
     * 计算玩家拥有的货币数据
 */
    public int countPlayerCoins(Player player) {
        long count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(mio_icif_normal.COIN.get())) {
                count += stack.getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, count);
    }
    
    /**
 * 更新玩家货币存（用于显示）
     */
    public void updatePlayerCoinsCache(Player player) {
        if (tradeInProgress) return;
        this.cachedPlayerCoins = countPlayerCoins(player);
        setChanged();
        syncToClient();
    }
    
    // ========== 业务逻辑 - 分类和分配?==========
    
    /**
     * 获取当前分类下的货品列表
     */
    public List<FutureCommodity> getCurrentCategoryCommodities() {
        return FutureCommodityManager.getCommoditiesByCategory(currentCategory);
    }
    
    /**
     * 获取当前页的货品列表
     */
    public List<FutureCommodity> getCurrentPageCommodities() {
        List<FutureCommodity> all = getCurrentCategoryCommodities();
        int itemsPerPage = Singularity_Iteration_Config.FUTURE_ITEMS_PER_PAGE.get();
        long start = (long) currentPage * itemsPerPage;
        if (start < 0 || start >= all.size()) return List.of();
        int startIndex = (int) start;
        int endIndex = (int) Math.min(start + itemsPerPage, all.size());
        
        if (startIndex >= all.size()) {
            return new ArrayList<>();
        }
        return all.subList(startIndex, endIndex);
    }
    
    /**
     * 获取当前选中的货品种类
 */
    public FutureCommodity getSelectedCommodity() {
        List<FutureCommodity> pageItems = getCurrentPageCommodities();
        if (selectedCommodityIndex >= 0 && selectedCommodityIndex < pageItems.size()) {
            return pageItems.get(selectedCommodityIndex);
        }
        return null;
    }
    
    /**
     * 设置选中的货品索引
 */
    public void setSelectedCommodity(int index) {
        if (tradeInProgress) return;
        this.selectedCommodityIndex = index;
        updatePriceHistoryCache();
        setChanged();
        syncToClient();
    }
    
    /**
     * 获取交易数量
     */
    public int getTradeQuantity() {
        return tradeQuantity;
    }
    
    /**
     * 设置交易数量
     */
    public void setTradeQuantity(int quantity) {
        if (tradeInProgress) return;
        this.tradeQuantity = Math.max(1, Math.min(quantity, 64));
        setChanged();
        syncToClient();
    }
    
    /**
     * 切换分类
     */
    public void setCategory(CommodityCategory category) {
        if (tradeInProgress) return;
        if (category == null) return;
        this.currentCategory = category;
        this.currentPage = 0;
        this.selectedCommodityIndex = -1;
        updatePriceHistoryCache();
        setChanged();
        syncToClient();
    }
    
    /**
     * 上一页
 */
    public void previousPage() {
        if (tradeInProgress) return;
        if (currentPage > 0) {
            currentPage--;
            selectedCommodityIndex = -1;
            setChanged();
            syncToClient();
        }
    }
    
    /**
     * 下一页
 */
    public void nextPage() {
        if (tradeInProgress) return;
        int maxPage = (getCurrentCategoryCommodities().size() - 1) / Singularity_Iteration_Config.FUTURE_ITEMS_PER_PAGE.get();
        if (currentPage < maxPage) {
            currentPage++;
            selectedCommodityIndex = -1;
            setChanged();
            syncToClient();
        }
    }
    
    /**
     * 获取当前分类
     */
    public CommodityCategory getCurrentCategory() {
        return currentCategory;
    }
    
    /**
     * 获取当前页码
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 获取选中的货品索引
 */
    public int getSelectedCommodityIndex() {
        return selectedCommodityIndex;
    }
    
    /**
     * 增加交易数量
     */
    public void increaseTradeQuantity() {
        if (tradeInProgress) return;
        tradeQuantity = Math.min(tradeQuantity + 1, 64);
        setChanged();
        syncToClient();
    }
    
    /**
     * 减少交易数量
     */
    public void decreaseTradeQuantity() {
        if (tradeInProgress) return;
        tradeQuantity = Math.max(tradeQuantity - 1, 1);
        setChanged();
        syncToClient();
    }
    
    // ========== 数据持久化 ==========
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("Category", currentCategory.name());
        tag.putInt("Page", currentPage);
        tag.putInt("SelectedIndex", selectedCommodityIndex);
        tag.putInt("TradeQuantity", tradeQuantity);
        tag.putLong("CurrentDay", currentDay);
        tag.putInt("DailyVolume", dailyTradeVolume);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        try {
            currentCategory = CommodityCategory.valueOf(tag.getString("Category"));
        } catch (IllegalArgumentException e) {
            currentCategory = CommodityCategory.MINERAL;
        }
        currentPage = Math.max(0, tag.getInt("Page"));
        selectedCommodityIndex = tag.getInt("SelectedIndex");
        tradeQuantity = Math.max(1, Math.min(64, tag.getInt("TradeQuantity")));
        currentDay = tag.getLong("CurrentDay");
        dailyTradeVolume = Math.max(0, tag.getInt("DailyVolume"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("Category", currentCategory.name());
        tag.putInt("Page", currentPage);
        tag.putInt("SelectedIndex", selectedCommodityIndex);
        tag.putInt("TradeQuantity", tradeQuantity);
        tag.putLong("CurrentDay", currentDay);
        tag.putInt("DailyVolume", dailyTradeVolume);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        try {
            currentCategory = CommodityCategory.valueOf(tag.getString("Category"));
        } catch (IllegalArgumentException e) {
            currentCategory = CommodityCategory.MINERAL;
        }
        currentPage = Math.max(0, tag.getInt("Page"));
        selectedCommodityIndex = tag.getInt("SelectedIndex");
        tradeQuantity = Math.max(1, Math.min(64, tag.getInt("TradeQuantity")));
        currentDay = tag.getLong("CurrentDay");
        dailyTradeVolume = Math.max(0, tag.getInt("DailyVolume"));
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 同步数据到客户端
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu(id, inventory, this, null, this.getContainerData());
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.future_elc");
    }
    
    /**
     * 获取物品栏能力（用于外部访问）
 */
    public IItemHandler getItemHandlerCapability(Direction direction) {
        // 期货机没有内部物品栏，返回null
        return null;
    }
}
