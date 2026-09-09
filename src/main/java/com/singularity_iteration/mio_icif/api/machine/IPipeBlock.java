package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.Direction;

import java.util.Collection;

/**
 * 管道方块接口。
 *
 * <p>用于描述流体/物品管道的行为和状态。
 * 实现此接口的方块可以管理连接方向、抽取/推送配置和传输速率。
 */
public interface IPipeBlock {

    /**
     * 管道类型枚举
     */
    enum PipeType {
        /** 物品管道 */
        ITEM,
        /** 流体管道 */
        FLUID,
        /** 电缆（能量管道） */
        CABLE,
        /** 通用管道 */
        UNIVERSAL
    }

    /**
     * 获取管道类型
     * @return 管道类型
     */
    PipeType getPipeType();

    /**
     * 获取当前已连接的方向集合
     * @return 已连接方向
     */
    Collection<Direction> getConnections();

    /**
     * 检查指定方向是否连接
     * @param side 方向
     * @return true 如果已连接
     */
    boolean isConnected(Direction side);

    /**
     * 检查指定方向是否处于抽取模式
     * @param side 方向
     * @return true 如果该方向正在抽取
     */
    boolean isExtracting(Direction side);

    /**
     * 设置指定方向的抽取模式
     * @param side       方向
     * @param extracting 是否抽取
     */
    void setExtracting(Direction side, boolean extracting);

    /**
     * 获取传输速率
     * @return 每 tick 最大传输量
     */
    int getTransferRate();

    /**
     * 检查管道是否被阻断
     * @return true 如果管道被阻断
     */
    default boolean isBlocked() {
        return false;
    }
}
