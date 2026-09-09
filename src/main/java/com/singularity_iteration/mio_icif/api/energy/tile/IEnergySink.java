package com.singularity_iteration.mio_icif.api.energy.tile;

import net.minecraft.core.Direction;

/**
 * 能量输入接口（能量消耗者）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergySink}。
 * 实现此接口的方块是电网中的能量消费者，如机器、电池箱等。
 * <p>
 * 电网每 tick 调用 {@link #getDemandedEnergy()} 查询此方块需要多少能量，
 * 然后调用 {@link #injectEnergy(Direction, long, double)} 注入能量。
 */
public interface IEnergySink extends IEnergyAcceptor {

    /**
     * 获取此方块当前需求的能量值（EU/tick）。
     * <p>
     * 返回值不应超过方块的存储容量剩余空间。
     *
     * @return 需求的能量值（EU）
     */
    long getDemandedEnergy();

    /**
     * 获取此方块的输入电压等级。
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
    int getSinkTier();

    /**
     * 向此方块注入能量。
     * <p>
     * 实现者应根据自身容量和接受能力处理注入的能量，
     * 并返回未被接受的剩余能量。
     *
     * @param from 能量注入方向
     * @param amount 注入的能量值（EU）
     * @param voltage 注入电压（EU/packet）
     * @return 未被接受的剩余能量
     */
    long injectEnergy(Direction from, long amount, double voltage);
}
