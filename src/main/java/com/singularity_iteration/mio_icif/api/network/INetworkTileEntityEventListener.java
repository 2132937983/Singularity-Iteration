package com.singularity_iteration.mio_icif.api.network;

/**
 * 方块实体网络事件监听接口（服务端）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code INetworkTileEntityEventListener}。
 * 实现此接口的方块实体可以在触发网络事件时收到通知。
 */
public interface INetworkTileEntityEventListener {

    /**
     * 当方块实体触发网络事件时调用。
     * <p>
     * 此方法在服务端执行。
     *
     * @param eventId 事件 ID
     */
    void onNetworkEvent(int eventId);
}
