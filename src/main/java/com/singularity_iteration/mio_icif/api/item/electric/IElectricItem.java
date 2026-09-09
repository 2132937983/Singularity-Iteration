package com.singularity_iteration.mio_icif.api.item.electric;

import net.minecraft.world.item.ItemStack;

/**
 * 电力物品接口（IC2 兼容 API）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IElectricItem}。
 * 实现此接口的物品可以存储和传输电能，如电池、电动工具、电力装甲等。
 * <p>
 * 注意：此接口用于物品声明自己的电力属性。实际充放电操作应通过
 * {@link IElectricItemManager} 进行，而不是直接调用此接口的方法。
 *
 * <h3>与 {@link com.singularity_iteration.mio_icif.api.item.IBatteryItem} 的关系</h3>
 * <p>本接口与 {@code IBatteryItem} 是两套并行的电池物品 API：
 * <ul>
 *   <li><b>{@code IElectricItem}</b>（本接口）— IC2 兼容 API，方法均需传入
 *       {@code ItemStack}，配合 {@code IElectricItemManager} 使用，
 *       适合需要 IC2 原版互操作的场景。</li>
 *   <li><b>{@code IBatteryItem}</b> — mio_icif 简化 API，方法签名简洁，
 *       适合 mio_icif 机器内部直接调用。</li>
 * </ul>
 * <p>两者可同时实现，互不冲突。mio_icif 自带物品默认实现 {@code IBatteryItem}。
 *
 * @deprecated 此 IC2 兼容 API 已不再需要，请使用 {@link com.singularity_iteration.mio_icif.api.item.IBatteryItem} 代替。
 *             计划在下一个主要版本中移除。
 */
@Deprecated(since = "1.21.1", forRemoval = true)
public interface IElectricItem {

    /**
     * 检查此物品是否可以提供能量给其他物品或方块。
     * <p>
     * 返回 true 的物品可以被电网识别为能量源，
     * 例如电池可以放电给机器，而电动工具通常不能。
     *
     * @param stack 物品堆
     * @return 如果可以提供能量则返回 true
     */
    boolean canProvideEnergy(ItemStack stack);

    /**
     * 获取此物品的最大电荷容量（EU）。
     *
     * @param stack 物品堆
     * @return 最大电荷容量
     */
    long getMaxCharge(ItemStack stack);

    /**
     * 获取此物品的电压等级。
     * <p>
     * 电压等级定义：
     * <ul>
     *   <li>1 = LV (32 EU/packet)</li>
     *   <li>2 = MV (128 EU/packet)</li>
     *   <li>3 = HV (512 EU/packet)</li>
     *   <li>4 = EV (2048 EU/packet)</li>
     *   <li>5 = IV (8192 EU/packet)</li>
     * </ul>
     *
     * @param stack 物品堆
     * @return 电压等级
     */
    int getTier(ItemStack stack);

    /**
     * 获取此物品的最大传输限制（EU/tick）。
     * <p>
     * 定义了每次充放电操作的最大能量传输量。
     *
     * @param stack 物品堆
     * @return 传输限制
     */
    long getTransferLimit(ItemStack stack);
}
