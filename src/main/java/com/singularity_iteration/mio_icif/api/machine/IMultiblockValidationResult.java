package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 多方块结构验证结果。
 *
 * <p>使用 {@link #success(Set)} 或 {@link #failure(String)} 创建结果，
 * 无需引用内部实现类。
 */
public interface IMultiblockValidationResult {

    boolean isValid();

    String getErrorMessage();

    Set<BlockPos> getStructureBlocks();

    Map<String, Object> getStructureData();

    static IMultiblockValidationResult success(Set<BlockPos> structureBlocks) {
        return new Impl(true, null, structureBlocks, Collections.emptyMap());
    }

    static IMultiblockValidationResult success(Set<BlockPos> structureBlocks, Map<String, Object> structureData) {
        return new Impl(true, null, structureBlocks, structureData);
    }

    static IMultiblockValidationResult failure(String errorMessage) {
        return new Impl(false, errorMessage, Collections.emptySet(), Collections.emptyMap());
    }

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private boolean valid;
        private String errorMessage = "";
        private final java.util.Set<BlockPos> structureBlocks = new java.util.HashSet<>();
        private final Map<String, Object> structureData = new HashMap<>();

        public Builder setValid(boolean valid) { this.valid = valid; return this; }

        public Builder setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public Builder addBlock(BlockPos pos) { this.structureBlocks.add(pos); return this; }

        public Builder addBlocks(java.util.Collection<BlockPos> positions) { this.structureBlocks.addAll(positions); return this; }

        public Builder setStructureData(String key, Object value) { this.structureData.put(key, value); return this; }

        public IMultiblockValidationResult build() {
            return new Impl(valid, errorMessage, structureBlocks, structureData);
        }
    }

    final class Impl implements IMultiblockValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Set<BlockPos> structureBlocks;
        private final Map<String, Object> structureData;

        Impl(boolean valid, String errorMessage, Set<BlockPos> structureBlocks, Map<String, Object> structureData) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.structureBlocks = Collections.unmodifiableSet(structureBlocks);
            this.structureData = Collections.unmodifiableMap(structureData);
        }

        @Override public boolean isValid() { return valid; }
        @Override public String getErrorMessage() { return errorMessage; }
        @Override public Set<BlockPos> getStructureBlocks() { return structureBlocks; }
        @Override public Map<String, Object> getStructureData() { return structureData; }
    }
}