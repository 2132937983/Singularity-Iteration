package com.singularity_iteration.mio_icif.api.machine;

import net.neoforged.neoforge.items.IItemHandler;

/**
 * 储能方块接口
 * 所有能存储/充放电的方块都应实现此接口
 *
 * <p>此接口扩展 {@link IEnergyBlock}，储能箱本质上也是一种能源方块，
 * 因此同时提供能量存储访问（继承自 {@link IEnergyBlock}）和电池槽位管理。
 */
public interface IEnergyContainerBlock extends IEnergyBlock {
    
    /**
     * 获取物品处理器
     * @return 物品处理器
     */
    IItemHandler getItemHandler();
    
    /**
     * 获取电池槽位数量
     * @return 电池槽位数量
     */
    int getBatterySlotCount();
    
    /**
     * 获取充电速率
     * @return 充电速率（EU/t）
     */
    long getChargeRate();
    
    /**
     * 获取放电速率
     * @return 放电速率（EU/t）
     */
    long getDischargeRate();
    
    /**
     * 获取总充电量
     * @return 总充电量（EU）
     */
    long getTotalCharged();
    
    /**
     * 获取总放电量
     * @return 总放电量（EU）
     */
    long getTotalDischarged();
    
    /**
     * 获取运行时间
     * @return 运行时间（tick）
     */
    long getRunningTime();
    
    /**
     * 获取运行状态
     * @return 运行状态描述
     */
    String getRunningState();
    
    /**
     * 获取红石模式
     * @return 红石模式 (0-7)
     */
    byte getRedstoneMode();
    
    /**
     * 设置红石模式
     * @param mode 红石模式 (0-7)
     */
    void setRedstoneMode(byte mode);
    
    /**
     * 是否应该发出红石信号
     * @return true 如果应该发出信号
     */
    boolean shouldEmitRedstone();
    
    /**
     * 获取红石信号强度
     * @return 信号强度 (0-15)
     */
    int getRedstoneSignalStrength();
    
    /**
     * 是否有红石输入
     * @return true 如果有红石输入
     */
    boolean hasRedstoneInput();
    
    /**
     * 是否启用能量输出
     * @return true 如果启用能量输出
     */
    boolean shouldEmitEnergy();
}