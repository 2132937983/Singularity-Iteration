package com.miophas.singularity_iteration.core.api.network;

/**
 * Listener interface for block entity network events (server side).
 * <p>
 * Equivalent to {@code INetworkTileEntityEventListener} in IC2 1.12.2.
 * A block entity that implements this interface gets a notification when a network event occurs.
 */
public interface INetworkTileEntityEventListener {

    /**
     * Called when the block entity triggers a network event.
     * <p>
     * This method runs on the server side.
     *
     * @param eventId the event ID
     */
    void onNetworkEvent(int eventId);
}
