// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.future;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.future.event.FallingTrendEvent;
import com.miophas.singularity_iteration.common.future.event.FuturePriceEvent;
import com.miophas.singularity_iteration.common.future.event.HighVolatilityEvent;
import com.miophas.singularity_iteration.common.future.event.LongBubbleEvent;
import com.miophas.singularity_iteration.common.future.event.LongCrashReboundEvent;
import com.miophas.singularity_iteration.common.future.event.LowVolatilityEvent;
import com.miophas.singularity_iteration.common.future.event.PriceStabilizationEvent;
import com.miophas.singularity_iteration.common.future.event.RisingTrendEvent;
import com.miophas.singularity_iteration.common.future.event.ShortBubbleEvent;
import com.miophas.singularity_iteration.common.future.event.ShortCrashReboundEvent;
import java.util.function.DoubleSupplier;
import net.minecraft.util.RandomSource;

/**
 * 0.1.10.21: the daily price model of the futures market in one place, so that the machine and the futures pricing
 * ({@code FuturesPricer}) use the same rules. Unchanged from before: when a day ends, a commodity without an event gets
 * one with {@link #EVENT_CHANCE}; then its price moves by the event, or else by a random step with mean reversion
 * ({@link #step}).
 */
public final class FuturePriceModel {
    private FuturePriceModel() {}

    /** Chance per day that a commodity without an active event gets a new one. */
    public static final double EVENT_CHANCE = 0.2;

    /**
     * A day without an event: a uniform random change of up to {@code volatility} either way, minus the reversion
     * toward the base price (15 %, half again as strong below the base price, stronger beyond half the base price away,
     * at most 60 %), rounded down and kept within the price range. Pure.
     *
     * @param unit a uniform random number in [0, 1)
     */
    public static int step(int price, int base, double volatility, double unit, int minPrice, int maxPrice) {
        double deviation = (double) (price - base) / base;
        double direction = deviation < 0 ? 1.5 : 1.0;
        double extreme = Math.max(0, Math.abs(deviation) - 0.5);
        double regression = Math.min(0.15 * direction + extreme * 0.2, 0.6);
        double change = (unit - 0.5) * 2 * volatility - deviation * regression;
        int next = (int) (price * (1 + change));
        return Math.max(minPrice, Math.min(maxPrice, next));
    }

    public static int minPrice(int base) {
        return Math.max(1, (int) (base * Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get()));
    }

    public static int maxPrice(int base) {
        return (int) (base * Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get());
    }

    /** The price after the day {@code day} ends: the effect of {@code event} (active on that day), else {@link #step}. */
    public static int next(FutureCommodity commodity, int price, FuturePriceEvent event, long day, DoubleSupplier random) {
        int base = commodity.getBasePrice();
        if (event != null) return event.applyEffect(price, base, (int) (day - event.getStartDay()), random);
        return step(price, base, commodity.getVolatility(), random.getAsDouble(), minPrice(base), maxPrice(base));
    }

    /** A random event from {@code startDay}: type, then duration and intensity by the kind of event. */
    public static FuturePriceEvent randomEvent(String commodityId, long startDay, RandomSource random) {
        FuturePriceEvent.EventType[] types = FuturePriceEvent.EventType.values();
        FuturePriceEvent.EventType type = types[random.nextInt(types.length)];
        int duration;
        double intensity;
        switch (type) {
            case SHORT_BUBBLE, SHORT_CRASH_REBOUND -> {
                duration = 3 + random.nextInt(3);                 // 3 - 5 days
                intensity = 0.8 + random.nextDouble() * 0.7;      // 0.8 - 1.5
            }
            case LONG_BUBBLE, LONG_CRASH_REBOUND -> {
                duration = 6 + random.nextInt(5);                 // 6 - 10 days
                intensity = 0.6 + random.nextDouble() * 0.6;      // 0.6 - 1.2
            }
            default -> {
                duration = 3 + random.nextInt(5);                 // 3 - 7 days
                intensity = 0.5 + random.nextDouble();            // 0.5 - 1.5
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
}
