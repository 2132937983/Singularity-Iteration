package com.singularity_iteration.mio_icif.energy.heat;

/**
 * HU (Heat Units) 热能存储接口
 * 工业2风格的热能系统，与 FE 能量完全独立
 * 
 * 热能特点：
 * - 只能由特定热源产生（如核反应堆、地热、燃烧等）
 * - 只能被特定机器消耗（如热交换器、蒸汽发生器等）
 * - 不能像电力那样远距离传输，需要特殊的热传导机制
 * - 可能会产生热量损耗
 */
public interface IHeatStorage {
    
    /**
     * 向存储中添加热能。返回实际添加的热量。
     * 
     * @param toReceive 要接收的热量
     * @param simulate  如果为 true，则只模拟操作，不实际改变存储。
     * @return 实际接收的热量
     */
    long receiveHeat(long toReceive, boolean simulate);
    
    /**
     * 从存储中提取热能。返回实际提取的热量。
     * 
     * @param toExtract 要提取的热量
     * @param simulate  如果为 true，则只模拟操作，不实际改变存储。
     * @return 实际提取的热量
     */
    long extractHeat(long toExtract, boolean simulate);
    
    /**
     * 获取当前存储的热能值。
     */
    long getHeatStored();
    
    /**
     * 获取最大热能容量。
     */
    long getMaxHeatStored();
    
    /**
     * 是否可以提取热能
     * 如果返回 false，extractHeat 将始终返回 0
     */
    boolean canExtractHeat();
    
    /**
     * 是否可以接收热能
     * 如果返回 false，receiveHeat 将始终返回 0
     */
    boolean canReceiveHeat();
    
    /**
     * 获取热能温度（摄氏度）
     * 用于显示和某些特殊机器
     */
    default int getTemperature() {
        if (getMaxHeatStored() == 0) return 20;
        long heatPercent = (getHeatStored() * 100) / getMaxHeatStored();
        return 20 + (int)((heatPercent * 980) / 100);
    }
    
    /**
     * 是否处于过热状态
     */
    default boolean isOverheated() {
        return getTemperature() >= 800;
    }
    
    /**
     * 获取热损失率（每 tick 损失的热量）
     * 热能系统通常会有热量散失
     */
    default long getHeatLossPerTick() {
        int temp = getTemperature();
        if (temp <= 100) return 0;
        return (temp - 100) / 100;
    }
}