package com.miophas.singularity_iteration.common.future;

import com.miophas.singularity_iteration.common.future.event.FuturePriceEvent;
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
 * Global futures market data. This class is the database layer.
 *
 * Responsibilities:
 * 1. Persist the current price and the 20-day price history of all futures commodities.
 * 2. Persist the price events.
 * 3. Supply standard database CRUD operations.
 * 4. Manage the game day count and the price history roll.
 *
 * This class only stores data. It contains no business logic.
 */
@SuppressWarnings("null")
public class FutureMarketData extends SavedData {

    private static final String DATA_NAME = "mio_icif_future_market";
    private static final int HISTORY_DAYS = 20;

    // ========== Data storage ==========

    /** Current prices: key = commodity ID, value = current price. */
    private final Map<String, Integer> currentPrices = new HashMap<>();

    /** Price history: key = commodity ID, value = list of 20 daily prices (index 0 = 19 days ago, index 19 = today). */
    private final Map<String, List<Integer>> priceHistory = new HashMap<>();

    /** Price events: key = commodity ID, value = list of active events for that commodity. */
    private final Map<String, List<FuturePriceEvent>> activeEvents = new HashMap<>();

    /** Current game day. */
    private long currentDay = 0;

    /** True when the data is initialized. */
    private boolean initialized = false;

    // ========== Constructor ==========

    public FutureMarketData() {}

    // ========== Singleton access ==========

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

    // ========== Data loading ==========

    public static FutureMarketData load(CompoundTag tag, HolderLookup.Provider provider) {
        FutureMarketData data = new FutureMarketData();

        data.currentDay = tag.getLong("CurrentDay");
        data.initialized = tag.getBoolean("Initialized");

        // Load the current prices.
        CompoundTag pricesTag = tag.getCompound("CurrentPrices");
        for (String key : pricesTag.getAllKeys()) {
            data.currentPrices.put(key, pricesTag.getInt(key));
        }

        // Load the price history.
        CompoundTag historyTag = tag.getCompound("PriceHistory");
        for (String commodityId : historyTag.getAllKeys()) {
            ListTag list = historyTag.getList(commodityId, Tag.TAG_INT);
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                prices.add(list.getInt(i));
            }
            data.priceHistory.put(commodityId, prices);
        }

