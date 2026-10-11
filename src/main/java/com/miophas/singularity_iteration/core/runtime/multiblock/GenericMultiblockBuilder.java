package com.miophas.singularity_iteration.core.runtime.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockBuilder;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Generic builder implementation in core. It runs the onFormed callback immediately after the structure forms,
 * and attaches the onBroken callback to the breakdown lifecycle of the structure.
 */
public final class GenericMultiblockBuilder implements IMultiblockBuilder {

    private IMultiblockValidator validator;
    @Nullable private BiConsumer<Level, BlockPos> onFormedCallback;
    @Nullable private BiConsumer<Level, BlockPos> onBrokenCallback;

    @Override
    public IMultiblockBuilder validator(IMultiblockValidator validator) {
        this.validator = validator;
        return this;
    }

    @Override
    public IMultiblockBuilder onFormed(BiConsumer<Level, BlockPos> callback) {
        this.onFormedCallback = callback;
        return this;
    }

    @Override
    public IMultiblockBuilder onBroken(BiConsumer<Level, BlockPos> callback) {
        this.onBrokenCallback = callback;
        return this;
    }

    @Override
    public @Nullable IMultiblockStructure buildAndRegister(Level level, BlockPos controllerPos) {
        if (validator == null) {
            throw new IllegalStateException("Validator must be set before building");
        }
        if (level == null || level.isClientSide()) {
            throw new IllegalArgumentException("Multiblocks can only be built on the server side");
        }

        var structure = new GenericMultiblockStructure(controllerPos, validator, onFormedCallback, onBrokenCallback);
        return structure.tryForm(level) ? structure : null;
    }
}
