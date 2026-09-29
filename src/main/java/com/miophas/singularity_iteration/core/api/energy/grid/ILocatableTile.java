// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Location contract for non-BlockEntity SI integration endpoints. */
public interface ILocatableTile extends IEnergyTile {
    Level getWorld();
    BlockPos getPos();
}
