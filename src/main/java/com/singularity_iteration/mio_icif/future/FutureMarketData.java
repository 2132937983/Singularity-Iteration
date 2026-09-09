package com.singularity_iteration.mio_icif.future;

import com.singularity_iteration.mio_icif.future.event.FuturePriceEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 期货市场全局数据 - 数据库层
 * 
 * 职责�? * 1. 持久化存储所有期货货品的当前价格�?0天历史价�? * 2. 持久化存储价格事�? * 3. 提供标准的数据库CRUD操作
 * 4. 管理游戏天数和价格滚�? * 
 * 注意：此类只负责数据存储，不涉及业务逻辑
 */
@SuppressWarnings("null")
public class FutureMarketData extends SavedData {
    
    private static final String DATA_NAME = "mio_icif_future_market";
    private static final int HISTORY_DAYS = 20;
    
    // ========== 数据存储 ==========
    
    /** 当前价格：key=货品ID, value=当前价格 */
    private final Map<String, Integer> currentPrices = new HashMap<>();
    
    /** 价格历史：key=货品ID, value=20天价格列�?索引0=19天前, 索引19=今天) */
    private final Map<String, List<Integer>> priceHistory = new HashMap<>();
    
    /** 价格事件：key=货品ID, value=该货品的活跃事件列表 */
    private final Map<String, List<FuturePriceEvent>> activeEvents = new HashMap<>();
    
    /** 当前游戏天数 */
    private long currentDay = 0;
    
    /** 是否已初始化 */
    private boolean initialized = false;
    
    // ========== 构造函数?==========
    
    public FutureMarketData() {}
    
    // ========== 单例获取 ==========
    
