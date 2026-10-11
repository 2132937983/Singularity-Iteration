package com.miophas.singularity_iteration.core.api.network;

import com.miophas.singularity_iteration.core.api.network.INetworkDataProvider;

import java.util.List;

/**
 * Network data provider interface.
 * <p>
 * Equivalent to {@code INetworkDataProvider} in IC2 1.12.2.
 * A block entity that implements this interface declares which fields need network sync.
 */
public interface INetworkDataProvider {

    /**
     * Gets the names of the fields to sync.
     * <p>
     * The mod sends these fields to the client automatically when the block entity loads.
     * When the server changes a field, {@code TileEntityFieldUpdatePacket} sends the update.
     *
     * @return the names of the fields to sync
     */
    List<String> getNetworkedFields();
}
