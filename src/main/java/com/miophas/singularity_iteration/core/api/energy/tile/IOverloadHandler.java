// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.tile;

/**
 * Allows a tile entity to suppress or customize the default overload explosion.
 *
 * <p>When the energy net detects that a packet exceeds a sink's tier, it calls
 * {@link #onOverload(int)} before exploding the block. If the handler returns
 * {@code true}, the default explosion is cancelled.</p>
 *
 * <p>This is the SI equivalent of IC2's {@code ic2.api.energy.tile.IOverloadHandler}.</p>
 */
public interface IOverloadHandler {
    /**
     * Called when a voltage overload is about to destroy this tile.
     *
     * @param tier the tier index of the overflowing packet
     * @return {@code true} to cancel the default explosion, {@code false} to proceed
     */
    boolean onOverload(int tier);
}