package com.singularity_iteration.mio_icif.api.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

/**
 * 核反应堆 API
 *
 * <p>提供与 mio_icif 核反应堆系统交互的接口，包括：
 * <ul>
 *   <li>查询反应堆元件属性</li>
 *   <li>检查物品是否是反应堆元件</li>
 *   <li>获取反应堆元件类型</li>
 *   <li>监控反应堆运行状态</li>
 *   <li>获取反应堆结构和热量信息</li>
 * </ul>
 */
public interface IReactorAPI {

    /**
     * 检查物品是否是核反应堆元件
     *
     * @param stack 物品栈
     * @return true 如果是反应堆元件
     */
    boolean isReactorComponent(ItemStack stack);

    /**
     * 获取反应堆元件类型
     *
     * @param stack 物品栈
     * @return 元件类型，如果不是反应堆元件则返回 null
     */
    ReactorComponentType getComponentType(ItemStack stack);

    /**
     * 获取反应堆元件的中子脉冲输出
     *
     * @param stack 物品栈
     * @return 中子脉冲数，如果不是燃料棒则返回 0
     */
    int getNeutronPulseOutput(ItemStack stack);

    /**
     * 获取反应堆元件的热量输出
     *
     * @param stack 物品栈
     * @return 热量输出值（HU）
     */
    int getHeatOutput(ItemStack stack);

    /**
     * 获取反应堆元件的最大热量存储
     *
     * @param stack 物品栈
     * @return 最大热量存储值（HU）
     */
    int getMaxHeatStorage(ItemStack stack);

    /**
     * 获取反应堆元件的热量传递效率
     *
     * @param stack 物品栈
     * @return 热量传递效率 (0-100)
     */
    int getHeatTransferEfficiency(ItemStack stack);

    /**
     * 检查物品是否是 MOX 燃料
     *
     * @param stack 物品栈
     * @return true 如果是 MOX 燃料
     */
    boolean isMoxFuel(ItemStack stack);

    /**
     * 检查物品是否是燃料棒
     *
     * @param stack 物品栈
     * @return true 如果是燃料棒
     */
    boolean isFuelRod(ItemStack stack);

    /**
     * 获取所有注册的反应堆元件 ID
     */
    Collection<net.minecraft.resources.ResourceLocation> getAllReactorComponentIds();

    // ========== 反应堆监控 API ==========

    /**
     * 检查反应堆是否正在运行
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return true 如果反应堆正在运行
     */
    boolean isReactorRunning(Level world, BlockPos pos);

    /**
     * 获取反应堆当前热量
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 当前热量值
     */
    long getReactorCurrentHeat(Level world, BlockPos pos);

    /**
     * 获取反应堆最大热量容量
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 最大热量容量
     */
    long getReactorMaxHeat(Level world, BlockPos pos);

    /**
     * 获取反应堆当前温度
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 当前温度（摄氏度）
     */
    double getReactorCurrentTemperature(Level world, BlockPos pos);

    /**
     * 获取反应堆当前发电量
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 发电量 (EU/tick)
     */
    long getReactorCurrentEnergyGeneration(Level world, BlockPos pos);

    /**
     * 获取反应堆可用柱数
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 可用柱数
     */
    int getReactorAvailableColumns(Level world, BlockPos pos);

    /**
     * 获取反应堆模式
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 反应堆模式（GENERATOR 或 FLUID）
     */
    ReactorMode getReactorMode(Level world, BlockPos pos);

    /**
     * 获取反应堆结构详细状态
     *
     * @param world 世界
     * @param pos 反应堆主控位置
     * @return 结构详细状态
     */
    ReactorStructureStatus getReactorStructureStatusDetailed(Level world, BlockPos pos);

    /**
     * 反应堆模式枚举
     */
    enum ReactorMode {
        /** 发电模式：产生EU */
        GENERATOR,
        /** 流体模式：产生热能/蒸汽 */
        FLUID
    }

    /**
     * 反应堆结构状态记录
     *
     * @param valid 结构是否完整
     * @param errors 错误信息列表
     * @param chambersConnected 已连接的反应堆舱数量
     */
    record ReactorStructureStatus(
        boolean valid,
        List<String> errors,
        int chambersConnected
    ) {
        /**
         * 创建一个有效的结构状态
         */
        public static ReactorStructureStatus valid(int chambersConnected) {
            return new ReactorStructureStatus(true, List.of(), chambersConnected);
        }

        /**
         * 创建一个无效的结构状态
         */
        public static ReactorStructureStatus invalid(List<String> errors) {
            return new ReactorStructureStatus(false, errors, 0);
        }
    }

    // ========== 燃料棒 API ==========

    /**
     * 获取燃料棒的中子脉冲数
     *
     * @param stack 燃料棒物品
     * @return 中子脉冲数
     */
    int getFuelRodPulses(ItemStack stack);

    /**
     * 获取燃料棒的联数（单/双/四）
     *
     * @param stack 燃料棒物品
     * @return 1=单联, 2=双联, 4=四联
     */
    int getFuelRodCells(ItemStack stack);

    /**
     * 检查燃料棒是否已耗尽
     *
     * @param stack 燃料棒物品
     * @return true 如果已耗尽
     */
    boolean isFuelRodDepleted(ItemStack stack);

    /**
     * 获取燃料棒的热量输出
     *
     * @param stack 燃料棒物品
     * @return 热量输出
     */
    long getFuelRodHeatOutput(ItemStack stack);

    /**
     * 获取燃料棒耗尽后的物品
     *
     * @param stack 燃料棒物品
     * @return 耗尽后的物品
     */
    ItemStack getFuelRodDepletedItem(ItemStack stack);

    /**
     * 获取燃料棒类型
     *
     * @param stack 燃料棒物品
     * @return 燃料棒类型
     */
    FuelRodType getFuelRodType(ItemStack stack);

    /**
     * 燃料棒类型枚举
     */
    enum FuelRodType {
        SINGLE(1),
        DUAL(2),
        QUAD(4);

        private final int cells;

        FuelRodType(int cells) {
            this.cells = cells;
        }

        public int getCells() {
            return cells;
        }
    }
}