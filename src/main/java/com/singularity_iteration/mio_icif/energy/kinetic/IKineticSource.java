package com.singularity_iteration.mio_icif.energy.kinetic;

import net.minecraft.core.Direction;

/**
 * KU (Kinetic Units) 动力源接受? * 用于可以产生动能的方法?物品
 * 
 * 动力源特点：
 * - 持续产生动能（如蒸汽轮机、水轮机、风力发电机等）
 * - 产生的动能可以输出到相邻的动能存储或传导�? * - 可能有启动条件（如需要蒸汽、水流、风力等�? */
public interface IKineticSource {
    
    /**
     * 获取当前产生的动能输出（KU/tick�?     * 
     * @param side 输出方向
     * @return 当前 tick 产生的动作?     */
    int getKineticOutput(Direction side);
    
    /**
     * 获取最大动能输出（KU/tick�?     * 
     * @param side 输出方向
     * @return 最大可能的动能输出
     */
    int getMaxKineticOutput(Direction side);
    
    /**
     * 是否正在产生动能
     * 
     * @param side 输出方向
     * @return 如果正在产生动能返回 true
     */
    boolean isProducingKinetic(Direction side);
    
    /**
     * 是否可以在指定方向输出动作?     * 
     * @param side 输出方向
     * @return 如果可以输出返回 true
     */
    boolean canOutputKinetic(Direction side);
    
    /**
     * 获取当前转速（RPM�?     * 动力源的转速可能影响输出效果?     */
    int getCurrentRPM();
    
    /**
     * 获取最大转速（RPM�?     */
    int getMaxRPM();
}


