// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Sided live FE adapter; conversion and actual capability calls share the product bridge. */
public interface IFECompatAPI {
    IEnergyStorage getStorage(Level world, BlockPos pos, Direction inputSide);
}
