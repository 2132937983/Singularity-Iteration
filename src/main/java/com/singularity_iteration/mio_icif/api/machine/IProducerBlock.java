package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * 生产机器接口
 * 所有能处理配方的机器都应实现此接口
 */
public interface IProducerBlock {
    
    /**
     * 强制开始工作
     */
    void forceStartWork();
    
    /**
     * 强制停止工作
     */
    void forceStopWork();
    
    /**
     * 获取升级统计信息
     * @return 升级统计
     */
    IMachineUpgradeStats getUpgradeStats();
    
    /**
     * 获取升级槽起始位置
     * @return 起始槽位索引
     */
    int getUpgradeSlotStart();
    
    /**
     * 获取升级槽数量
     * @return 升级槽数量
     */
    int getUpgradeSlotCount();
    
    /**
     * 获取物品处理器
     * @return 物品处理器
     */
    IItemHandler getItemHandler();
    
    /**
     * 获取升级物品列表
     * @return 升级物品列表
     */
    List<ItemStack> getUpgrades();
    
    /**
     * 获取工作进度
     * @return 当前进度
     */
    int getProgress();
    
    /**
     * 获取最大工作进度
     * @return 最大进度
     */
    int getMaxProgress();
    
    /**
     * 获取基础最大进度
     * @return 基础最大进度
     */
    int getBaseMaxProgress();
    
    /**
     * 获取每 tick 能耗
     * @return 每 tick 能耗（EU）
     */
    long getEnergyPerTick();
    
    /**
     * 获取槽位布局
     * @return 槽位布局
     */
    ISlotLayout getSlotLayout();
    
    /**
     * 获取容器大小
     * @return 容器大小
     */
    int getContainerSize();
    
    /**
     * 获取能量存储
     * @return 能量存储
     */
    IEnergyStorageAccess getEnergyStorage();
    
    /**
     * 获取工作完成回调
     * @return 工作完成回调，可能为 null
     */
    IWorkCompleteCallback getWorkCompleteCallback();
    
    /**
     * 是否正在工作
     * @return true 如果正在工作
     */
    boolean isWorking();
    
    /**
     * 获取有效的每 tick 能量消耗（考虑升级）
     * @return 有效能耗（EU）
     */
    long getEffectiveEnergyPerTick();
    
    /**
     * 获取总处理量
     * @return 总处理量
     */
    long getTotalProcessed();
    
    /**
     * 获取当前红石模式
     * @return 红石模式，默认返回 NONE
     */
    default IMachineAPI.RedstoneMode getRedstoneMode() {
        return IMachineAPI.RedstoneMode.NONE;
    }
    
    /**
     * 设置红石模式
     * @param mode 红石模式
     */
    default void setRedstoneMode(IMachineAPI.RedstoneMode mode) {
        // 默认实现为空，子类可选择性覆盖
    }

    default void setLit(boolean lit) {
        // 默认实现为空，子类可选择性覆盖
    }

    default boolean isLit() {
        return isWorking();
    }

    default void serverTick() {
        // 默认实现为空，子类可选择性覆盖
    }

    default IMachineAPI.MetalFormerMode getMetalFormerMode() {
        return IMachineAPI.MetalFormerMode.ROLLING;
    }

    default void setMetalFormerMode(IMachineAPI.MetalFormerMode mode) {
        // 默认实现为空，子类可选择性覆盖
    }
}