package com.miophas.singularity_iteration.common.item.reactor;

import net.minecraft.world.item.ItemStack;

/**
 * Component heat vent.
 *
 * Properties (from IC2):
 * - Stores no heat.
 * - Does not absorb heat from the reactor or from fuel rods.
 * - Removes 4 heat per tick from each adjacent component.
 *
 * Operation:
 * - Each tick, it pulls heat from adjacent heat vents, heat exchangers and similar components.
 * - It releases the absorbed heat directly to the environment.
 * - It stores no heat. Thus it cannot overheat and melt.
 * - Its main function is to help other cooling components remove excess heat.
 *
 * Use cases:
 * - Use it with overclocked heat vents. They absorb heat fast but release it slowly.
 * - Use it to add cooling in compact reactor layouts.
 */
@SuppressWarnings("null")
public class mio_icif_component_heat_vent extends mio_icif_heat_vent {

    // 元件散热片参数
public static final int MAX_HEAT = 0;              // Stores no heat.
public static final int SELF_COOLING = 0;          // Releases no heat of its own.
public static final int REACTOR_ABSORPTION = 0;    // Absorbs no heat from the reactor.
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
     * Returns the stored heat. A component heat vent always stores 0 heat.
     * @param stack the component item stack
 * @return always 0
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


