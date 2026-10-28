// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.future;

import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.MarketDayRepair;
import com.miophas.singularity_iteration.common.future.event.FuturePriceEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * 0.1.10.27 (X2): the change of the market day, moved unchanged out of the futures machine
 * ({@code mio_icif_future_elc.checkDayUpdate}) so that the Trade Terminal advances the day as well: a server whose only
 * loaded exchange block is a terminal still clears the daily auction. Whichever block ticks first after the game day changed
 * runs it once; the others find the market day already current.
 *
 * <p>The day is the overworld's (before, the machine's own level: the same in the vanilla dimensions, which share the
 * overworld's time). The steps are those of 0.1.10.21-0.1.10.26: record today's prices, roll the history, draw events (with the day that
 * ends), move the prices of the commodities that reprice on the new day, record again, set the new day, then the exchange
 * day ({@link ExchangeService#onNewDay}).
 *
 * <p>0.1.10.33: the market day is no longer the world's day: it follows the world day's changes and never goes back
 * ({@link MarketDay}; {@code /time set day} counts as one day), and a market whose day went back under an older version is
 * repaired at the first check after loading ({@link MarketDayRepair}).
 */
public final class MarketDayDriver {
    private MarketDayDriver() {}

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Initializes the market or advances it to the game day; returns true when something changed. Server thread only. */
    public static boolean check(ServerLevel level) {
        // the overworld's day (0.1.10.27: a terminal or machine in another dimension follows the same day)
        long worldDay = level.getServer().overworld().getDayTime() / 24000L;
        FutureMarketData marketData = FutureMarketData.get(level);
        if (!marketData.isInitialized()) {
            for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
                String id = commodity.getItemId();
                marketData.setPrice(id, commodity.getBasePrice());
                marketData.initializeCommodityHistory(id, commodity.getBasePrice());
            }
            marketData.setCurrentDay(worldDay);
            marketData.setRawDay(worldDay);
            marketData.setInitialized(true);
            marketData.takeDayCheck();
            return true;
        }
        // 0.1.10.33: a save of an older version: the market day was the world's day when the market saw it last
        if (marketData.getRawDay() == MarketDay.UNSEEN) marketData.setRawDay(marketData.getCurrentDay());
        boolean repaired = false;
        if (marketData.takeDayCheck()) repaired = repair(level, marketData).changed();
        long currentDay = marketData.getCurrentDay();
        // 0.1.10.33: the market day follows the changes of the world's day and never goes back (MarketDay)
        long gameDay = MarketDay.next(currentDay, marketData.getRawDay(), worldDay);
        if (MarketDay.wentBack(marketData.getRawDay(), worldDay))
            LOGGER.info("The world's day went back from {} to {}: market day {} -> {} (the market day never goes back)",
                marketData.getRawDay(), worldDay, currentDay, gameDay);
        marketData.setRawDay(worldDay);
        if (gameDay == currentDay) return repaired;
        RandomSource random = level.random;
        recordTodayPrices(marketData);
        marketData.rollPriceHistory();
        // events, drawn with the day that ends
        marketData.cleanupExpiredEvents(currentDay);
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            if (marketData.getActiveEvent(commodity.getItemId(), currentDay) != null) continue;
            if (random.nextDouble() < FuturePriceModel.EVENT_CHANCE) {
                FuturePriceEvent event = FuturePriceModel.randomEvent(commodity.getItemId(), currentDay, random);
                if (event != null) marketData.addEvent(commodity.getItemId(), event);
            }
        }
        // prices of the commodities that reprice on the new day (datapack period_days)
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
            if (!commodity.repricesOn(gameDay)) continue;
            marketData.setPrice(commodity.getItemId(), newPrice(marketData, commodity, currentDay, random));
        }
        recordTodayPrices(marketData);
        marketData.setCurrentDay(gameDay);   // the daily counters of the market start again with the new day
        ExchangeService.onNewDay(level.getServer(), gameDay);
        return true;
    }

    /**
     * 0.1.10.33: the repair of a market day that went back under an older version ({@link MarketDayRepair}), once per load
     * and by {@code /mio_icif market day repair}.
     */
    public static MarketDayRepair.Report repair(ServerLevel level, FutureMarketData marketData) {
        MarketDayRepair.Report r = MarketDayRepair.repair(ExchangeService.data(level.getServer()), marketData);
        if (r.changed())
            LOGGER.warn("Repaired the market day: {} -> {} (latest stamp {}, {} records made after the world's day went back moved with it)",
                r.oldDay(), r.newDay(), r.latest(), r.moved());
        return r;
    }

    static void recordTodayPrices(FutureMarketData marketData) {
        for (FutureCommodity commodity : FutureCommodityManager.getCommodities())
            marketData.recordTodayPrice(commodity.getItemId(), marketData.getPrice(commodity.getItemId(), commodity.getBasePrice()));
    }

    /** The next price: the active event's effect, else {@link FuturePriceModel#step} (as in the futures machine). */
    static int newPrice(FutureMarketData marketData, FutureCommodity commodity, long currentDay, RandomSource random) {
        int currentPrice = marketData.getPrice(commodity.getItemId(), commodity.getBasePrice());
        int basePrice = commodity.getBasePrice();
        FuturePriceEvent activeEvent = marketData.getActiveEvent(commodity.getItemId(), currentDay);
        if (activeEvent != null) {
            int dayOffset = (int) (currentDay - activeEvent.getStartDay());
            return activeEvent.applyEffect(currentPrice, basePrice, dayOffset);
        }
        return FuturePriceModel.step(currentPrice, basePrice, commodity.getVolatility(), random.nextDouble(),
            FuturePriceModel.minPrice(basePrice), FuturePriceModel.maxPrice(basePrice));
    }
}
