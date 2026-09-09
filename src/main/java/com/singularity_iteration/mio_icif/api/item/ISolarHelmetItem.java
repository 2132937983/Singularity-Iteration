package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 太阳能头盔接口。
 *
 * <p>扩展 {@link IBatteryItem}，为太阳能头盔/护甲提供统一的 API。
 * 太阳能头盔在白天（或满足光照条件时）自动为自身或其他装备充电。
 */
public interface ISolarHelmetItem extends IBatteryItem {

    /**
     * 获取太阳能发电速率
     * @return 每 tick 发电量（EU）
     */
    long getGenerationRate();

    /**
     * 检查是否需要天空可见（无方块遮挡）
     * @return true 如果需要天空可见
     */
    boolean requiresSky();

    /**
     * 检查是否仅在白天工作
     * @return true 如果仅白天发电
     */
    boolean isDayOnly();

    /**
     * 获取最低光照等级要求（0-15）
     * @return 最低光照等级
     */
    default int getMinLightLevel() {
        return 0;
    }

    /**
     * 检查当前条件是否满足发电要求
     *
     * @param stack     物品堆
     * @param skyLight  天空光照等级（0-15）
     * @param blockLight 方块光照等级（0-15）
     * @param canSeeSky 是否可以看到天空
     * @param isDaytime 是否为白天
     * @return true 如果满足发电条件
     */
    default boolean canGenerate(ItemStack stack, int skyLight, int blockLight, boolean canSeeSky, boolean isDaytime) {
        if (requiresSky() && !canSeeSky) return false;
        if (isDayOnly() && !isDaytime) return false;
        int light = requiresSky() ? skyLight : blockLight;
        return light >= getMinLightLevel();
    }

    /**
     * 对装备了此头盔的玩家执行太阳能充电
     *
     * @param stack           物品堆
     * @param chargeTarget    充电目标物品堆
     * @param simulate        是否仅模拟
     * @return 实际充入的能量
     */
    default long chargeTarget(ItemStack stack, ItemStack chargeTarget, boolean simulate) {
        if (chargeTarget.isEmpty()) return 0;
        if (chargeTarget.getItem() instanceof IBatteryItem battery) {
            long rate = getGenerationRate();
            if (simulate) {
                long current = battery.getEnergy(chargeTarget);
                long max = battery.getMaxEnergy();
                return Math.min(rate, max - current);
            }
            return battery.addEnergy(chargeTarget, rate);
        }
        return 0;
    }
}
