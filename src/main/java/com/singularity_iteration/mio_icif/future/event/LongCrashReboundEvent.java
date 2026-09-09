package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 长期暴跌暴涨事件（崩盘反弹）
 *
 * 效果：长期内价格持续稳步下跌，然后在最后几天快速暴力? * 典型模式：前 80% 时间稳步下跌 5%-15%，最终?20% 时间暴涨 30%-50%
 * 强度影响：强度越高，整体波动幅度越大
 */
@SuppressWarnings("null")
public class LongCrashReboundEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public LongCrashReboundEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.LONG_CRASH_REBOUND;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        double totalChange;

        // 判断是下跌阶段还是反弹阶�
    // �?80% 的时间稳步下跌，�?20% 的时间快速暴力
    int crashDays = Math.max(1, (int) (duration * 0.8));

        if (dayOffset < crashDays) {
            // 下跌阶段：每天稳定下面?5% - 15%
            double baseFall = 0.05 + (0.10 * intensity);
            // 长期下跌，递减较慢
            double dayMultiplier = 1.0 + (dayOffset * 0.05);
            totalChange = -baseFall * dayMultiplier;

            // 较小的随机波动，保持相对稳定
            double randomFactor = 0.90 + (RANDOM.nextDouble() * 0.2);
            totalChange *= randomFactor;
        } else {
            // 反弹阶段：每天暴力?25% - 45%
            double baseRise = 0.25 + (0.20 * intensity);
            int reboundDay = dayOffset - crashDays;
            double dayMultiplier = 1.0 + (reboundDay * 0.25); // 暴涨加载
        totalChange = baseRise * dayMultiplier;

            // 较大的随机波动，模拟恐慌性买�
        double randomFactor = 0.80 + (RANDOM.nextDouble() * 0.4);
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


