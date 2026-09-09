package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_validator;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_validation_result;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

final class MultiblockBridge {

    private MultiblockBridge() {}

    static IMultiblockStructure findStructure(Level level, BlockPos pos) {
        return mio_icif_multiblock_manager.getStructureAt(level, pos);
    }

    static IMultiblockBuilder createBuilder() {
        return new MultiblockBuilderImpl();
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

    private static final class MultiblockBuilderImpl implements IMultiblockBuilder {
        private IMultiblockValidator validator;
        private BiConsumer<Level, BlockPos> onFormedCallback;
        private BiConsumer<Level, BlockPos> onBrokenCallback;

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
        public IMultiblockStructure buildAndRegister(Level level, BlockPos controllerPos) {
            if (validator == null) {
                throw new IllegalStateException("Validator must be set before building");
            }

            mio_icif_multiblock_validator internalValidator = new ValidatorAdapter(validator);
            mio_icif_multiblock_manager<mio_icif_multiblock_validator> manager =
                    new mio_icif_multiblock_manager<>(controllerPos, internalValidator);

            boolean formed = manager.tryForm(level);
            if (formed) {
                return manager;
            }
            return null;
        }
    }
}