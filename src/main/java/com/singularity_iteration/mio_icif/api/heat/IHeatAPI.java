package com.singularity_iteration.mio_icif.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * 热能 (HU) API
 *
 * <p>提供与 mio_icif 热能系统交互的接口，包括：
 * <ul>
 *   <li>查询方块的热能状态</li>
 *   <li>获取热能存储信息</li>
 *   <li>注入/提取热能</li>
 *   <li>查询热能连接</li>
 * </ul>
 */
public interface IHeatAPI {

    /**
     * 获取指定位置的热能存储信息
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 热能存储访问接口；如果位置没有热能方块或存储信息不可用则返回空 Optional
     */
    Optional<IHeatStorageAccess> getHeatStorage(Level world, BlockPos pos);

    /**
     * 检查指定位置是否有热能方块
     *
     * @param world 世界
     * @param pos 方块位置
     * @return true 如果位置有热能方块
     */
    boolean hasHeatTile(Level world, BlockPos pos);

    /**
     * 获取指定位置的热能连接方向
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 连接方向集合
     */
    Collection<Direction> getHeatConnections(Level world, BlockPos pos);

    /**
     * 向指定位置的热能存储注入热量。
     *
     * @param world 世界
     * @param pos 方块位置
     * @param amount 要注入的热量（HU），必须 ≥ 0
     * @param simulate 如果 true 只模拟不实际修改
     * @return 实际注入的热量（HU）；如果位置没有热能方块返回 0
     */
    default long insertHeat(Level world, BlockPos pos, long amount, boolean simulate) {
        return getHeatStorage(world, pos).map(s -> s.insertHeat(amount, simulate)).orElse(0L);
    }

    /**
     * 从指定位置的热能存储提取热量。
     *
     * @param world 世界
     * @param pos 方块位置
     * @param amount 要提取的热量（HU），必须 ≥ 0
     * @param simulate 如果 true 只模拟不实际修改
     * @return 实际提取的热量（HU）；如果位置没有热能方块返回 0
     */
    default long extractHeat(Level world, BlockPos pos, long amount, boolean simulate) {
        return getHeatStorage(world, pos).map(s -> s.extractHeat(amount, simulate)).orElse(0L);
    }
}