package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 持续下跌事件
 *
 * 效果：未来N天价格持续下跌，每天跌幅在基础价格�?%-15%之间
 * 强度影响：强度越高，跌幅越大
 */
@SuppressWarnings("null")
public class FallingTrendEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public FallingTrendEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.FALLING_TREND;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        // 基础跌幅 5% - 15%，受强度影响
        double baseFall = 0.05 + (0.10 * intensity);
        // 随天数递增，最后一天跌幅最终
    double dayMultiplier = 1.0 + (dayOffset * 0.1);
        double totalChange = baseFall * dayMultiplier;

        // 添加随机波动 (-20% to +20%)
        double randomFactor = 0.8 + (RANDOM.nextDouble() * 0.4);
        totalChange *= randomFactor;

        int newPrice = (int) (currentPrice * (1.0 - totalChange));

        // 使用配置的价格范围限制
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));

        return Math.max(newPrice, minPrice);
    }
}


