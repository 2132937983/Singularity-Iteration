package com.singularity_iteration.mio_icif.api.energy.tile;

/**
 * 多能量源接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IMultiEnergySource}。
 * 实现此接口的电源可以在每个 tick 内发送多个能量包，
 * 而不是只有一个大包。这对于多方块结构或高输出电源很有用。
 */
public interface IMultiEnergySource extends IEnergySource {

    /**
     * 检查此电源是否发送多个能量包。
     * <p>
     * 如果返回 true，电网将多次调用 {@link #getOfferedEnergy()} 和
     * {@link #drawEnergy(long)} 来获取和抽取能量，每次最多
     * {@link #getMultipleEnergyPacketAmount()} EU。
     *
     * @return 如果发送多个能量包则返回 true
     */
    boolean sendMultipleEnergyPackets();

    /**
     * 获取每个能量包的最大能量量（EU）。
     * <p>
     * 当 {@link #sendMultipleEnergyPackets()} 返回 true 时，
     * 电网将使用此值来确定每次能量包的最大大小。
     *
     * @return 每个能量包的最大能量量（EU）
     */
    int getMultipleEnergyPacketAmount();
}
