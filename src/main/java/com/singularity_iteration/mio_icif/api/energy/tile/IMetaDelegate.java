package com.singularity_iteration.mio_icif.api.energy.tile;

import com.singularity_iteration.mio_icif.api.energy.event.EnergyTileEvent;

import java.util.List;

/**
 * 多方块能源委托接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IMetaDelegate}。
 * 实现此接口的方块可以将多个子方块组合为一个能源节点，
 * 常用于多方块结构（如 MFE、MFSU 等）。
 */
public interface IMetaDelegate extends EnergyTileEvent.IEnergyTileMarker {

    /**
     * 获取此方块的子能源方块列表。
     * <p>
     * 返回的列表应包含所有参与能源网络的子方块。
     * 电网将分别查询每个子方块的能源属性。
     *
     * @return 子能源方块列表，不能返回 null
     */
    List<EnergyTileEvent.IEnergyTileMarker> getSubTiles();
}