package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 把注册在 core 里的通用验证器适配为内置管理器使用的验证器接口。 */
final class CoreValidatorAdapter implements mio_icif_multiblock_validator {

    private final IMultiblockValidator coreValidator;

    CoreValidatorAdapter(IMultiblockValidator coreValidator) {
        this.coreValidator = coreValidator;
    }

    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        IMultiblockValidationResult result = coreValidator.validate(level, controllerPos);
        if (result.isValid()) {
            return mio_icif_multiblock_validation_result.success(result.getStructureBlocks(), result.getStructureData());
        }
        return mio_icif_multiblock_validation_result.failure(result.getErrorMessage());
    }

    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        coreValidator.onStructureFormed(level, controllerPos, manager);
    }

    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        coreValidator.onStructureBroken(level, controllerPos, manager);
    }

    @Override
    public String getStructureName() {
        return coreValidator.getStructureName();
    }
}