        // Load the price events.
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
                    // Ignore an invalid event.
                }
            }
            data.activeEvents.put(commodityId, events);
        }

        return data;
    }

    // ========== Data saving ==========

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("CurrentDay", currentDay);
        tag.putBoolean("Initialized", initialized);

        // Save the current prices.
        CompoundTag pricesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : currentPrices.entrySet()) {
            pricesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("CurrentPrices", pricesTag);

        // Save the price history.
        CompoundTag historyTag = new CompoundTag();
        for (Map.Entry<String, List<Integer>> entry : priceHistory.entrySet()) {
            ListTag list = new ListTag();
            for (Integer price : entry.getValue()) {
                list.add(net.minecraft.nbt.IntTag.valueOf(price));
            }
            historyTag.put(entry.getKey(), list);
        }
        tag.put("PriceHistory", historyTag);

        // Save the price events.
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

    // ========== Standard database operations: current prices ==========

    /**
     * Gets the current price of a commodity.
     * @param commodityId the commodity ID
     * @param defaultPrice the default price. The method returns it when no price is found.
     * @return the current price
     */
    public int getPrice(String commodityId, int defaultPrice) {
        return currentPrices.getOrDefault(commodityId, defaultPrice);
    }

    /**
     * Sets the current price of a commodity.
     * @param commodityId the commodity ID
     * @param price the new price
     */
    public void setPrice(String commodityId, int price) {
        currentPrices.put(commodityId, price);
        setDirty();
    }

    /**
     * Sets several prices at once.
     * @param prices the price map
     */
    public void setPrices(Map<String, Integer> prices) {
        currentPrices.putAll(prices);
        setDirty();
    }

    /**
     * Returns all current prices.
     * @return a copy of the price map
     */
    public Map<String, Integer> getAllPrices() {
        return new HashMap<>(currentPrices);
    }

    // ========== Standard database operations: price history ==========

    /**
     * Gets the price history of a commodity.
     * @param commodityId the commodity ID
     * @return the list of 20 daily prices (index 0 = 19 days ago, index 19 = today), or an empty list if no history exists
     */
    public List<Integer> getPriceHistory(String commodityId) {
        List<Integer> history = priceHistory.get(commodityId);
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history);
    }

    /**
     * Sets the price history of a commodity. It replaces the existing history.
     * @param commodityId the commodity ID
     * @param history the price history list
     */
    public void setPriceHistory(String commodityId, List<Integer> history) {
        priceHistory.put(commodityId, new ArrayList<>(history));
        setDirty();
    }

    /**
     * Records the price of today at the end of the history.
     * @param commodityId the commodity ID
     * @param price the price of today
     */
    public void recordTodayPrice(String commodityId, int price) {
        List<Integer> history = priceHistory.computeIfAbsent(commodityId, k -> new ArrayList<>());

        // Fill the list to 20 entries with the current price. This prevents zero values.
        while (history.size() < HISTORY_DAYS) {
            history.add(Math.max(1, price));
        }

        // Set the price for today (the last element). The price is at least 1.
        history.set(HISTORY_DAYS - 1, Math.max(1, price));
        setDirty();
    }

    /**
     * Rolls the price history forward by one day.
     * Removes the oldest day and copies the last price to the new day. This prevents zero values.
     */
    public void rollPriceHistory() {
        for (List<Integer> history : priceHistory.values()) {
            if (!history.isEmpty()) {
                // Get the price of the last day. The price is at least 1.
                int lastPrice = Math.max(1, history.get(history.size() - 1));
                history.remove(0);
                history.add(lastPrice);
            }
        }
        setDirty();
    }

    /**
     * Initializes the price history of a commodity.
     * @param commodityId the commodity ID
     * @param basePrice the base price that fills the history
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

    // ========== Standard database operations: day count ==========

    /**
     * Gets the current game day.
     * @return the day number
     */
    public long getCurrentDay() {
        return currentDay;
    }

    /**
     * Sets the current game day.
     * @param day the day number
     */
    public void setCurrentDay(long day) {
        this.currentDay = day;
        setDirty();
    }

    // ========== Standard database operations: initialization state ==========

    /**
     * Checks if the market data is initialized.
     * @return true if the market data is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets the initialization state.
     * @param initialized the initialization state
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        setDirty();
    }

    // ========== Standard database operations: price events ==========

    /**
     * Adds a price event.
     * @param commodityId the commodity ID
     * @param event the price event
     */
    public void addEvent(String commodityId, FuturePriceEvent event) {
        activeEvents.computeIfAbsent(commodityId, k -> new ArrayList<>()).add(event);
        setDirty();
    }

    /**
     * Returns the active events of a commodity.
     * @param commodityId the commodity ID
     * @return the event list, or an empty list if there are no events
     */
    public List<FuturePriceEvent> getEvents(String commodityId) {
        List<FuturePriceEvent> events = activeEvents.get(commodityId);
        if (events == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(events);
    }

    /**
     * Returns all active events.
     * @return the event map
     */
    public Map<String, List<FuturePriceEvent>> getAllEvents() {
        Map<String, List<FuturePriceEvent>> result = new HashMap<>();
        for (Map.Entry<String, List<FuturePriceEvent>> entry : activeEvents.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    /**
     * Removes expired events.
     * @param commodityId the commodity ID
     * @param currentDay the current game day
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
     * Removes all expired events.
     * @param currentDay the current game day
     */
    public void cleanupExpiredEvents(long currentDay) {
        for (String commodityId : new ArrayList<>(activeEvents.keySet())) {
            removeExpiredEvents(commodityId, currentDay);
        }
    }

    /**
     * Returns the active event of a commodity for the price calculation.
     * @param commodityId the commodity ID
     * @param currentDay the current game day
     * @return the active event, or null if there is no active event
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

    // ========== Constants ==========

    public static int getHistoryDays() {
        return HISTORY_DAYS;
    }
}


