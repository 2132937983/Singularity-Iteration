package com.singularity_iteration.mio_icif.future.event;

import java.util.Random;

/**
 * 价格稳定事件
 *
 * 效果：未来N天价格向基础价格回归，最终稳定在基础价格附近
 * 强度影响：强度越高，回归速度越快
 */
@SuppressWarnings("null")
public class PriceStabilizationEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public PriceStabilizationEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.PRICE_STABILIZATION;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        // 计算与基础价格的偏�?
        double deviation = (double) (currentPrice - basePrice) / basePrice;

        // 回归力度随天数和强度增加
        double regressionStrength = 0.2 + (intensity * 0.3);
        double dayProgress = (double) dayOffset / duration;
        double totalRegression = deviation * regressionStrength * (1 + dayProgress);

        // 添加微小随机波动 (-2% to +2%)
        double randomFactor = 0.98 + (RANDOM.nextDouble() * 0.04);

        // 计算新价�?
        int newPrice = (int) (currentPrice * (1.0 - totalRegression) * randomFactor);

        // 确保价格不会偏离基础价格太远（基础价格�?0%-150%�?
        int minPrice = (int) (basePrice * 0.5);
        int maxPrice = (int) (basePrice * 1.5);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


