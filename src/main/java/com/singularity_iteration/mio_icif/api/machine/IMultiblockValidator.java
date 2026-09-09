package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IMultiblockValidator {

    IMultiblockValidationResult validate(Level level, BlockPos controllerPos);

    default void onStructureFormed(Level level, BlockPos controllerPos, IMultiblockStructure manager) {}

    default void onStructureBroken(Level level, BlockPos controllerPos, IMultiblockStructure manager) {}

    default String getStructureName() {
        return getClass().getSimpleName();
    }
}