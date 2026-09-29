// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

/**
 * A tile that is a routing conductor and a native energy endpoint at the same time.
 *
 * <p>This is the IC2 dual-node layout: the reference energy net registers a conductor
 * node and a sink node for the same tile, and its links never treat a sink as a
 * dead end, so a machine placed in the middle of a cable run stays part of one
 * connected net. IndependentSiEnergy keeps conductors and endpoints in separate
 * maps, so a tile that must do both has to say so explicitly.
 *
 * <p>Implementing this interface makes the tile keep its own ports (its owned,
 * network-controlled balance still charges and discharges) while energy is also
 * routed through its position to endpoints beyond it.
 *
 * <p>The block ID must additionally be registered as a conductor with
 * {@link EnergyNodeRegistry}, otherwise the topology never indexes the position
 * as a conductor and only the endpoint role is published.
 */
public interface IEnergyConductiveEndpoint {
}
