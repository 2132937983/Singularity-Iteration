package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * 持续下跌事件
 *
 * Effect: the price falls for the next N days. The daily fall is 5%-15% of the price.
 * Intensity effect: a higher intensity gives a larger fall.
 */
@SuppressWarnings("null")
public class FallingTrendEvent extends FuturePriceEvent {


    public FallingTrendEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.FALLING_TREND;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        // 基础跌幅 5% - 15%，受强度影响
        double baseFall = 0.05 + (0.10 * intensity);
        // 随天数递增，最后一天跌幅最终
    double dayMultiplier = 1.0 + (dayOffset * 0.1);
        double totalChange = baseFall * dayMultiplier;

        // 添加随机波动 (-20% to +20%)
        double randomFactor = 0.8 + (random.getAsDouble() * 0.4);
        totalChange *= randomFactor;

        int newPrice = (int) (currentPrice * (1.0 - totalChange));

        // 使用配置的价格范围限制
    double minMultiplier = Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get();
        int minPrice = Math.max(1, (int) (basePrice * minMultiplier));

        return Math.max(newPrice, minPrice);
    }
}


