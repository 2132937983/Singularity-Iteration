package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * Low volatility event.
 *
 * Effect: for the next N days, price changes become much smaller. Prices change slowly.
 * Intensity: a higher intensity gives smaller price changes.
 */
@SuppressWarnings("null")
public class LowVolatilityEvent extends FuturePriceEvent {


    public LowVolatilityEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.LOW_VOLATILITY;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        // Low volatility: the change is between -5% and +5%. A higher intensity gives a smaller range.
    double volatility = 0.05 * (1.0 - intensity * 0.5);
        double change = (random.getAsDouble() * 2 - 1) * volatility;

        int newPrice = (int) (currentPrice * (1.0 + change));

        // Apply the configured price range limits.
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


