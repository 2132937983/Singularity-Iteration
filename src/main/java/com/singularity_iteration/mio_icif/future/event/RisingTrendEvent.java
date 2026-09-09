package com.singularity_iteration.mio_icif.future.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;

import java.util.Random;

/**
 * 持续上涨事件
 * 
 * 效果：未来N天价格持续上涨，每天涨幅在基础价格�?%-15%之间
 * 强度影响：强度越高，涨幅越大
 */
@SuppressWarnings("null")
public class RisingTrendEvent extends FuturePriceEvent {
    
    private static final Random RANDOM = new Random();
    
    public RisingTrendEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }
    
    @Override
    public EventType getType() {
        return EventType.RISING_TREND;
    }
    
    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset) {
        // 基础涨幅 5% - 15%，受强度影响
        double baseRise = 0.05 + (0.10 * intensity);
        // 随天数递增，最后一天涨幅最终
    double dayMultiplier = 1.0 + (dayOffset * 0.1);
        double totalChange = baseRise * dayMultiplier;
        
        // 添加随机波动 (-20% to +20%)
        double randomFactor = 0.8 + (RANDOM.nextDouble() * 0.4);
        totalChange *= randomFactor;
        
        int newPrice = (int) (currentPrice * (1.0 + totalChange));
        
        // 使用配置的价格范围限制
    double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int maxPrice = (int) (basePrice * maxMultiplier);
        
        return Math.min(newPrice, maxPrice);
    }
}


