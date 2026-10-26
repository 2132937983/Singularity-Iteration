package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * Long-term crash and surge event (crash rebound).
 *
 * Effect: the price falls steadily for a long time. Then it rises quickly and sharply in the last days.
 * Typical pattern: the price falls steadily by 5%-15% for the first 80% of the time. It rises by 30%-50% in the last 20% of the time.
 * Intensity: a higher intensity gives a larger total fluctuation.
 */
@SuppressWarnings("null")
public class LongCrashReboundEvent extends FuturePriceEvent {


    public LongCrashReboundEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.LONG_CRASH_REBOUND;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        double totalChange;

        // Decide if the event is in the crash phase or the rebound phase.
    // The price falls steadily for the first 80% of the duration. It rises sharply in the last 20%.
    int crashDays = Math.max(1, (int) (duration * 0.8));

        if (dayOffset < crashDays) {
            // Crash phase: the price falls steadily by 5% - 15% each day.
            double baseFall = 0.05 + (0.10 * intensity);
            // Long-term fall. The fall increases slowly.
            double dayMultiplier = 1.0 + (dayOffset * 0.05);
            totalChange = -baseFall * dayMultiplier;

            // Use a small random fluctuation to keep the price relatively stable.
            double randomFactor = 0.90 + (random.getAsDouble() * 0.2);
            totalChange *= randomFactor;
        } else {
            // Rebound phase: the price rises sharply by 25% - 45% each day.
            double baseRise = 0.25 + (0.20 * intensity);
            int reboundDay = dayOffset - crashDays;
            double dayMultiplier = 1.0 + (reboundDay * 0.25); // The surge accelerates.
        totalChange = baseRise * dayMultiplier;

            // Use a large random fluctuation to simulate panic buying.
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


