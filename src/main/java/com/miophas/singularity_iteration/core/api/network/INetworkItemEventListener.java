package com.miophas.singularity_iteration.core.api.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/**
 * Interface to listen for item network events.
 * <p>
 * This is the equivalent of {@code INetworkItemEventListener} in IC2 for 1.12.2.
 * An item that implements this interface can run client logic when a network event arrives.
 */
public interface INetworkItemEventListener {

    /**
     * Called when the item receives a network event.
     * <p>
     * This method runs on the client side. It handles item events that the server sends.
     *
     * @param stack the item stack
     * @param player the player that receives the event
     * @param eventId the event ID
     */
    void onNetworkEvent(ItemStack stack, Player player, int eventId);
}
