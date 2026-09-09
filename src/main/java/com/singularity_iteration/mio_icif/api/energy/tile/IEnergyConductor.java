package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;

/**
 * 能量导体接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnergyConductor}。
 * 实现此接口的方块是电网中的导体（电缆），可以同时接收和输出能量，
 * 并具有能量传输损耗。
 * <p>
 * 导体方块通常不存储能量，而是将接收到的能量按一定损耗率传输给相邻方块。
 */
public interface IEnergyConductor extends IEnergyAcceptor, IEnergyEmitter {

    /**
     * 获取此导体的能量传输损耗率。
     * <p>
     * 返回值范围 0.0 ~ 1.0。
     * 0.0 表示无损耗，1.0 表示全部损耗。
     * <p>
     * 例如：0.05 表示每传输 100 EU 损耗 5 EU。
     *
     * @return 损耗率
     */
    double getConductionLoss();

    /**
     * 获取绝缘层可吸收的能量值。
     * <p>
     * 当导体受到超过此值的电压冲击时，绝缘层会被破坏。
     * 返回 0 表示此导体没有绝缘层。
     *
     * @return 绝缘层吸收上限（EU）
     */
    long getInsulationEnergyAbsorption();

    /**
     * 获取绝缘层破坏所需的能量值。
     * <p>
     * 当单次注入能量超过此值时，绝缘层被击穿。
     * 返回 0 表示此导体没有绝缘层或绝缘层已破坏。
     *
     * @return 绝缘层破坏阈值（EU）
     */
    long getInsulationBreakdownEnergy();

    /**
     * 获取导体本身破坏所需的能量值。
     * <p>
     * 当单次注入能量超过此值时，导体本身被破坏（可能引发爆炸）。
     *
     * @return 导体破坏阈值（EU）
     */
    long getConductorBreakdownEnergy();

    /**
     * 移除绝缘层。
     * <p>
     * 调用此方法后，导体的绝缘层被破坏，不再能承受电压冲击。
     * 如果此导体没有绝缘层，此方法不执行任何操作。
     */
    void removeInsulation();

    /**
     * 移除导体。
     * <p>
     * 调用此方法后，导体的损坏状态被标记，可能导致方块破坏或爆炸。
     * 如果此导体没有损坏机制，此方法不执行任何操作。
     */
    void removeConductor();

    default void onEnergyPass() {}

    /**
     * 获取此导体的电缆等级。
     *
     * <p>默认返回 LV（32 EU/t）。实现类应覆盖此方法返回实际电缆等级。
     * 不建议在此 default 方法中调用 {@code MioIcifAPI}，因为接口 default 方法
     * 可能在模组加载极早期被调用，此时 ModList 可能尚未就绪。
     *
     * @return 电缆等级，默认 LV
     */
    default ICableTier getCableTier() {
        return LV_TIER;
    }

    ICableTier LV_TIER = new ICableTier() {
        @Override public String getName() { return "lv"; }
        @Override public String getDisplayName() { return "LV"; }
        @Override public String getFullName() { return "Low Voltage"; }
        @Override public long getPowerRating() { return 32; }
        @Override public float getElectricDamage() { return 0.2f; }
        @Override public long getConductorBreakdownEnergy() { return 2048; }
        @Override public long getInsulationBreakdownEnergy() { return 512; }
        @Override public int getTier() { return 1; }
    };
}