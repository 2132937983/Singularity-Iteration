package com.miophas.singularity_iteration.core.api.network;

/**
 * Network update listener interface.
 * <p>
 * Equivalent to {@code INetworkUpdateListener} in IC2 1.12.2.
 * A block entity that implements this interface gets a notification when a network field updates.
 * It also gets the latest field value that the server synchronizes.
 */
public interface INetworkUpdateListener {

    /**
     * Called when a network-synchronized field updates (with the field value).
     * <p>
     * This method runs on the client. It responds to field changes that the server synchronizes.
     * {@code value} is the latest field value that the server sends. It is transmitted as a {@code double}.
     * You can safely cast it to {@code int}, {@code long}, {@code float} or {@code boolean}.
     * <p>
     * The implementation must apply the value to the field that has the given field name:
     * <pre>{@code
     * switch (fieldName) {
     *     case "progress" -> this.progress = (int) value;
     *     case "energy"   -> this.energy = (long) value;
     *     case "active"   -> this.active = value != 0;
     *     default         -> {} // Unknown field: ignore it
     * }
     * }</pre>
     *
     * @param fieldName the name of the field that updated
     * @param value     the new value of the field (synchronized from the server)
     */
    default void onNetworkUpdate(String fieldName, double value) {
        onNetworkUpdate(fieldName);
    }

    /**
     * Called when a network-synchronized field updates (field name only, no value).
     * <p>
     * This method runs on the client. It is called when a legacy network packet (with no value) arrives.
     * The default implementation does nothing. Override this method for legacy compatibility,
     * or override {@link #onNetworkUpdate(String, double)} to get the field value.
     *
     * @param fieldName the name of the field that updated
     * @deprecated Use {@link #onNetworkUpdate(String, double)} to get the field value that the server synchronizes.
     */
    @Deprecated
    default void onNetworkUpdate(String fieldName) {
    }
}