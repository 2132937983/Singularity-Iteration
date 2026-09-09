package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 电池功能接口（mio_icif 简化 API）。
 * 所有可存储电力的电池物品都应实现此接口。
 *
 * <h3>与 {@link com.singularity_iteration.mio_icif.api.item.electric.IElectricItem} 的关系</h3>
 * <p>本接口与 {@code IElectricItem} 是两套并行的电池物品 API，服务于不同场景：
 * <ul>
 *   <li><b>{@code IBatteryItem}</b>（本接口）— mio_icif 内部简化 API。
 *       方法签名简洁（如 {@link #getMaxEnergy()} 无需传入 ItemStack），
 *       适合机器内部直接调用。所有 mio_icif 自带的电池、电动工具、电力装甲
 *       均通过其基类（{@code mio_icif_bat}、{@code mio_icif_tool_elc}、
 *       {@code mio_icif_armor_elc}）实现此接口。</li>
 *   <li><b>{@code IElectricItem}</b> — IC2 兼容 API。
 *       遵循 IC2 1.12.2 的 {@code IElectricItem} 规范，所有方法均需传入
 *       {@code ItemStack} 参数，配合 {@code IElectricItemManager} 使用。
 *       适用于需要与 IC2 原版工具/电池互操作的附属模组。</li>
 * </ul>
 *
 * <p><b>选择建议：</b>
 * <ul>
 *   <li>若物品仅需在 mio_icif 机器中使用 → 实现 {@code IBatteryItem}</li>
 *   <li>若物品需要 IC2 原版兼容性（如 IC2 充电bench、电动工具管理器）→ 实现 {@code IElectricItem}</li>
 *   <li>两者可同时实现，互不冲突</li>
 * </ul>
 */
public interface IBatteryItem {

    /**
     * 获取最大能量容量 (EU)
     */
    long getMaxEnergy();

    /**
     * 获取指定物品堆的最大能量容量 (EU)
     * <p>默认委托到无参的 {@link #getMaxEnergy()}，适用于容量固定的电池。
     * 支持可变容量（如受附魔/ NBT / 升级影响的）物品应覆盖此方法，
     * 根据 {@code stack} 返回实际容量。
     *
     * @param stack 物品堆
     * @return 最大能量容量 (EU)
     */
    default long getMaxEnergy(ItemStack stack) {
        return getMaxEnergy();
    }

    /**
     * 获取当前存储的能量 (EU)
     */
    long getEnergy(ItemStack stack);

    /**
     * 添加能量 (EU)
     * @return 实际添加的能量
     */
    long addEnergy(ItemStack stack, long amount);

    /**
     * 提取能量 (EU)
     * @return 实际提取的能量
     */
    long extractEnergy(ItemStack stack, long amount);

    /**
     * 检查是否已充满
     */
    boolean isFull(ItemStack stack);

    /**
     * 检查是否为空
     */
    boolean isEmpty(ItemStack stack);

    /**
     * 获取充电速率 (EU/tick)
     */
    default long getChargeRate(ItemStack stack) {
        return 0;
    }

    /**
     * 设置能量值（不检查上限）。
     * <p>主要用于创造模式标签页等需要直接设置能量的场景。
     * 默认实现为空操作，支持能量设置的物品应覆盖此方法。
     *
     * @param stack  物品堆
     * @param energy 要设置的能量值 (EU)
     */
    default void setEnergy(ItemStack stack, long energy) {
        // 默认实现：空操作
    }
}