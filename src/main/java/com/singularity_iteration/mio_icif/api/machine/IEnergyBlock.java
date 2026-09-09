package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 能源方块接口
 * 所有能存储/传输能量的方块都应实现此接口
 */
public interface IEnergyBlock {
    
    /**
     * 获取能量存储
     * @return 能量存储实例
     */
    IEnergyStorageAccess getEnergyStorage();
    
    /**
     * 是否是电源
     * @return true 如果是电源
     */
    boolean isPowerSource();
    
    /**
     * 获取功率输出（EU/t）
     * @return 功率输出
     */
    long getPowerOutput();
    
    /**
     * 获取有效电缆等级
     * @return 电缆等级
     */
    ICableTier getEffectiveCableTier();
    
    /**
     * 获取有效能量容量
     * @return 能量容量
     */
    long getEffectiveCapacity();
    
    /**
     * 获取有效最大接收速率
     * @return 最大接收速率
     */
    long getEffectiveMaxReceive();
    
    /**
     * 消耗能量（内部做功），不受 maxExtract 限制
     *
     * <p>适用于机器内部加工、运转等场景。
     * 与对外输出（extract/discharge）不同，此方法不受 {@code maxExtract} 限制。
     *
     * @param amount 消耗量 (EU)
     * @param simulate 如果为true，仅模拟而不实际消耗
     * @return 实际消耗的能量量
     */
    long useEnergy(long amount, boolean simulate);
    
    /**
     * 生成能量（内部发电），不受 maxReceive 限制
     *
     * <p>适用于发电机内部产电等场景。
     * 与从外部充入（receive/charge）不同，此方法不受 {@code maxReceive} 限制。
     *
     * @param amount 生成量 (EU)
     * @param simulate 如果为true，仅模拟而不实际生成
     * @return 实际生成的能量量
     */
    long generateEnergy(long amount, boolean simulate);
    
    /**
     * 是否可以连接
     * @param side 方向
     * @return true 如果可以连接
     */
    boolean canConnect(@Nullable Direction side);
    
    /**
     * 获取机器类型。
     *
     * <p>仅对真正的机器（生产者、发电机等）有意义。
     * 对于电缆、储能箱等非机器能源方块，返回 {@link IMachineAPI.MachineType#CUSTOM}。
     *
     * @return 机器类型，默认 {@link IMachineAPI.MachineType#CUSTOM}
     */
    default IMachineAPI.MachineType getMachineType() {
        return IMachineAPI.MachineType.CUSTOM;
    }
    
    /**
     * 获取世界
     * @return 世界实例
     */
    Level getLevel();
    
    /**
     * 获取位置
     * @return 方块位置
     */
    BlockPos getBlockPos();
    
    /**
     * 获取方块状态
     * @return 方块状态
     */
    BlockState getBlockState();
}