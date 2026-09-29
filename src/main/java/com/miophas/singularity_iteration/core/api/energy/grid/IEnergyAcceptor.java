// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

import net.minecraft.core.Direction;

/** A side-aware SI input port; this interface performs no transfer. */
public interface IEnergyAcceptor extends IEnergyTile {
    boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction);
}
