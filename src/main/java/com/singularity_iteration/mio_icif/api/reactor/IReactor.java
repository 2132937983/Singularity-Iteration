package com.singularity_iteration.mio_icif.api.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 核反应堆核心接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IReactor}。
 * 实现此接口的方块实体代表一个核反应堆核心，
 * 管理反应堆的热量、燃料和能量输出。
 * <p>
 * 热量值使用 {@code long} 类型，与 {@link IReactorAPI#getReactorCurrentHeat} 保持一致，
 * 避免极端情况下（如大量中子脉冲叠加）溢出 {@code int} 范围。
 */
public interface IReactor {

    /**
     * 获取反应堆所在世界
     */
    Level getLevel();

    /**
     * 获取反应堆方块位置
     */
    BlockPos getBlockPos();

    /**
     * 获取反应堆当前热量
     *
     * @return 当前热量值（HU）
     */
    long getHeat();

    /**
     * 设置反应堆热量
     *
     * @param heat 新的热量值
     */
    void setHeat(long heat);

    /**
     * 向反应堆添加热量
     *
     * @param heat 要添加的热量值
     * @return 实际添加的热量值
     */
    long addHeat(long heat);

    /**
     * 获取反应堆最大热量容量
     *
     * @return 最大热量容量
     */
    long getMaxHeat();

    /**
     * 设置反应堆最大热量容量
     *
     * @param maxHeat 新的最大热量容量
     */
    void setMaxHeat(long maxHeat);

    /**
     * 添加散热热量
     *
     * @param heat 散热量
     */
    void addEmitHeat(long heat);

    /**
     * 获取热量效果乘数
     *
     * @return 热量效果乘数
     */
    float getHeatEffectModifier();

    /**
     * 设置热量效果乘数
     *
     * @param modifier 新的乘数
     */
    void setHeatEffectModifier(float modifier);

    /**
     * 获取反应堆能量输出
     *
     * @return 能量输出（EU/tick）
     */
    long getReactorEnergyOutput();

    /**
     * 获取指定位置的物品
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 该位置的物品
     */
    ItemStack getItemAt(int x, int y);

    /**
     * 设置指定位置的物品
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param stack 要放置的物品
     */
    void setItemAt(int x, int y, ItemStack stack);

    /**
     * 引发反应堆爆炸
     */
    void explode();

    /**
     * 获取反应堆 tick 速率
     *
     * @return tick 速率
     */
    int getTickRate();

    /**
     * 尝试在当前 tick 产生能量
     *
     * @return 如果成功产生能量则返回 true
     */
    boolean produceEnergy();

    /**
     * 检查反应堆是否是流体冷却的
     *
     * @return 如果是流体冷却则返回 true
     */
    boolean isFluidCooled();
}