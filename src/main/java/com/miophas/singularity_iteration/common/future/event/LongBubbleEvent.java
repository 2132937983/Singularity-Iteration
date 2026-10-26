package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * Long-term rise and crash event (large bubble).
 *
 * Effect: the price rises steadily for a long time. Then it falls fast in the last days.
 * Typical pattern: in the first 80% of the duration, the price rises 5%-15%. In the last 20%, it falls 30%-50%.
 * Intensity: a higher intensity gives larger price changes overall.
 */
@SuppressWarnings("null")
public class LongBubbleEvent extends FuturePriceEvent {


    public LongBubbleEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.LONG_BUBBLE;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        double totalChange;

        // Find whether the day is in the rise phase or in the crash phase.
    // The first 80% of the duration is a steady rise. The last 20% is a fast crash.
    int riseDays = Math.max(1, (int) (duration * 0.8));

        if (dayOffset < riseDays) {
            // Rise phase: the price rises 5% - 15% each day.
            double baseRise = 0.05 + (0.10 * intensity);
            // Long rise. The increase grows slowly.
            double dayMultiplier = 1.0 + (dayOffset * 0.05);
            totalChange = baseRise * dayMultiplier;

            // Small random change. The price stays almost stable.
            double randomFactor = 0.90 + (random.getAsDouble() * 0.2);
            totalChange *= randomFactor;
        } else {
            // Crash phase: the price falls 25% - 45% each day.
            double baseFall = 0.25 + (0.20 * intensity);
            int crashDay = dayOffset - riseDays;
            double dayMultiplier = 1.0 + (crashDay * 0.25); // The crash becomes faster each day
        totalChange = -baseFall * dayMultiplier;

            // Large random change that simulates panic selling.
        double randomFactor = 0.80 + (random.getAsDouble() * 0.4);
            totalChange *= randomFactor;
        }

        int newPrice = (int) (currentPrice * (1.0 + totalChange));

        // Limit the price to the configured price range.
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


