package com.miophas.singularity_iteration.common.blockentity.producer;

/**
 * 采矿机工作范围提供者。
 *
 * <p>用于在客户端渲染工作范围边界时读取机器的水平工作半径。
 * 该接口必须同时可在服务端与客户端安全调用（仅依赖已同步的方块实体数据）。
 */
public interface MiningRangeProvider {
    /**
     * 采矿机水平工作范围的半径（以机器所在 X/Z 列为中心的方形半宽）。
     *
     * @return 半径，单位为方块；返回 0 或负数表示不渲染范围
     */
    int getMiningRangeRadius();
}
