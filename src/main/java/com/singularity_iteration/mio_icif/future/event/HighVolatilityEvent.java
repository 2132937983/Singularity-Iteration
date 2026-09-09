package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 高波动事�? *
 * 效果：未来N天价格波动幅度大幅增加，可能出现剧烈涨跌
 * 强度影响：强度越高，波动范围越大
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
        // 高波动：-30% �?+30% 的变化，受强度影�
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


