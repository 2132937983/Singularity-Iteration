// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.future;

import com.miophas.singularity_iteration.common.exchange.ExchangeService;
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
 */
public final class MarketDayDriver {
    private MarketDayDriver() {}

    /** Initializes the market or advances it to the game day; returns true when something changed. Server thread only. */
    public static boolean check(ServerLevel level) {
        // the overworld's day (0.1.10.27: a terminal or machine in another dimension follows the same day)
        long gameDay = level.getServer().overworld().getDayTime() / 24000L;
        FutureMarketData marketData = FutureMarketData.get(level);
        if (!marketData.isInitialized()) {
            for (FutureCommodity commodity : FutureCommodityManager.getCommodities()) {
                String id = commodity.getItemId();
                marketData.setPrice(id, commodity.getBasePrice());
                marketData.initializeCommodityHistory(id, commodity.getBasePrice());
            }
            marketData.setCurrentDay(gameDay);
            marketData.setInitialized(true);
            return true;
        }
        long currentDay = marketData.getCurrentDay();
        if (gameDay == currentDay) return false;
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
