package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;

import java.util.Random;

/**
 * Short-term crash and rebound event (flash crash rebound).
 *
 * Effect: the price falls fast for a short time. Then it rises strongly in the last days.
 * Typical pattern: the price falls 20%-40% in the first days. It then rises 30%-50% in the last 1-2 days.
 * Intensity: a higher intensity gives a larger fall and a larger rebound.
 */
@SuppressWarnings("null")
public class ShortCrashReboundEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public ShortCrashReboundEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.SHORT_CRASH_REBOUND;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        double totalChange;

        // Find whether the day is in the crash phase or in the rebound phase.
    // The first 70% of the duration is the crash. The last 30% is the strong rebound.
    int crashDays = Math.max(1, (int) (duration * 0.7));

        if (dayOffset < crashDays) {
            // Crash phase: the price falls sharply by 15% - 35% each day.
            double baseFall = 0.15 + (0.20 * intensity);
            double dayMultiplier = 1.0 + (dayOffset * 0.15); // Later days fall more.
        totalChange = -baseFall * dayMultiplier;

            // Add a random fluctuation.
            double randomFactor = 0.85 + (RANDOM.nextDouble() * 0.3);
            totalChange *= randomFactor;
        } else {
            // Rebound phase: the price rises sharply by 25% - 45% each day.
            double baseRise = 0.25 + (0.20 * intensity);
            int reboundDay = dayOffset - crashDays;
            double dayMultiplier = 1.0 + (reboundDay * 0.2); // Later days rise more.
        totalChange = baseRise * dayMultiplier;

            // Add a random fluctuation.
            double randomFactor = 0.85 + (RANDOM.nextDouble() * 0.3);
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


