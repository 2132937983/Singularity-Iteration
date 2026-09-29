// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.energy.IFECompatAPI;

import com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class FECompatAPIImpl implements IFECompatAPI {
    @Override public IEnergyStorage getStorage(Level world, BlockPos pos, Direction inputSide) {
        var adapter = new FECompatTile(world, pos, inputSide.getOpposite());
        return adapter.isValid() ? adapter : null;
    }
}
