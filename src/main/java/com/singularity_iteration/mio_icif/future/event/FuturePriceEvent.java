package com.singularity_iteration.mio_icif.future.event;

import net.minecraft.nbt.CompoundTag;

/**
 * 期货价格事件基类
 * 
 * 职责�? * 1. 定义价格事件的基本属性和行为
 * 2. 提供事件的序列化和反序列化接受? * 3. 定义事件对价格的影响计算
 * 
 * 事件类型�? * - 持续上涨事件：未来N天价格持续上�? * - 持续下跌事件：未来N天价格持续下面? * - 波动增强事件：价格波动幅度增�? * - 波动减弱事件：价格波动幅度减�? * - 价格稳定事件：价格维持在基础价格附近
 */
@SuppressWarnings("null")
public abstract class FuturePriceEvent {
    
    /** 事件类型枚举 */
    @SuppressWarnings("null")
public enum EventType {
        RISING_TREND,         // 持续上涨
        FALLING_TREND,        // 持续下跌
        HIGH_VOLATILITY,      // 高波�
    LOW_VOLATILITY,       // 低波�
    PRICE_STABILIZATION,  // 价格稳定
        SHORT_BUBBLE,         // 短期暴涨暴跌（泡沫）
        LONG_BUBBLE,          // 长期暴涨暴跌（大泡沫�
    SHORT_CRASH_REBOUND,  // 短期暴跌暴涨（闪崩反弹）
        LONG_CRASH_REBOUND    // 长期暴跌暴涨（崩盘反弹）
    }
    
    // ========== 事件属性?==========
    
    protected final String commodityId;  // 目标货品ID
    protected final long startDay;       // 事件开始天�
protected final int duration;        // 事件持续天数
    protected final double intensity;    // 事件强度 (0.0 - 2.0)
    
    // ========== 构造函数?==========
    
    public FuturePriceEvent(String commodityId, long startDay, int duration, double intensity) {
        this.commodityId = commodityId;
        this.startDay = startDay;
        this.duration = duration;
        this.intensity = Math.max(0.0, Math.min(2.0, intensity));
    }
    
    // ========== 抽象方法 ==========
    
    /**
     * 获取事件类型
     */
    public abstract EventType getType();
    
    /**
     * 计算事件对价格的影响
     * @param currentPrice 当前价格
     * @param basePrice 基础价格
     * @param dayOffset 事件开始后的第几天 (0 = 第一�?
     * @return 新价�
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
    
    // ========== Getter 方法 ==========
    
    public String getCommodityId() { return commodityId; }
    public long getStartDay() { return startDay; }
    public int getDuration() { return duration; }
    public double getIntensity() { return intensity; }
}

