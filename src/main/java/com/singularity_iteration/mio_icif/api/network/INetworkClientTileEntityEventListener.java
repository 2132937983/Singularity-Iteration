package com.singularity_iteration.mio_icif.api.network;

import net.minecraft.world.entity.player.Player;

/**
 * 方块实体网络事件监听接口（客户端）。
 * <p>
 * 对应 IC2 1.12.2 的 {@code INetworkClientTileEntityEventListener}。
 * 实现此接口的方块实体可以在客户端收到网络事件通知。
 */
public interface INetworkClientTileEntityEventListener {

    /**
     * 当客户端收到方块实体的网络事件时调用。
     *
     * @param player 触发事件的玩家（可能为 null）
     * @param eventId 事件 ID
     */
    void onNetworkEvent(Player player, int eventId);
}
