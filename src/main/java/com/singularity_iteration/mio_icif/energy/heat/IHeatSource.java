package com.singularity_iteration.mio_icif.energy.heat;

/**
 * 热源接口
 * 用于可以产生热能的方块或物品
 * 
 * 热源特点�? * - 核燃料棒：持续产生大量热�? * - 燃烧室：燃烧燃料产生热能
 * - 地热：从环境获取热能
 * - 太阳能集热器：利用阳光产生热�? */
public interface IHeatSource {
    
    /**
     * 获取当前热输出（HU/tick�?     */
    int getHeatOutput();
    
    /**
     * 获取最大热输出
     */
    int getMaxHeatOutput();
    
    /**
     * 是否正在产生热能
     */
    boolean isProducingHeat();
    
    /**
     * 获取热源温度
     */
    int getSourceTemperature();
    
    /**
     * 获取剩余工作时间（tick�?     * 0 表示无限或已耗尽
     */
    int getRemainingTime();
    
    /**
     * 获取效率百分比（0-100�?     */
    default int getEfficiency() {
        return 100;
    }
    
    /**
     * 是否过热
     */
    default boolean isOverheating() {
        return getSourceTemperature() > 800;
    }
}


