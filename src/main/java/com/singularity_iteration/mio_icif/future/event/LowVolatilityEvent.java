package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 低波动事? *
 * 效果：未来N天价格波动幅度大幅减小，价格变化平
 * 强度影响：强度越高，波动越小
 */
@SuppressWarnings("null")
public class LowVolatilityEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public LowVolatilityEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.LOW_VOLATILITY;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        // 低波动：-5% ?+5% 的变化，受强度影响（强度越高波动越小
    double volatility = 0.05 * (1.0 - intensity * 0.5);
        double change = (RANDOM.nextDouble() * 2 - 1) * volatility;

        int newPrice = (int) (currentPrice * (1.0 + change));

        // 使用配置的价格范围限
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));
        int maxPrice = (int) (basePrice * maxMultiplier);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


