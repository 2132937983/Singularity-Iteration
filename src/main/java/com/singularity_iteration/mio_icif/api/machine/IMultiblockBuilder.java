package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

public interface IMultiblockBuilder {

    IMultiblockBuilder validator(IMultiblockValidator validator);

    IMultiblockBuilder onFormed(BiConsumer<Level, BlockPos> callback);

    IMultiblockBuilder onBroken(BiConsumer<Level, BlockPos> callback);

    IMultiblockStructure buildAndRegister(Level level, BlockPos controllerPos);
}