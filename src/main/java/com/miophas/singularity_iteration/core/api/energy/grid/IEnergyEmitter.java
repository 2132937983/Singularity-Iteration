// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

import net.minecraft.core.Direction;

/** A side-aware SI output port; this interface performs no transfer. */
public interface IEnergyEmitter extends IEnergyTile {
    boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction);
}
