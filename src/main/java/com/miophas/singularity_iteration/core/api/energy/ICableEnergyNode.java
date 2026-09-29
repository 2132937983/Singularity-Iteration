package com.miophas.singularity_iteration.core.api.energy;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyConductor;
import net.minecraft.core.Direction;

/** A conductor whose individual connections may be disabled. */
public interface ICableEnergyNode extends IEnergyConductor {
    boolean isDirectionBlocked(Direction direction);
}
