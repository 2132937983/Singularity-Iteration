package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Wraps a core {@code IMultiblockValidator} so that a builtin
 * {@link mio_icif_multiblock_manager} can use it.
 *
 * <p>The manager uses this adapter when it forms a structure from the
 * {@code MultiblockControllers} registry. For a builtin structure the call chain is:
 * manager, then this adapter, then {@link ToCoreValidatorAdapter}, then the builtin
 * validator. The two adapters only copy data between the two result types.
 * They do not change the check.
 */
final class CoreValidatorAdapter implements mio_icif_multiblock_validator {

    private final IMultiblockValidator coreValidator;

    CoreValidatorAdapter(IMultiblockValidator coreValidator) {
        this.coreValidator = coreValidator;
    }

    /**
     * Runs the core validator and converts its result.
     *
     * <p>A valid result keeps the member positions and the structure data.
     * A failure result keeps only the error message.
     */
    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        IMultiblockValidationResult result = coreValidator.validate(level, controllerPos);
        if (result.isValid()) {
            return mio_icif_multiblock_validation_result.success(result.getStructureBlocks(), result.getStructureData());
        }
        return mio_icif_multiblock_validation_result.failure(result.getErrorMessage());
    }

    /** Forwards the call. The manager is also a core {@code IMultiblockStructure}. */
    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        coreValidator.onStructureFormed(level, controllerPos, manager);
    }

    /** Forwards the call. The manager is also a core {@code IMultiblockStructure}. */
    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        coreValidator.onStructureBroken(level, controllerPos, manager);
    }

    @Override
    public String getStructureName() {
        return coreValidator.getStructureName();
    }
}
