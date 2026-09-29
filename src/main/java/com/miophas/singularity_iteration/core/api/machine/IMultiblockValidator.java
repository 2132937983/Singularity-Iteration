package com.miophas.singularity_iteration.core.api.machine;

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