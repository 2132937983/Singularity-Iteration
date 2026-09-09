package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

/**
 * 爆炸威力覆盖接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IExplosionPowerOverride}。
 * 实现此接口的能源方块在过载或损坏时可以自定义爆炸行为，
 * 而不是使用默认的爆炸逻辑。
 */
public interface IExplosionPowerOverride extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 检查此方块是否应该爆炸。
     * <p>
     * 返回 false 可以阻止方块在过载时爆炸，
     * 改为执行其他破坏逻辑（如降级或损坏）。
     *
     * @return 如果应该爆炸则返回 true
     */
    boolean shouldExplode();

    /**
     * 获取爆炸威力。
     * <p>
     * 返回值定义了爆炸的破坏范围和强度。
     * 返回 0 表示无爆炸（即使 {@link #shouldExplode()} 返回 true）。
     *
     * @param tier 过载能量的电压等级
     * @param power 基础爆炸威力
     * @return 实际爆炸威力
     */
    float getExplosionPower(int tier, float power);
}