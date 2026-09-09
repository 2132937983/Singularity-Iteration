package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * 机器信息接口
 *
 * <p>提供机器运行时状态的只读视图，用于查询机器的能量、进度、配方等信息。
 *
 * <p>使用示例：
 * <pre>{@code
 * IMachineInfo info = machineAPI.getMachineInfo(world, pos);
 * if (info != null) {
 *     long energy = info.getStoredEnergy();
 *     int progress = info.getProgress();
 *     boolean working = info.isWorking();
 * }
 * }</pre>
 */
public interface IMachineInfo {

    /**
     * 获取机器所在的世界
     */
    Level getWorld();

    /**
     * 获取机器方块位置
     */
    BlockPos getPos();

    /**
     * 获取机器类型
     */
    IMachineAPI.MachineType getMachineType();

    /**
     * 获取机器当前存储的能量
     */
    long getStoredEnergy();

    /**
     * 获取机器最大能量容量
     */
    long getMaxEnergy();

    /**
     * 获取机器当前工作进度
     */
    int getProgress();

    /**
     * 获取机器最大工作进度
     */
    int getMaxProgress();

    /**
     * 获取工作进度百分比（0.0 - 1.0）
     */
    default double getProgressPercent() {
        if (getMaxProgress() <= 0) return 0.0;
        return (double) getProgress() / getMaxProgress();
    }

    /**
     * 检查机器是否正在工作
     */
    boolean isWorking();

    /**
     * 获取机器当前每 tick 的能量消耗
     */
    long getEnergyPerTick();

    /**
     * 获取机器基础每 tick 能量消耗（不受升级影响）
     */
    long getBaseEnergyPerTick();

    /**
     * 获取机器的电缆等级
     */
    ICableTier getCableTier();

    /**
     * 获取机器当前输入物品
     */
    ItemStack getInputItem();

    /**
     * 获取机器当前输出物品
     */
    ItemStack getOutputItem();

    /**
     * 获取机器所有输入槽的物品
     */
    Collection<ItemStack> getInputItems();

    /**
     * 获取机器所有输出槽的物品
     */
    Collection<ItemStack> getOutputItems();

    /**
     * 获取本次运行的持续时间（tick）
     */
    long getRuntimeTicks();

    /**
     * 获取机器总加工数量
     */
    long getTotalProcessed();

    /**
     * 获取机器速度倍率
     */
    double getSpeedMultiplier();

    /**
     * 获取机器能耗倍率
     */
    double getEnergyMultiplier();

    /**
     * 检查机器是否支持升级插件
     */
    boolean supportsUpgrades();

    /**
     * 获取已安装的升级插件
     */
    Collection<ItemStack> getInstalledUpgrades();

    /**
     * 获取机器效率百分比
     *
     * <p>效率定义为每单位能量输入的工作输出：
     * {@code (speedMultiplier / energyMultiplier) * 100}。
     * 100% 表示无升级影响。低于 100% 表示机器相对于速度增益消耗更多能量
     * （例如超频插件）。高于 100% 表示能量效率提升（例如效率升级插件）。
     *
     * <p>如果能耗倍率为零或负数（表示 bug 或无效状态），
     * 此方法返回 0.0 表示机器无法正常工作。
     */
    default double getEfficiency() {
        double speed = getSpeedMultiplier();
        double energy = getEnergyMultiplier();
        // 如果能耗倍率为0或负数，表示机器处于无效状态，效率为0
        if (energy <= 0) return 0.0;
        return (speed / energy) * 100.0;
    }
}