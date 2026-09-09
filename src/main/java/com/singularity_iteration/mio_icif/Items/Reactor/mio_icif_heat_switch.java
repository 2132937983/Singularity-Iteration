package com.singularity_iteration.mio_icif.Items.Reactor;

import com.singularity_iteration.mio_icif.api.reactor.ReactorComponentType;

import com.singularity_iteration.mio_icif.Items.DataComponent.ReactorComponentData;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 热交换器基类
 *
 * 特性（参考IC2）：
 * - 可以从燃料棒吸热
 * - 用于元件之间热量交换
 * - 可以向核反应堆传递热�? * - 自身有热量存�? * - 超过热量上限会熔�? *
 * 工作原理�? * - 从相邻燃料棒吸收热量
 * - 向相邻其他元件传递热�? * - 向核反应堆传递热�? * - 热量与耐久度成反比
 *
 * 热交换器类型�? * - 基础热交换器�?500热量，向相邻传�?2，向反应堆传�?
 * - 反应堆热交换器：5000热量，不向相邻传递，向反应堆传�?2
 * - 元件热交换器�?000热量，向相邻传�?6，不向反应堆传�? * - 高级热交换器�?0000热量，向相邻传�?4，向反应堆传�?
 */
@SuppressWarnings("null")
public class mio_icif_heat_switch extends mio_icif_reactor {

    // 最大热量存�
protected final int maxHeatStorage;

    // 向相邻元件传递的热量
    protected final int heatTransferToAdjacent;

    // 向核反应堆传递的热量
    protected final int heatTransferToReactor;

    /**
     * 构造函数
 * @param properties 物品属性
 * @param maxHeatStorage 最大热量存�
 * @param heatTransferToAdjacent 向相邻元件传递的热量
     * @param heatTransferToReactor 向核反应堆传递的热量
     */
    public mio_icif_heat_switch(Properties properties, int maxHeatStorage,
                                int heatTransferToAdjacent, int heatTransferToReactor) {
        super(properties, maxHeatStorage, ReactorComponentType.HEAT_SWITCH, false, true);
        this.maxHeatStorage = maxHeatStorage;
        this.heatTransferToAdjacent = heatTransferToAdjacent;
        this.heatTransferToReactor = heatTransferToReactor;
    }

    // ==================== 热量存储相关 ====================

    /**
     * 获取当前存储的热�
 * @param stack 物品栈
 * @return 当前热量
     */
    @Override
    public int getStoredHeat(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data != null ? data.storedValue() : 0;
    }

    /**
     * 设置存储的热�
 * @param stack 物品栈
 * @param heat 热量�
 */
    @Override
    public void setStoredHeat(ItemStack stack, int heat) {
        heat = Math.max(0, Math.min(heat, maxHeatStorage));
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        if (data != null) {
            stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(heat));
        }
        if (heat >= maxHeatStorage) {
            stack.setCount(0);
        }
    }

    /**
     * 添加热量
     * @param stack 物品栈
 * @param heat 要添加的热量
     * @return 实际添加的热�
 */
    public int addHeat(ItemStack stack, int heat) {
        int currentHeat = getStoredHeat(stack);
        int maxAddable = maxHeatStorage - currentHeat;
        int actualAdd = Math.min(heat, maxAddable);

        if (actualAdd > 0) {
            setStoredHeat(stack, currentHeat + actualAdd);
        }

        return actualAdd;
    }

    /**
     * 减少热量
     * @param stack 物品栈
 * @param heat 要减少的热量
     * @return 实际减少的热�
 */
    public int removeHeat(ItemStack stack, int heat) {
        int currentHeat = getStoredHeat(stack);
        int actualRemove = Math.min(heat, currentHeat);

        if (actualRemove > 0) {
            setStoredHeat(stack, currentHeat - actualRemove);
        }

        return actualRemove;
    }

    // ==================== 热量传递相同?====================

    /**
     * 获取向相邻元件传递的热量
     * @return 传递热�
 */
    public int getHeatTransferToAdjacent() {
        return heatTransferToAdjacent;
    }

    /**
     * 获取向核反应堆传递的热量
     * @return 传递热�
 */
    public int getHeatTransferToReactor() {
        return heatTransferToReactor;
    }

    /**
     * 获取最大热量存�
 * @return 最大热�
 */
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }

    /**
     * 计算热量百分配
 * @param stack 物品栈
 * @return 热量百分比（0.0 - 1.0�
 */
    public double getHeatPercentage(ItemStack stack) {
        return (double) getStoredHeat(stack) * 100.0 / maxHeatStorage;
    }

    // ==================== 熔毁相关 ====================

    /**
     * 检查热交换器是否熔�
 * @param stack 物品栈
 * @return 是否熔毁
     */
    @Override
    public boolean isMelted(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage;
    }

    /**
     * 热交换器会熔�
 * @param stack 物品栈
 * @return 始终返回true
     */
    @Override
    public boolean shouldMelt(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
            getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
    }
}

