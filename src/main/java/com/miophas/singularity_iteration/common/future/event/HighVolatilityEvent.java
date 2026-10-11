package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;

import java.util.Random;

/**
 * High volatility event.
 *
 * Effect: for the next N days, price changes become much larger. Sudden rises and falls can occur.
 * Intensity: a higher intensity gives a larger range of change.
 */
@SuppressWarnings("null")
public class HighVolatilityEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public HighVolatilityEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.HIGH_VOLATILITY;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        // High volatility: the change is between -30% and +30%, multiplied by the intensity.
    double volatility = 0.30 * intensity;
        double change = (RANDOM.nextDouble() * 2 - 1) * volatility;

        int newPrice = (int) (currentPrice * (1.0 + change));

        // 使用配置的价格范围限制
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


