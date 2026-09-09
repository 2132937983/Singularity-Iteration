package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 短期暴跌暴涨事件（闪崩反弹）
 *
 * 效果：短期内价格快速暴跌，然后在最后几天强势反�? * 典型模式：前几天暴跌 20%-40%，最终?-2天暴力?30%-50%
 * 强度影响：强度越高，跌幅和反弹幅度都越大
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

        // 判断是暴跌阶段还是反弹阶�
    // �?70% 的时间暴跌，�?30% 的时间强势反�
    int crashDays = Math.max(1, (int) (duration * 0.7));

        if (dayOffset < crashDays) {
            // 暴跌阶段：每天暴力?15% - 35%
            double baseFall = 0.15 + (0.20 * intensity);
            double dayMultiplier = 1.0 + (dayOffset * 0.15); // 越往后跌得越�
        totalChange = -baseFall * dayMultiplier;

            // 添加随机波动
            double randomFactor = 0.85 + (RANDOM.nextDouble() * 0.3);
            totalChange *= randomFactor;
        } else {
            // 反弹阶段：每天暴力?25% - 45%
            double baseRise = 0.25 + (0.20 * intensity);
            int reboundDay = dayOffset - crashDays;
            double dayMultiplier = 1.0 + (reboundDay * 0.2); // 越往后反弹越�
        totalChange = baseRise * dayMultiplier;

            // 添加随机波动
            double randomFactor = 0.85 + (RANDOM.nextDouble() * 0.3);
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


