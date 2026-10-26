package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * Short-term spike and crash event (bubble).
 *
 * Effect: the price rises quickly for a short time. Then the price crashes in the last days.
 * Typical pattern: the price rises 20%-40% in the first days. In the last 1-2 days, the price falls 30%-50%.
 * Intensity: a higher intensity gives a larger rise and a larger fall.
 */
@SuppressWarnings("null")
public class ShortBubbleEvent extends FuturePriceEvent {


    public ShortBubbleEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.SHORT_BUBBLE;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        double totalChange;

        // Check if the price is in the rise phase or the crash phase.
    // The price rises for the first 70% of the duration and crashes for the last 30%.
    int riseDays = Math.max(1, (int) (duration * 0.7));

        if (dayOffset < riseDays) {
            // Rise phase: the price rises 15% - 35% each day.
            double baseRise = 0.15 + (0.20 * intensity);
            double dayMultiplier = 1.0 + (dayOffset * 0.15); // The rise gets faster each day
        totalChange = baseRise * dayMultiplier;

            // 添加随机波动
            double randomFactor = 0.85 + (random.getAsDouble() * 0.3);
            totalChange *= randomFactor;
        } else {
            // Crash phase: the price falls 20% - 40% each day.
            double baseFall = 0.20 + (0.20 * intensity);
            int crashDay = dayOffset - riseDays;
            double dayMultiplier = 1.0 + (crashDay * 0.2); // The fall gets faster each day
        totalChange = -baseFall * dayMultiplier;

            // 添加随机波动
            double randomFactor = 0.85 + (random.getAsDouble() * 0.3);
            totalChange *= randomFactor;
        }

        int newPrice = (int) (currentPrice * (1.0 + totalChange));

        // 使用配置的价格范围限制
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


