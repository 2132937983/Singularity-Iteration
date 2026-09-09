package com.singularity_iteration.mio_icif.api.reactor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 反应堆组件基类接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IBaseReactorComponent}。
 * 所有反应堆元件都应实现此接口。
 */
public interface IBaseReactorComponent {

    /**
     * 检查此组件是否可以放置在指定反应堆中。
     *
     * @param stack 组件物品堆
     * @param reactor 目标反应堆
     * @return 如果可以放置则返回 true
     */
    default boolean canBePlacedIn(ItemStack stack, IReactor reactor) {
        return true;
    }

    /**
     * 获取燃料棒的联数（单联/双联/四联）。
     * 非燃料棒组件默认返回 1。
     *
     * @return 联数（1=单联, 2=双联, 4=四联）
     */
    default int getNumberOfCells() {
        return 1;
    }

    // ========== 组件类型信息 ==========

    /**
     * 获取组件类型
     */
    ReactorComponentType getComponentType();

    /**
     * 获取中子脉冲输出量（燃料棒）
     */
    default int getNeutronPulseOutput() {
        return 0;
    }

    /**
     * 获取热量输出 (HU)
     */
    default int getHeatOutput() {
        return 0;
    }

    /**
     * 获取最大热量存储 (HU)
     */
    default int getMaxHeatStorage() {
        return 0;
    }

    /**
     * 获取热量传递效率 (0-100)
     */
    default int getHeatTransferEfficiency() {
        return 0;
    }

    /**
     * 是否是 MOX 燃料
     */
    default boolean isMoxFuel() {
        return false;
    }

    /**
     * 是否是燃料棒
     */
    default boolean isFuelRod() {
        return getComponentType() == ReactorComponentType.FUEL_ROD;
    }

    /**
     * 是否已耗尽
     */
    default boolean isDepleted(ItemStack stack) {
        return false;
    }

    /**
     * 获取耗尽后的物品（null 表示无掉落）
     */
    default Item getDepletedItem() {
        return null;
    }
}
