package com.miophas.singularity_iteration.core.api.network;

import net.minecraft.world.entity.player.Player;

/**
 * Listener interface for block entity network events (client side).
 * <p>
 * Equivalent to {@code INetworkClientTileEntityEventListener} in IC2 1.12.2.
 * A block entity that implements this interface receives network events on the client.
 */
public interface INetworkClientTileEntityEventListener {

    /**
     * Called when the client receives a network event for this block entity.
     *
     * @param player the player that caused the event (can be null)
     * @param eventId the event ID
     */
    void onNetworkEvent(Player player, int eventId);
}
