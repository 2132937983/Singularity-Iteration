package com.singularity_iteration.mio_icif.Items.Reactor;

/**
 * 超频散热�? *
 * 特性（参考IC2）：
 * - 最大热量存储：10000�? * - 自身散热速度�?0�?tick
 * - 从反应堆吸热速度�?8�?tick
 * - 不会从燃料棒吸收热量
 * - 由于吸热速度大于散热速度，多余热量会积累
 * - 必须配合热交换器或元件散热片来散发多余热�? * - 超过热量上限会熔毁（耐久度变化?�? *
 * 使用建议�? * - 配合元件散热片使用（四周都摆满）
 * - 在流体反应堆中确保冷却液循环顺畅
 * - 可以使用脉冲模式（运�?秒停止?秒）来避免过程? */
@SuppressWarnings("null")
public class mio_icif_overclocked_heat_vent extends mio_icif_heat_vent {

    // 超频散热片参数
public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 20;
    public static final int REACTOR_ABSORPTION = 36;

    /**
     * 构造函数
 * @param properties 物品属性
 */
    public mio_icif_overclocked_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

}