    public static FutureMarketData get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DimensionDataStorage storage = serverLevel.getDataStorage();
            return storage.computeIfAbsent(
                new Factory<>(FutureMarketData::new, FutureMarketData::load),
                DATA_NAME
            );
        }
        throw new IllegalStateException("Cannot get FutureMarketData on client side");
    }
    
    // ========== 数据加载 ==========
    
    public static FutureMarketData load(CompoundTag tag, HolderLookup.Provider provider) {
        FutureMarketData data = new FutureMarketData();
        
        data.currentDay = tag.getLong("CurrentDay");
        data.initialized = tag.getBoolean("Initialized");
        
        // 加载当前价格
        CompoundTag pricesTag = tag.getCompound("CurrentPrices");
        for (String key : pricesTag.getAllKeys()) {
            data.currentPrices.put(key, pricesTag.getInt(key));
        }
        
        // 加载价格历史
        CompoundTag historyTag = tag.getCompound("PriceHistory");
        for (String commodityId : historyTag.getAllKeys()) {
            ListTag list = historyTag.getList(commodityId, Tag.TAG_INT);
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                prices.add(list.getInt(i));
            }
            data.priceHistory.put(commodityId, prices);
        }
        
        // 加载价格事件
        CompoundTag eventsTag = tag.getCompound("ActiveEvents");
        for (String commodityId : eventsTag.getAllKeys()) {
            ListTag eventList = eventsTag.getList(commodityId, Tag.TAG_COMPOUND);
            List<FuturePriceEvent> events = new ArrayList<>();
            for (int i = 0; i < eventList.size(); i++) {
                CompoundTag eventTag = eventList.getCompound(i);
                try {
                    FuturePriceEvent event = FuturePriceEvent.deserialize(eventTag);
                    events.add(event);
                } catch (Exception e) {
                    // 忽略无效事件
                }
            }
            data.activeEvents.put(commodityId, events);
        }
        
        return data;
    }
    
    // ========== 数据保存 ==========
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("CurrentDay", currentDay);
        tag.putBoolean("Initialized", initialized);
        
        // 保存当前价格
        CompoundTag pricesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : currentPrices.entrySet()) {
            pricesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("CurrentPrices", pricesTag);
        
        // 保存价格历史
        CompoundTag historyTag = new CompoundTag();
        for (Map.Entry<String, List<Integer>> entry : priceHistory.entrySet()) {
            ListTag list = new ListTag();
            for (Integer price : entry.getValue()) {
                list.add(net.minecraft.nbt.IntTag.valueOf(price));
            }
            historyTag.put(entry.getKey(), list);
        }
        tag.put("PriceHistory", historyTag);
        
        // 保存价格事件
        CompoundTag eventsTag = new CompoundTag();
        for (Map.Entry<String, List<FuturePriceEvent>> entry : activeEvents.entrySet()) {
            ListTag eventList = new ListTag();
            for (FuturePriceEvent event : entry.getValue()) {
                eventList.add(event.serialize());
            }
            eventsTag.put(entry.getKey(), eventList);
        }
        tag.put("ActiveEvents", eventsTag);
        
        return tag;
    }
    
    // ========== 标准数据库操�?- 当前价格 ==========
    
    /**
     * 获取货品当前价格
     * @param commodityId 货品ID
     * @param defaultPrice 默认价格（未找到时返回）
     * @return 当前价格
     */
    public int getPrice(String commodityId, int defaultPrice) {
        return currentPrices.getOrDefault(commodityId, defaultPrice);
    }
    
    /**
     * 设置货品当前价格
     * @param commodityId 货品ID
     * @param price 新价�?     */
    public void setPrice(String commodityId, int price) {
        currentPrices.put(commodityId, price);
        setDirty();
    }
    
    /**
     * 批量设置价格
     * @param prices 价格映射
     */
    public void setPrices(Map<String, Integer> prices) {
        currentPrices.putAll(prices);
        setDirty();
    }
    
    /**
     * 获取所有当前价�?     * @return 价格映射副本
     */
    public Map<String, Integer> getAllPrices() {
        return new HashMap<>(currentPrices);
    }
    
    // ========== 标准数据库操�?- 价格历史 ==========
    
    /**
     * 获取货品价格历史
     * @param commodityId 货品ID
     * @return 20天价格列表（索引0=19天前，索引?9=今天），如果不存在返回空列表
     */
    public List<Integer> getPriceHistory(String commodityId) {
        List<Integer> history = priceHistory.get(commodityId);
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history);
    }
    
    /**
     * 设置货品价格历史（覆盖）
     * @param commodityId 货品ID
     * @param history 价格历史列表
     */
    public void setPriceHistory(String commodityId, List<Integer> history) {
        priceHistory.put(commodityId, new ArrayList<>(history));
        setDirty();
    }
    
    /**
     * 记录当天价格到历史末�?     * @param commodityId 货品ID
     * @param price 当天价格
     */
    public void recordTodayPrice(String commodityId, int price) {
        List<Integer> history = priceHistory.computeIfAbsent(commodityId, k -> new ArrayList<>());
        
        // 确保列表�?0个元素，用当前价格填充（避免0值）
        while (history.size() < HISTORY_DAYS) {
            history.add(Math.max(1, price));
        }
        
        // 设置今天的价格（最后一个元素），确保至少为1
        history.set(HISTORY_DAYS - 1, Math.max(1, price));
        setDirty();
    }
    
    /**
     * 滚动价格历史（新的一天）
     * 移除最旧的一天，复制最后一天的价格（避�?值）
     */
    public void rollPriceHistory() {
        for (List<Integer> history : priceHistory.values()) {
            if (!history.isEmpty()) {
                // 获取最后一天的价格，确保至少为1
                int lastPrice = Math.max(1, history.get(history.size() - 1));
                history.remove(0);
                history.add(lastPrice);
            }
        }
        setDirty();
    }
    
    /**
     * 初始化货品价格历�?     * @param commodityId 货品ID
     * @param basePrice 基础价格（用于填充历史）
     */
    public void initializeCommodityHistory(String commodityId, int basePrice) {
        if (!priceHistory.containsKey(commodityId)) {
            List<Integer> history = new ArrayList<>();
            for (int i = 0; i < HISTORY_DAYS; i++) {
                history.add(basePrice);
            }
            priceHistory.put(commodityId, history);
            setDirty();
        }
    }
    
    // ========== 标准数据库操�?- 天数管理 ==========
    
    /**
     * 获取当前游戏天数
     * @return 天数
     */
    public long getCurrentDay() {
        return currentDay;
    }
    
    /**
     * 设置当前游戏天数
     * @param day 天数
     */
    public void setCurrentDay(long day) {
        this.currentDay = day;
        setDirty();
    }
    
    // ========== 标准数据库操�?- 初始化状态?==========
    
    /**
     * 检查是否已初始�?     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 设置初始化状态?     * @param initialized 初始化状态?     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        setDirty();
    }
    
    // ========== 标准数据库操�?- 价格事件 ==========
    
    /**
     * 添加价格事件
     * @param commodityId 货品ID
     * @param event 价格事件
     */
    public void addEvent(String commodityId, FuturePriceEvent event) {
        activeEvents.computeIfAbsent(commodityId, k -> new ArrayList<>()).add(event);
        setDirty();
    }
    
    /**
     * 获取货品的活跃事件列�?     * @param commodityId 货品ID
     * @return 事件列表，如果没有返回空列表
     */
    public List<FuturePriceEvent> getEvents(String commodityId) {
        List<FuturePriceEvent> events = activeEvents.get(commodityId);
        if (events == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(events);
    }
    
    /**
     * 获取所有活跃事�?     * @return 事件映射
     */
    public Map<String, List<FuturePriceEvent>> getAllEvents() {
        Map<String, List<FuturePriceEvent>> result = new HashMap<>();
        for (Map.Entry<String, List<FuturePriceEvent>> entry : activeEvents.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * 移除过期事件
     * @param commodityId 货品ID
     * @param currentDay 当前游戏天数
     */
    public void removeExpiredEvents(String commodityId, long currentDay) {
        List<FuturePriceEvent> events = activeEvents.get(commodityId);
        if (events != null) {
            events.removeIf(event -> !event.isActive(currentDay));
            if (events.isEmpty()) {
                activeEvents.remove(commodityId);
            }
            setDirty();
        }
    }
    
    /**
     * 清理所有过期事�?     * @param currentDay 当前游戏天数
     */
    public void cleanupExpiredEvents(long currentDay) {
        for (String commodityId : new ArrayList<>(activeEvents.keySet())) {
            removeExpiredEvents(commodityId, currentDay);
        }
    }
    
    /**
     * 获取货品的活跃事件（用于价格计算�?     * @param commodityId 货品ID
     * @param currentDay 当前游戏天数
     * @return 活跃事件，如果没有返回null
     */
    public FuturePriceEvent getActiveEvent(String commodityId, long currentDay) {
        List<FuturePriceEvent> events = activeEvents.get(commodityId);
        if (events != null) {
            for (FuturePriceEvent event : events) {
                if (event.isActive(currentDay)) {
                    return event;
                }
            }
        }
        return null;
    }
    
    // ========== 常量获取 ==========
    
    public static int getHistoryDays() {
        return HISTORY_DAYS;
    }
}


