package com.singularity_iteration.mio_icif.api.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/**
 * 物品网络事件监听接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code INetworkItemEventListener}。
 * 实现此接口的物品可以在网络事件到达时执行客户端逻辑。
 */
public interface INetworkItemEventListener {

    /**
     * 当物品收到网络事件时调用。
     * <p>
     * 此方法在客户端执行，用于处理从服务端同步的物品事件。
     *
     * @param stack 物品堆
     * @param player 接收事件的玩家
     * @param eventId 事件 ID
     */
    void onNetworkEvent(ItemStack stack, Player player, int eventId);
}
