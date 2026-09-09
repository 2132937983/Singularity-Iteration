package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/**
 * 彩色能源方块接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IColoredEnergyTile}。
 * 实现此接口的能源方块（通常是电缆）可以被染色，
 * 用于区分不同的电网或实现电缆颜色过滤功能。
 */
public interface IColoredEnergyTile extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 获取指定方向的颜色。
     * <p>
     * 对于电缆，每个面可以有不同的颜色。
     * 返回 {@code null} 表示该面没有颜色（默认颜色）。
     *
     * @param side 方向
     * @return 颜色，如果该面没有颜色则返回 null
     */
    DyeColor getColor(Direction side);
}