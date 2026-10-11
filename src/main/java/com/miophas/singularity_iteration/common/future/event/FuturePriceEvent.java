package com.miophas.singularity_iteration.common.future.event;

import net.minecraft.nbt.CompoundTag;

/**
 * 期货价格事件基类
 * 
 * Responsibilities:
 * 1. Define the basic properties and behavior of a price event.
 * 2. Provide serialization and deserialization of events.
 * 3. Define how an event changes the price.
 *
 * Event types:
 * - Rising trend: the price rises for the next N days.
 * - Falling trend: the price falls for the next N days.
 * - High volatility: the price fluctuation range increases.
 * - Low volatility: the price fluctuation range decreases.
 * - Price stabilization: the price stays near the base price.
 */
@SuppressWarnings("null")
public abstract class FuturePriceEvent {
    
    /** 事件类型枚举 */
    @SuppressWarnings("null")
public enum EventType {
        RISING_TREND,         // 持续上涨
        FALLING_TREND,        // 持续下跌
        HIGH_VOLATILITY,      // High volatility
    LOW_VOLATILITY,       // Low volatility
    PRICE_STABILIZATION,  // 价格稳定
        SHORT_BUBBLE,         // 短期暴涨暴跌（泡沫）
        LONG_BUBBLE,          // Long-term surge then crash (large bubble)
    SHORT_CRASH_REBOUND,  // 短期暴跌暴涨（闪崩反弹）
        LONG_CRASH_REBOUND    // 长期暴跌暴涨（崩盘反弹）
    }
    
    // ========== Event properties ==========
    
    protected final String commodityId;  // 目标货品ID
    protected final long startDay;       // Start day of the event
protected final int duration;        // 事件持续天数
    protected final double intensity;    // 事件强度 (0.0 - 2.0)
    
    // ========== Constructor ==========
    
    public FuturePriceEvent(String commodityId, long startDay, int duration, double intensity) {
        this.commodityId = commodityId;
        this.startDay = startDay;
        this.duration = duration;
        this.intensity = Math.max(0.0, Math.min(2.0, intensity));
    }
    
    // ========== Abstract methods ==========
    
    /**
     * 获取事件类型
     */
    public abstract EventType getType();
    
    /**
     * 计算事件对价格的影响
     * @param currentPrice 当前价格
     * @param basePrice 基础价格
     * @param dayOffset the day number after the event start (0 = first day)
     * @return the new price
     */
    public abstract int applyEffect(int currentPrice, int basePrice, int dayOffset);
    
    /**
     * 检查事件在指定天数是否有效
     * @param gameDay 当前游戏天数
     * @return 是否有效
     */
    public boolean isActive(long gameDay) {
        long dayOffset = gameDay - startDay;
        return dayOffset >= 0 && dayOffset < duration;
    }
    
    /**
     * 获取事件剩余天数
     * @param gameDay 当前游戏天数
     * @return 剩余天数，如果事件已结束返回0
     */
    public int getRemainingDays(long gameDay) {
        long dayOffset = gameDay - startDay;
        if (dayOffset < 0) return duration;
        if (dayOffset >= duration) return 0;
        return (int) (duration - dayOffset);
    }
    
    /**
     * 序列化为NBT
     */
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", getType().name());
        tag.putString("CommodityId", commodityId);
        tag.putLong("StartDay", startDay);
        tag.putInt("Duration", duration);
        tag.putDouble("Intensity", intensity);
        serializeAdditional(tag);
        return tag;
    }
    
    /**
     * 子类可重写以序列化额外数据
 */
    protected void serializeAdditional(CompoundTag tag) {}
    
    /**
     * 从NBT反序列化
     */
    public static FuturePriceEvent deserialize(CompoundTag tag) {
        EventType type = EventType.valueOf(tag.getString("Type"));
        String commodityId = tag.getString("CommodityId");
        long startDay = tag.getLong("StartDay");
        int duration = tag.getInt("Duration");
        double intensity = tag.getDouble("Intensity");
        
        return switch (type) {
            case RISING_TREND -> new RisingTrendEvent(commodityId, startDay, duration, intensity);
            case FALLING_TREND -> new FallingTrendEvent(commodityId, startDay, duration, intensity);
            case HIGH_VOLATILITY -> new HighVolatilityEvent(commodityId, startDay, duration, intensity);
            case LOW_VOLATILITY -> new LowVolatilityEvent(commodityId, startDay, duration, intensity);
            case PRICE_STABILIZATION -> new PriceStabilizationEvent(commodityId, startDay, duration, intensity);
            case SHORT_BUBBLE -> new ShortBubbleEvent(commodityId, startDay, duration, intensity);
            case LONG_BUBBLE -> new LongBubbleEvent(commodityId, startDay, duration, intensity);
            case SHORT_CRASH_REBOUND -> new ShortCrashReboundEvent(commodityId, startDay, duration, intensity);
            case LONG_CRASH_REBOUND -> new LongCrashReboundEvent(commodityId, startDay, duration, intensity);
        };
    }
    
    // ========== Getter methods ==========
    
    public String getCommodityId() { return commodityId; }
    public long getStartDay() { return startDay; }
    public int getDuration() { return duration; }
    public double getIntensity() { return intensity; }
}

