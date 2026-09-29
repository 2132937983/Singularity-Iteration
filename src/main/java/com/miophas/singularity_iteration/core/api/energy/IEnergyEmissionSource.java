package com.miophas.singularity_iteration.core.api.energy;

import java.util.List;
import net.minecraft.core.BlockPos;

/** Multiple contact positions belonging to one authoritative energy source. */
public interface IEnergyEmissionSource {
    List<BlockPos> electricalContactPositions();
}
