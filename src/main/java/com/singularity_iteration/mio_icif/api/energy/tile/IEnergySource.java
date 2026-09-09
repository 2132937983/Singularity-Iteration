package com.singularity_iteration.mio_icif.api.energy.tile;

/**
 * 能量输出接口（能量生产者）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergySource}。
 * 实现此接口的方块是电网中的能量生产者，如发电机、太阳能板等。
 * <p>
 * 电网每 tick 调用 {@link #getOfferedEnergy()} 查询此方块提供多少能量，
 * 然后调用 {@link #drawEnergy(long)} 抽取能量。
 */
public interface IEnergySource extends IEnergyEmitter {

    /**
     * 获取此方块当前可提供的能量值（EU/tick）。
     * <p>
     * 返回值不应超过方块的当前存储量。
     *
     * @return 可提供的能量值（EU）
     */
    long getOfferedEnergy();

    /**
     * 从此方块抽取指定量的能量。
     * <p>
     * 实现者应从此方块的储能中减去指定量的能量。
     * 此方法仅在电网确认需要抽取能量后调用。
     * {@code amount} 始终为非负值，实现者无需处理负值情况。
     *
     * @param amount 要抽取的能量值（EU），始终 &ge; 0
     */
    void drawEnergy(long amount);

    /**
     * 获取此方块的输出电压等级。
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
     * @return 电压等级
     */
    int getSourceTier();
}