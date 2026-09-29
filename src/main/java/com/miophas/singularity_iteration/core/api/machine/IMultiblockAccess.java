package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Host-provided structure discovery and construction. */
public interface IMultiblockAccess {
    @Nullable IMultiblockStructure findStructure(Level level, BlockPos position);
    IMultiblockBuilder createBuilder();
}
