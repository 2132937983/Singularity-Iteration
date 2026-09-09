package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 短期暴涨暴跌事件（泡沫）
 *
 * 效果：短期内价格快速暴涨，然后在最后几天暴力? * 典型模式：前几天暴涨 20%-40%，最终?-2天暴力?30%-50%
 * 强度影响：强度越高，涨幅和跌幅都越大
 */
@SuppressWarnings("null")
public class ShortBubbleEvent extends FuturePriceEvent {

    private static final Random RANDOM = new Random();

    public ShortBubbleEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.SHORT_BUBBLE;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        double totalChange;

        // 判断是上涨阶段还是暴跌阶�
    // �?70% 的时间上涨，�?30% 的时间暴力
    int riseDays = Math.max(1, (int) (duration * 0.7));

        if (dayOffset < riseDays) {
            // 上涨阶段：每天暴力?15% - 35%
            double baseRise = 0.15 + (0.20 * intensity);
            double dayMultiplier = 1.0 + (dayOffset * 0.15); // 越往后涨得越�
        totalChange = baseRise * dayMultiplier;

            // 添加随机波动
            double randomFactor = 0.85 + (RANDOM.nextDouble() * 0.3);
            totalChange *= randomFactor;
        } else {
            // 暴跌阶段：每天暴力?20% - 40%
            double baseFall = 0.20 + (0.20 * intensity);
            int crashDay = dayOffset - riseDays;
            double dayMultiplier = 1.0 + (crashDay * 0.2); // 越往后跌得越�
        totalChange = -baseFall * dayMultiplier;

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


