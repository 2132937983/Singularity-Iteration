package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

public interface IMultiblockBuilder {

    IMultiblockBuilder validator(IMultiblockValidator validator);

    IMultiblockBuilder onFormed(BiConsumer<Level, BlockPos> callback);

    IMultiblockBuilder onBroken(BiConsumer<Level, BlockPos> callback);

    IMultiblockStructure buildAndRegister(Level level, BlockPos controllerPos);
}