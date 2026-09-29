package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 把内置验证器（common 接口）注册进 core 控制方块注册表时的适配层。 */
final class ToCoreValidatorAdapter implements IMultiblockValidator {

    private final mio_icif_multiblock_validator builtinValidator;

    ToCoreValidatorAdapter(mio_icif_multiblock_validator builtinValidator) {
        this.builtinValidator = builtinValidator;
    }

    @Override
    public IMultiblockValidationResult validate(Level level, BlockPos controllerPos) {
        mio_icif_multiblock_validation_result result = builtinValidator.validate(level, controllerPos);
        if (result.isValid()) {
            return IMultiblockValidationResult.success(result.getStructureBlocks(), result.getStructureData());
        }
        return IMultiblockValidationResult.failure(result.getErrorMessage());
    }

    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, IMultiblockStructure structure) {
        if (structure instanceof mio_icif_multiblock_manager<?> manager) {
            builtinValidator.onStructureFormed(level, controllerPos, manager);
        }
    }

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
