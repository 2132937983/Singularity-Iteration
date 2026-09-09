package com.singularity_iteration.mio_icif.api.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * 动能 (KU) API
 *
 * <p>提供与 mio_icif 动能系统交互的接口，包括：
 * <ul>
 *   <li>查询方块的动能状态</li>
 *   <li>获取动能存储信息</li>
 *   <li>注入/提取动能</li>
 *   <li>查询动能连接</li>
 * </ul>
 */
public interface IKineticAPI {

    /**
     * 获取指定位置的动能存储信息
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 动能存储访问接口；如果位置没有动能方块或存储信息不可用则返回空 Optional
     */
    Optional<IKineticStorageAccess> getKineticStorage(Level world, BlockPos pos);

    /**
     * 检查指定位置是否有动能方块
     *
     * @param world 世界
     * @param pos 方块位置
     * @return true 如果位置有动能方块
     */
    boolean hasKineticTile(Level world, BlockPos pos);

    /**
     * 获取指定位置的动能连接方向
     *
     * @param world 世界
     * @param pos 方块位置
     * @return 连接方向集合
     */
    Collection<Direction> getKineticConnections(Level world, BlockPos pos);

    /**
     * 向指定位置的动能存储注入动能。
     *
     * @param world 世界
     * @param pos 方块位置
     * @param amount 要注入的动能（KU），必须 ≥ 0
     * @param simulate 如果 true 只模拟不实际修改
     * @return 实际注入的动能（KU）；如果位置没有动能方块返回 0
     */
    default long insertKinetic(Level world, BlockPos pos, long amount, boolean simulate) {
        return getKineticStorage(world, pos).map(s -> s.insertKinetic(amount, simulate)).orElse(0L);
    }

    /**
     * 从指定位置的动能存储提取动能。
     *
     * @param world 世界
     * @param pos 方块位置
     * @param amount 要提取的动能（KU），必须 ≥ 0
     * @param simulate 如果 true 只模拟不实际修改
     * @return 实际提取的动能（KU）；如果位置没有动能方块返回 0
     */
    default long extractKinetic(Level world, BlockPos pos, long amount, boolean simulate) {
        return getKineticStorage(world, pos).map(s -> s.extractKinetic(amount, simulate)).orElse(0L);
    }
}