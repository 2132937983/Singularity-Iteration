package com.singularity_iteration.mio_icif.energy.kinetic;

/**
 * KU (Kinetic Units) 动能存储接口
 * 工业2风格的动能系统，与 FE 能量和 HU 热能完全独立
 * 
 * 动能特点：
 * - 只能由特定动力源产生（如蒸汽轮机、水轮机、风力发电机等）
 * - 只能被特定机器消耗（如粉碎机、压缩机、离心机等）
 * - 不能像电力那样远距离传输，需要机械传动（如传动轴、齿轮箱等）
 * - 传动过程中可能会有动能损耗
 * - 需要旋转部件来传动
 */
public interface IKineticStorage {
    
    /**
     * 向存储中添加动能。返回实际添加的动能。
     * 
     * @param toReceive 要接收的动能
     * @param simulate  如果为 true，则只模拟操作，不实际改变存储。
     * @return 实际接收的动能
     */
    long receiveKinetic(long toReceive, boolean simulate);
    
    /**
     * 从存储中提取动能。返回实际提取的动能。
     * 
     * @param toExtract 要提取的动能
     * @param simulate  如果为 true，则只模拟操作，不实际改变存储。
     * @return 实际提取的动能
     */
    long extractKinetic(long toExtract, boolean simulate);
    
    /**
     * 获取当前存储的动能值。
     */
    long getKineticStored();
    
    /**
     * 获取最大动能容量。
     */
    long getMaxKineticStored();
    
    /**
     * 是否可以提取动能
     * 如果返回 false，extractKinetic 将始终返回 0
     */
    boolean canExtractKinetic();
    
    /**
     * 是否可以接收动能
     * 如果返回 false，receiveKinetic 将始终返回 0
     */
    boolean canReceiveKinetic();
    
    /**
     * 获取当前转速（RPM）
     * 用于显示和某些特殊机器
     */
    default int getRPM() {
        if (getMaxKineticStored() == 0) return 0;
        long kineticPercent = (getKineticStored() * 100) / getMaxKineticStored();
        return (int)((kineticPercent * 10000) / 100);
    }
    
    /**
     * 是否处于超速状态
     */
    default boolean isOverspeed() {
        return getRPM() >= 8000;
    }
    
    /**
     * 获取动能损失率（每 tick 损失的动能）
     * 动能系统通常会有摩擦损失
     */
    default long getKineticLossPerTick() {
        int rpm = getRPM();
        if (rpm < 1000) return 0;
        if (rpm < 3000) return 1;
        if (rpm < 6000) return 2;
        return 3;
    }
    
    /**
     * 获取最大接收速率
     */
    long getMaxReceive();
    
    /**
     * 获取最大提取速率
     */
    long getMaxExtract();
}