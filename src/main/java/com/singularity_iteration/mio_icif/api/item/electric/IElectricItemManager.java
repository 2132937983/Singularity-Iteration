package com.singularity_iteration.mio_icif.api.item.electric;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 电力物品管理器接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IElectricItemManager}。
 * 提供对电力物品的充放电操作。
 * <p>
 * 此接口由 {@link IElectricItem} 的实现类提供，或通过
 * {@link IBackupElectricItemManager} 为不实现 {@link IElectricItem} 的物品提供。
 *
 * @deprecated 此 IC2 兼容 API 已不再需要，请使用 {@link com.singularity_iteration.mio_icif.api.item.IBatteryItem} 代替。
 *             计划在下一个主要版本中移除。
 */
@Deprecated(since = "1.21.1", forRemoval = true)
public interface IElectricItemManager {

    /**
     * 向物品充入能量。
     *
     * @param stack 物品堆
     * @param amount 要充入的能量值（EU）
     * @param tier 充电电压等级
     * @param ignoreTransferLimit 是否忽略传输限制
     * @param simulate 如果为 true，仅模拟充电，不实际改变物品 NBT
     * @return 实际充入的能量值（EU）
     */
    long charge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate);

    /**
     * 从物品抽取能量。
     *
     * @param stack 物品堆
     * @param amount 要抽取的能量值（EU）
     * @param tier 放电电压等级
     * @param ignoreTransferLimit 是否忽略传输限制
     * @param simulate 如果为 true，仅模拟放电，不实际改变物品 NBT
     * @param remove 如果为 true，实际从物品中移除能量
     * @return 实际抽取的能量值（EU）
     */
    long discharge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate, boolean remove);

    /**
     * 获取物品的当前电荷量。
     *
     * @param stack 物品堆
     * @return 当前电荷量（EU）
     */
    long getCharge(ItemStack stack);

    /**
     * 检查物品是否有足够的能量执行指定操作。
     *
     * @param stack 物品堆
     * @param amount 需要的能量值（EU）
     * @return 如果能量足够则返回 true
     */
    boolean canUse(ItemStack stack, long amount);

    /**
     * 消耗物品中的能量以执行操作。
     *
     * @param stack 物品堆
     * @param amount 要消耗的能量值（EU）
     * @param entity 使用物品的实体
     * @return 如果成功消耗能量则返回 true
     */
    boolean use(ItemStack stack, long amount, LivingEntity entity);

    /**
     * 从实体穿着的装甲中抽取能量为手持物品充电。
     *
     * @param stack 要充电的物品堆
     * @param entity 实体
     */
    void chargeFromArmor(ItemStack stack, LivingEntity entity);

    /**
     * 获取此物品的电荷状态提示文本。
     *
     * @param stack 物品堆
     * @return 提示文本
     */
    String getToolTip(ItemStack stack);
}
