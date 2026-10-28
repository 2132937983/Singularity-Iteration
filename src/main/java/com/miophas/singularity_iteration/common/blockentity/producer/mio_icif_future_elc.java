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
import com.miophas.singularity_iteration.common.future.MarketDayDriver;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketQuote;
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
    public static final int DATA_COUNT = 126;  // 0.1.10.20: + buy cost, sale payout, market room (121..125)
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

    // 0.1.10.20: the quote of the selected commodity for the trade quantity (wide values: low / high 16 bits)
    public static final int DATA_BUY_COST_LOW = 121, DATA_BUY_COST_HIGH = 122;
    /** Sale payout; 0 when the market does not buy that many today. */
    public static final int DATA_SELL_PAYOUT_LOW = 123, DATA_SELL_PAYOUT_HIGH = 124;
    /** Items of the selected commodity that the market still buys today (32767 = no limit or more). */
    public static final int DATA_MARKET_ROOM = 125;

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
    
    // 0.1.10.20: the daily trade count is per player in FutureMarketData (all machines of the server). The old
    // field of each machine multiplied the limit by the number of machines, and only the machine that rolled
    // the day reset its own count: on the other machines the count of the last day stayed.

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
                case DATA_DAILY_VOLUME -> 0; // 0.1.10.20: per player, filled in by viewerData
            case DATA_DAILY_LIMIT -> buyLimit(); // 0.1.10.25: the daily buy limit (32767 = none)
                case DATA_BUY_COST_LOW -> (int) (quote(true) & 0xFFFF);
                case DATA_BUY_COST_HIGH -> (int) ((quote(true) >>> 16) & 0xFFFF);
                case DATA_SELL_PAYOUT_LOW -> (int) (Math.max(0, quote(false)) & 0xFFFF);
                case DATA_SELL_PAYOUT_HIGH -> (int) ((Math.max(0, quote(false)) >>> 16) & 0xFFFF);
                case DATA_MARKET_ROOM -> marketRoom();
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

    /**
     * 0.1.10.20: the menu data for one viewer. The daily count and the coins are the viewer's own; before,
     * all viewers saw the count of the machine and the coins of one of them.
     */
    public ContainerData viewerData(Player viewer) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_DAILY_VOLUME -> level instanceof ServerLevel
                        ? Math.min(FutureMarketData.get(level).getPlayerVolume(viewer.getUUID()), 32767) : 0;
                    case DATA_PLAYER_COINS_LOW -> countPlayerCoins(viewer) & 0xFFFF;
                    case DATA_PLAYER_COINS_HIGH -> (countPlayerCoins(viewer) >>> 16) & 0xFFFF;
                    default -> data.get(index);
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    // ========== 0.1.10.20: quotes (spread and daily buy budget, see MarketQuote) ==========

    private static double spread() {
        return com.miophas.singularity_iteration.common.exchange.EconomySettings.spread();
    }

    /** 0.1.10.25 (X1): the daily buy limit for the menu data (32767 = none or more). */
    private static int buyLimit() {
        int cap = com.miophas.singularity_iteration.common.exchange.EconomySettings.buyItemsPerDay();
        return cap <= 0 ? 32767 : Math.min(cap, 32767);
    }

    /** Coins a purchase of {@code quantity} costs now. */
    public long buyCost(FutureCommodity commodity, int quantity) {
        return MarketQuote.buyCost(getCurrentPrice(commodity), quantity, spread());
    }

    /**
     * Coins a sale of {@code quantity} pays now on the sell curve (0.1.10.25: without the seller's own sales today, which
     * the exchange adds per player), or -1 off the server.
     */
    public long sellPayout(FutureCommodity commodity, int quantity) {
        if (!(level instanceof ServerLevel server)) return -1;
        var market = com.miophas.singularity_iteration.common.exchange.ExchangeService.market(server.getServer(), commodity.getItemId());
        if (market == null) return -1;
        var d = com.miophas.singularity_iteration.common.exchange.ExchangeData.get(server.getServer());
        return (long) Math.floor(com.miophas.singularity_iteration.common.exchange.SellDepth.payout(
            com.miophas.singularity_iteration.common.exchange.ExchangeService.curve(market), 0,
            com.miophas.singularity_iteration.common.exchange.ExchangeService.pressure(d, market.id()), quantity));
    }

    /** The quote of the selected commodity for the trade quantity (server side; 0 / -1 without a selection). */
    private long quote(boolean buying) {
        FutureCommodity commodity = level instanceof ServerLevel ? getSelectedCommodity() : null;
        if (commodity == null) return buying ? 0 : -1;
        long coins = buying ? buyCost(commodity, tradeQuantity) : sellPayout(commodity, tradeQuantity);
        return Math.min(coins, 0xFFFFFFFFL);
    }

    /** 0.1.10.25: the market buys any amount (on the sell curve); kept for the menu data layout. */
    private int marketRoom() {
        return 32767;
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
        
        // 没有事件时使用默认计算逻辑 (0.1.10.21: the formula moved unchanged to FuturePriceModel.step, which the futures pricing simulates)
        net.minecraft.util.RandomSource random = level != null ? level.random : net.minecraft.util.RandomSource.create();
        int base = commodity.getBasePrice();
        return com.miophas.singularity_iteration.common.future.FuturePriceModel.step(currentPrice, base, commodity.getVolatility(),
            random.nextDouble(), com.miophas.singularity_iteration.common.future.FuturePriceModel.minPrice(base),
            com.miophas.singularity_iteration.common.future.FuturePriceModel.maxPrice(base));
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
     * Gets the price history of a commodity (20 days).
     * The returned list uses index 0 = 19 days ago and index 19 = today.
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
 * Updates the price history cache. The cache is synchronized to the client.
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
     * Checks the market day. 0.1.10.27 (X2): the change of the day moved unchanged to {@link MarketDayDriver} (initialize the
     * market, prices, events, history, the exchange day), which the Trade Terminal runs as well; this machine then follows the
     * market's day.
     */
    public void checkDayUpdate(Level level) {
        if (tradeInProgress) return;
        if (!(level instanceof ServerLevel server)) return;
        boolean changed = MarketDayDriver.check(server);
        long marketDay = FutureMarketData.get(level).getCurrentDay();
        if (changed || currentDay != marketDay) {
            currentDay = marketDay;
            updatePriceHistoryCache();
            setChanged();
            syncToClient();
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

    /**
     * 0.1.10.21: the Buy / Sell buttons trade the trade quantity now through the exchange ({@code ExchangeService}):
     * cheaper resting orders of other players first, then the market (its treasury and daily import for a purchase,
     * its daily budget for a sale), never worse than the market's own price. The rest is not kept as an order.
     */
    private boolean executeTrade(Player player, boolean buying) {
        if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread()
                || !(player instanceof ServerPlayer customer) || tradeInProgress || !TRADING_PLAYERS.add(customer)) return false;
        tradeInProgress = true;
        try {
            if (!serves(customer)) return false;
            FutureCommodity commodity = getSelectedCommodity();
            if (commodity == null || commodity.getItem() == Items.AIR) return refuse(customer, "message.mio_icif.future.no_commodity_selected");
            var market = com.miophas.singularity_iteration.common.exchange.ExchangeService.market(world.getServer(), commodity.getItemId());
            if (market == null) return false;
            long quoted = buying ? Math.max(1, (buyCost(commodity, 1) * 11 + 9) / 10) : Math.max(1, sellPayout(commodity, 1));
            // 0.1.10.33: a sale may go under the band (the market's bid falls under it when much was sold)
            final long limit = buying ? Math.max(market.low(), Math.min(market.high(), quoted)) : Math.max(1, Math.min(market.high(), quoted));
            return exchangeAction(customer, () -> com.miophas.singularity_iteration.common.exchange.ExchangeService.place(
                customer, commodity.getItemId(), buying, false, limit, tradeQuantity));
        } finally {
            tradeInProgress = false;
            TRADING_PLAYERS.remove(customer);
        }
    }

    /** 0.1.10.21: the player uses this machine now (open menu, in reach, live machine): it may act on the exchange. */
    public boolean serves(ServerPlayer player) {
        return level instanceof ServerLevel world && player.containerMenu instanceof FutureElcMenu menu && authorized(world, player, menu);
    }

    /**
     * 0.1.10.21: runs one exchange action for the energy fee of a trade: the fee is reserved first and kept only when
     * the action succeeds. The result message goes to the action bar.
     */
    public boolean exchangeAction(ServerPlayer player, java.util.function.Supplier<com.miophas.singularity_iteration.common.exchange.ExchangeService.Result> action) {
        int fee = Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
        CustomEUEnergyStorage.InternalPayment payment = null;
        if (fee > 0) {
            payment = energyStorage.scexReserveInternal(fee);
            if (payment == null) return refuse(player, "message.mio_icif.future.no_energy");
        }
        boolean ok = false;
        try {
            var result = action.get();
            ok = result.ok();
            player.displayClientMessage(Component.translatable(result.key(), result.args()), true);
            if (ok && payment != null && !payment.commit()) throw new IllegalStateException("Future payment settled twice");
            if (ok) { setChanged(); player.inventoryMenu.broadcastChanges(); }
            return ok;
        } finally {
            if (payment != null && payment.isPending()) payment.cancel();
            // 0.1.10.26 (X3): the trade and bounty events of the action, after it is complete
            com.miophas.singularity_iteration.common.exchange.ExchangeEvents.flush(player.server);
        }
    }

    /** 0.1.10.20: tells the trader why a trade did not happen (before, a refused trade showed nothing). */
    private static boolean refuse(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
        return false;
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
     * Counts the coins that the player has.
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
 * Updates the player coin cache. The cache is used for display.
     */
    public void updatePlayerCoinsCache(Player player) {
        if (tradeInProgress) return;
        this.cachedPlayerCoins = countPlayerCoins(player);
        setChanged();
        syncToClient();
    }
    
    // ========== Business logic - categories and pages ==========
    
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
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("Category", currentCategory.name());
        tag.putInt("Page", currentPage);
        tag.putInt("SelectedIndex", selectedCommodityIndex);
        tag.putInt("TradeQuantity", tradeQuantity);
        tag.putLong("CurrentDay", currentDay);
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
        return new com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu(id, inventory, this, null, viewerData(player));
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
