package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 基础散热�? *
 * 特性（参考IC2）：
 * - 最大热量存储：1000�? * - 自身散热速度�?�?tick
 * - 从周围燃料棒吸收热量
 * - 超过热量上限会熔毁（耐久度变化?，物品消失）
 *
 * 工作原理�? * - 每tick从周围的燃料棒吸收热�? * - 同时向外界散�?点热�?tick
 * - 如果吸收的热量大于散发的热量，热量会积累
 * - 热量与耐久度成反比，热量越高耐久度越�? * - 当热量达�?000时，散热片熔毁消耗? */
@SuppressWarnings("null")
public class mio_icif_basic_heat_vent extends mio_icif_heat_vent {

    // 基础散热片参数
public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 6;
    public static final int REACTOR_ABSORPTION = 0;    // 不从反应堆直接吸热，而是从周围燃料棒吸热

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_basic_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

}


