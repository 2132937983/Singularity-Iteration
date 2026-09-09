package com.singularity_iteration.mio_icif.Items.Reactor;

import net.minecraft.world.item.ItemStack;

/**
 * 元件散热�? *
 * 特性（参考IC2）：
 * - 自身不储存热�? * - 不从反应堆或燃料棒吸收热�? * - 只会散发周围元件4点热�?tick
 *
 * 工作原理�? * - 每tick从相邻的散热片、热交换器等元件中吸�?点热�? * - 直接将吸收的热量散发到外�? * - 自身不存储热量，因此不会过热熔毁
 * - 主要用于帮助其他散热组件散发多余热量
 *
 * 使用场景�? * - 配合超频散热片使用（超频散热片吸热快但散热慢，需要元件散热片帮助散热�? * - 在紧凑的反应堆布局中辅助散�? */
@SuppressWarnings("null")
public class mio_icif_component_heat_vent extends mio_icif_heat_vent {

    // 元件散热片参数
public static final int MAX_HEAT = 0;              // 不储存热�
public static final int SELF_COOLING = 0;          // 不散发自热热�
public static final int REACTOR_ABSORPTION = 0;    // 不从反应堆吸�
public static final int COMPONENT_COOLING = 4;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_component_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

    /**
     * 获取从周围元件吸热的速度
     * @return 元件散热速度
     */
    public int getComponentCooling() {
        return COMPONENT_COOLING;
    }

    /**
     * 元件散热片不会熔毁，因为它不存储热量
     * @param stack 物品栈
 * @return 始终返回false
     */
    @Override
    public boolean isMelted(ItemStack stack) {
        return false;
    }

    /**
     * 获取当前存储的热�?- 元件散热片始终为0
     * @param stack 物品栈
 * @return 始终返回0
     */
    @Override
    public int getStoredHeat(ItemStack stack) {
        return 0;
    }

    @Override
    public void setStoredHeat(ItemStack stack, int heat) {
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}


