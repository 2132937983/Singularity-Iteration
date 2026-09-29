package com.miophas.singularity_iteration.common.service.machine;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockBuilder;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;

import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_validator;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_validation_result;
import com.miophas.singularity_iteration.core.runtime.multiblock.ActiveMultiblocks;
import com.miophas.singularity_iteration.core.runtime.multiblock.GenericMultiblockBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;

public final class MultiblockBridge {

    private MultiblockBridge() {}

    public static IMultiblockStructure findStructure(Level level, BlockPos pos) {
        return ActiveMultiblocks.getStructureAt(level, pos);
    }

    /** core 通用构建器：onFormed/onBroken 回调随结构生命周期真实执行。 */
    public static IMultiblockBuilder createBuilder() {
        return new GenericMultiblockBuilder();
    }

    static IMultiblockValidationResult wrapResult(mio_icif_multiblock_validation_result internal) {
        return new MultiblockResultAdapter(internal);
    }

    static mio_icif_multiblock_validator unwrapValidator(IMultiblockValidator apiValidator) {
        return new ValidatorAdapter(apiValidator);
    }

    private static final class MultiblockResultAdapter implements IMultiblockValidationResult {
        private final mio_icif_multiblock_validation_result internal;

        MultiblockResultAdapter(mio_icif_multiblock_validation_result internal) {
            this.internal = internal;
        }

        @Override
        public boolean isValid() { return internal.isValid(); }

        @Override
        public String getErrorMessage() { return internal.getErrorMessage(); }

        @Override
        public Set<BlockPos> getStructureBlocks() { return internal.getStructureBlocks(); }

        @Override
        public Map<String, Object> getStructureData() { return internal.getStructureData(); }
    }

    private static final class ValidatorAdapter implements mio_icif_multiblock_validator {
        private final IMultiblockValidator apiValidator;

        ValidatorAdapter(IMultiblockValidator apiValidator) {
            this.apiValidator = apiValidator;
        }

        @Override
        public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
            IMultiblockValidationResult apiResult = apiValidator.validate(level, controllerPos);
            if (apiResult.isValid()) {
                return mio_icif_multiblock_validation_result.success(apiResult.getStructureBlocks(), apiResult.getStructureData());
            } else {
                return mio_icif_multiblock_validation_result.failure(apiResult.getErrorMessage());
            }
        }

        @Override
        public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
            apiValidator.onStructureFormed(level, controllerPos, manager);
        }

        @Override
        public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
            apiValidator.onStructureBroken(level, controllerPos, manager);
        }

        @Override
        public String getStructureName() {
            return apiValidator.getStructureName();
        }
    }

}