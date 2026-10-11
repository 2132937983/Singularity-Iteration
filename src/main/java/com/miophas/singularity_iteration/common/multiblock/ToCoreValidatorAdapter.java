package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Wraps a builtin {@link mio_icif_multiblock_validator} as a core
 * {@code IMultiblockValidator}.
 *
 * <p>{@link BuiltinMultiblockControllers} uses this adapter to put the builtin
 * validators into the core {@code MultiblockControllers} registry. The core
 * registry only accepts the core interface.
 */
final class ToCoreValidatorAdapter implements IMultiblockValidator {

    private final mio_icif_multiblock_validator builtinValidator;

    ToCoreValidatorAdapter(mio_icif_multiblock_validator builtinValidator) {
        this.builtinValidator = builtinValidator;
    }

    /**
     * Runs the builtin validator and converts its result.
     *
     * <p>A valid result keeps the member positions and the structure data.
     * A failure result keeps only the error message.
     */
    @Override
    public IMultiblockValidationResult validate(Level level, BlockPos controllerPos) {
        mio_icif_multiblock_validation_result result = builtinValidator.validate(level, controllerPos);
        if (result.isValid()) {
            return IMultiblockValidationResult.success(result.getStructureBlocks(), result.getStructureData());
        }
        return IMultiblockValidationResult.failure(result.getErrorMessage());
    }

    /**
     * Forwards the call only if {@code structure} is a builtin manager.
     *
     * <p>A structure of another type gets no callback. The builtin callbacks need
     * the manager type.
     */
    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, IMultiblockStructure structure) {
        if (structure instanceof mio_icif_multiblock_manager<?> manager) {
            builtinValidator.onStructureFormed(level, controllerPos, manager);
        }
    }

    /**
     * Forwards the call only if {@code structure} is a builtin manager.
     *
     * <p>A structure of another type gets no callback.
     */
    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, IMultiblockStructure structure) {
        if (structure instanceof mio_icif_multiblock_manager<?> manager) {
            builtinValidator.onStructureBroken(level, controllerPos, manager);
        }
    }

    @Override
    public String getStructureName() {
        return builtinValidator.getStructureName();
    }
}
