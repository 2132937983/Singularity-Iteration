package com.singularity_iteration.mio_icif.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * FE (Forge Energy) ↔ EU 兼容 API
 *
 * <p>提供将 FE 机器接入 EU 电网的功能。
 * 转换比例：1 EU = 4 FE。
 *
 * <p>附属模组可以通过 {@link #createFECompatNode(Level, BlockPos, Direction)}
 * 创建一个 FE 兼容节点，使 FE 机器能够参与 EU 电网的能量交换。
 *
 * <p>使用示例：
 * <pre>{@code
 * IFECompatAPI feAPI = MioIcifAPI.instance().getFECompatAPI();
 *
 * // 创建 FE 兼容节点
 * long nodeId = feAPI.createFECompatNode(world, feMachinePos, Direction.NORTH);
 *
 * // 查询兼容节点状态
 * if (feAPI.isFECompatActive(world, feMachinePos)) {
 *     // FE 机器已接入 EU 电网
 * }
 *
 * // 移除兼容节点
 * feAPI.removeFECompatNode(world, feMachinePos);
 * }</pre>
 */
public interface IFECompatAPI {

    /**
     * 在指定位置创建 FE 兼容节点。
     *
     * <p>如果该位置已有 FE 兼容节点，会先移除旧节点再创建新的。
     * 节点会自动注册到 EU 电网中。
     *
     * @param world 世界
     * @param pos FE 机器位置（方块面的对侧）
     * @param side 连接方向
     * @return 节点 ID（大于 0 表示成功，0 表示失败）
     */
    long createFECompatNode(Level world, BlockPos pos, Direction side);

    /**
     * 移除指定位置的 FE 兼容节点。
     *
     * @param world 世界
     * @param pos FE 机器位置
     * @return true 如果成功移除
     */
    boolean removeFECompatNode(Level world, BlockPos pos);

    /**
     * 检查指定位置是否有活跃的 FE 兼容节点。
     *
     * @param world 世界
     * @param pos FE 机器位置
     * @return true 如果有活跃的 FE 兼容节点
     */
    boolean isFECompatActive(Level world, BlockPos pos);

    /**
     * 获取 FE↔EU 转换比例。
     *
     * @return 每 1 EU 对应的 FE 数量（当前为 4）
     */
    int getFEPerEU();

    /**
     * 将 EU 值转换为 FE 值。
     *
     * @param eu EU 数量
     * @return 等效的 FE 数量
     */
    default long euToFE(long eu) {
        return eu * getFEPerEU();
    }

    /**
     * 将 FE 值转换为 EU 值。
     *
     * @param fe FE 数量
     * @return 等效的 EU 数量
     */
    default long feToEU(long fe) {
        return fe / getFEPerEU();
    }
}
