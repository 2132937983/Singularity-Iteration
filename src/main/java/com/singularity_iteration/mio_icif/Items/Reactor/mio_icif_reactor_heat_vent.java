package com.singularity_iteration.mio_icif.Items.Reactor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 反应堆散热片
 *
 * 特性（参考IC2）：
 * - 最大热量存储：1000�? * - 自身散热速度�?�?tick
 * - 优先承载来自燃料棒的热量
 * - 当没有来自燃料棒的热量时，从反应堆吸�?点热�?tick
 * - 超过热量上限会熔毁（耐久度变化?，物品消失）
 *
 * 工作原理�? * - 优先从相邻燃料棒接收热量
 * - 如果没有来自燃料棒的热量，每tick从反应堆吸收5点热�? * - 同时向外界散�?点热�?tick
 * - 热量与耐久度成反比，热量越高耐久度越�? * - 当热量达�?0000时，散热片熔毁消耗? */
@SuppressWarnings("null")
public class mio_icif_reactor_heat_vent extends mio_icif_heat_vent {

    // 反应堆散热片参数
    public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 5;
    public static final int REACTOR_ABSORPTION = 5;

    // NBT键名
    private static final String RECEIVED_FROM_FUEL_ROD_KEY = "ReceivedFromFuelRod";

    /**
     * 构造函数?     * @param properties 物品属性?     */
    public mio_icif_reactor_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

    /**
     * 检查本tick是否从燃料棒接收了热�?     * @param stack 物品栈?     * @return 如果从燃料棒接收了热量返回true
     */
    public boolean hasReceivedFromFuelRod(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(RECEIVED_FROM_FUEL_ROD_KEY)) {
                return tag.getBoolean(RECEIVED_FROM_FUEL_ROD_KEY);
            }
        }
        return false;
    }

    /**
     * 设置是否从燃料棒接收了热�?     * @param stack 物品栈?     * @param received 是否接收了热�?     */
    public void setReceivedFromFuelRod(ItemStack stack, boolean received) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putBoolean(RECEIVED_FROM_FUEL_ROD_KEY, received);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 重置从燃料棒接收热量的标记（每tick开始时调用�?     * @param stack 物品栈?     */
    public void resetReceivedFromFuelRod(ItemStack stack) {
        setReceivedFromFuelRod(stack, false);
    }

    /**
     * 标记从燃料棒接收了热�?     * @param stack 物品栈?     */
    public void markReceivedFromFuelRod(ItemStack stack) {
        setReceivedFromFuelRod(stack, true);
    }

    /**
     * 执行散热操作（每tick调用一次）
     * 参考IC2机制造?     * 1. 先从反应堆吸热（reactorVent�?     * 2. 再自身散热（selfVent�?     * 反应堆散热片优先从反应堆吸热
     * @param stack 物品栈?     * @param reactorHeat 当前反应堆热�?     * @param reactorHeatDissipated 反应堆总散热量（引用传递）
     * @return 实际从反应堆吸收的热�?     */
    @Override
    public int cool(ItemStack stack, long reactorHeat, java.util.concurrent.atomic.AtomicInteger reactorHeatDissipated) {
        int absorbed = super.cool(stack, reactorHeat, reactorHeatDissipated);
        resetReceivedFromFuelRod(stack);
        return absorbed;
    }

}