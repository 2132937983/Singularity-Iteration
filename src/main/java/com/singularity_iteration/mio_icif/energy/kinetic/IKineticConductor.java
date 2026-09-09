package com.singularity_iteration.mio_icif.energy.kinetic;

import net.minecraft.core.Direction;

/**
 * KU (Kinetic Units) 动能传导器接? * 用于可以传导动能的方块（如传动轴、齿轮箱等）
 * 
 * 传导器特点：
 * - 可以接收来自动力源的动能
 * - 可以将动能传导到相邻的方? * - 传导过程中可能会有动能损失（摩擦? * - 可能有方向性限? */
public interface IKineticConductor {
    
    /**
     * 从指定方向接收动?     * 
     * @param fromSide 接收方向
     * @param amount 接收的动能数?     * @param simulate 如果?true，只模拟操作
     * @return 实际接收的动能数?     */
    int receiveKineticFrom(Direction fromSide, int amount, boolean simulate);
    
    /**
     * 向指定方向输出动?     * 
     * @param toSide 输出方向
     * @param amount 输出的动能数?     * @param simulate 如果?true，只模拟操作
     * @return 实际输出的动能数?     */
    int outputKineticTo(Direction toSide, int amount, boolean simulate);
    
    /**
     * 是否可以从指定方向接收动?     * 
     * @param fromSide 接收方向
     * @return 如果可以接收返回 true
     */
    boolean canReceiveFrom(Direction fromSide);
    
    /**
     * 是否可以向指定方向输出动?     * 
     * @param toSide 输出方向
     * @return 如果可以输出返回 true
     */
    boolean canOutputTo(Direction toSide);
    
    /**
     * 获取传导效率?.0 - 1.0?     * 传导过程中会有能量损?     */
    float getConductionEfficiency();
    
    /**
 * 获取当前存储的动能（冲? * 传导器通常有小量? */
    int getBufferedKinetic();
    
    /**
 * 获取最大冲容纳? */
    int getMaxBufferCapacity();
    
    /**
     * 获取当前转速（RPM?     */
    int getCurrentRPM();
}


