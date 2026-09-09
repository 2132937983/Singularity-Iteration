package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 发电机接口
 * 所有能发电的机器都应实现此接口
 *
 * <p>此接口扩展 {@link IBurnControl}，发电机在支持燃烧时间查询的同时，
 * 也应支持通过 API 控制燃烧进度（强制启停）。
 */
public interface IGeneratorBlock extends IBurnControl {
    
    /**
     * 获取电缆等级
     * @return 电缆等级
     */
    ICableTier getCableTier();
    
    /**
     * 是否正在燃烧燃料
     * @return true 如果正在燃烧
     */
    boolean isBurning();
    
    /**
     * 获取当前燃烧时间
     * @return 当前燃烧时间（tick）
     */
    int getBurnTime();
    
    /**
     * 获取最大燃烧时间
     * @return 最大燃烧时间（tick）
     */
    int getMaxBurnTime();
    
    /**
     * 获取发电量（EU/t）
     * @return 发电量
     */
    long getPowerOutput();
    
    /**
     * 获取燃料槽位物品
     * @return 燃料槽位物品
     */
    ItemStack getFuelSlotItem();
    
    /**
     * 获取充电槽位物品
     * @return 充电槽位物品
     */
    ItemStack getChargeSlotItem();
    
    /**
     * 获取物品处理器
     * @return 物品处理器
     */
    IItemHandler getItemHandler();

    /**
     * 检查发电机是否正在工作（产出能量）
     *
     * <p>默认委托到 {@link #isBurning()}，子类可覆盖以区分"燃烧中"与"发电中"。
     *
     * @return true 如果正在发电
     */
    default boolean isGenerating() {
        return isBurning();
    }

    /**
     * 获取当前发电速率（EU/t）
     *
     * <p>默认委托到 {@link #getPowerOutput()}。
     *
     * @return 每 tick 发电量
     */
    default long getGenerationRate() {
        return getPowerOutput();
    }

    /**
     * 获取发电机类型 ID
     *
     * <p>默认返回空字符串，子类可覆盖以返回自定义类型标识。
     *
     * @return 类型 ID
     */
    default String getGeneratorTypeId() {
        return "";
    }

    /**
     * 获取发电机累计总发电量（EU）
     *
     * <p>默认返回 0，子类可覆盖以跟踪累计发电量。
     *
     * @return 总发电量
     */
    default long getTotalGenerated() {
        return 0;
    }
}