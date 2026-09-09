package com.singularity_iteration.mio_icif.energy.heat;

import net.minecraft.core.Direction;

/**
 * 热传导介质接受? * 用于可以传导热能的方块（如热导管、热交换器等�? * 
 * 热传导特点：
 * - 传导效率取决于材料? * - 距离越远，损失越�? * - 需要温度差才能传导
 */
public interface IHeatConductor {
    
    /**
     * 获取传导效率�?.0 - 1.0�?     * 1.0 表示 100% 效率，无损失
     */
    float getConductionEfficiency();
    
    /**
     * 获取最大传导速率（HU/tick�?     */
    int getMaxConductionRate();
    
    /**
     * 从指定方向传导热�?     * @param fromDirection 热源方向
     * @param amount 要传导的热量
     * @param simulate 是否模拟
     * @return 实际传导的热�?     */
    int conductHeat(Direction fromDirection, int amount, boolean simulate);
    
    /**
     * 是否可以从此方向接收热能
     */
    boolean canReceiveFrom(Direction direction);
    
    /**
     * 是否可以向此方向输出热能
     */
    boolean canOutputTo(Direction direction);
    
    /**
     * 获取当前存储的热量（用于缓冲�?     */
    int getBufferedHeat();
    
    /**
     * 获取最大缓冲热�?     */
    int getMaxBufferedHeat();
    
    /**
     * 获取热阻（影响传导速度�?     * 越低传导越快
     */
    default float getThermalResistance() {
        return 1.0f - getConductionEfficiency();
    }
    
    /**
     * 是否需要温度差才能传导
     */
    default boolean requiresTemperatureDifference() {
        return true;
    }
    
    /**
     * 获取最小工作温�?     */
    default int getMinimumWorkingTemperature() {
        return 20;
    }
}


