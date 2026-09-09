package com.singularity_iteration.mio_icif.multiblock;

import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * 多方块结构验证结果? */
@SuppressWarnings("null")
public class mio_icif_multiblock_validation_result {
    
    // 是否验证通过
    private final boolean valid;
    
    // 验证失败的原�
private final String errorMessage;
    
    // 结构中的所有方块位置
private final Set<BlockPos> structureBlocks;
    
    // 结构数据（可用于存储额外的结构信息）
    private final Map<String, Object> structureData;
    
    // 私有构造函数，使用 Builder 模式创建
    private mio_icif_multiblock_validation_result(boolean valid, String errorMessage, 
                                                   Set<BlockPos> structureBlocks, 
                                                   Map<String, Object> structureData) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.structureBlocks = structureBlocks != null ? Collections.unmodifiableSet(structureBlocks) : Collections.emptySet();
        this.structureData = structureData != null ? Collections.unmodifiableMap(structureData) : Collections.emptyMap();
    }
    
    /**
     * 创建验证成功的结果
 */
    public static mio_icif_multiblock_validation_result success(Set<BlockPos> structureBlocks) {
        return new mio_icif_multiblock_validation_result(true, null, structureBlocks, new HashMap<>());
    }
    
    /**
     * 创建验证成功的结果（带结构数据）
     */
    public static mio_icif_multiblock_validation_result success(Set<BlockPos> structureBlocks, 
                                                                 Map<String, Object> structureData) {
        return new mio_icif_multiblock_validation_result(true, null, structureBlocks, structureData);
    }
    
    /**
     * 创建验证失败的结果
 */
    public static mio_icif_multiblock_validation_result failure(String errorMessage) {
        return new mio_icif_multiblock_validation_result(false, errorMessage, null, null);
    }
    
    /**
     * 检查是否验证通过
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 获取结构中的所有方块位置
 */
    public Set<BlockPos> getStructureBlocks() {
        return structureBlocks;
    }
    
    /**
     * 获取结构数据
     */
    public Map<String, Object> getStructureData() {
        return structureData;
    }
    
    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 验证结果构建�
 */
    public static class Builder {
        private boolean valid = false;
        private String errorMessage = "";
        private final Set<BlockPos> structureBlocks = new HashSet<>();
        private final Map<String, Object> structureData = new HashMap<>();
        
        public Builder setValid(boolean valid) {
            this.valid = valid;
            return this;
        }
        
        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder addBlock(BlockPos pos) {
            this.structureBlocks.add(pos);
            return this;
        }
        
        public Builder addBlocks(Collection<BlockPos> positions) {
            this.structureBlocks.addAll(positions);
            return this;
        }
        
        public Builder setStructureData(String key, Object value) {
            this.structureData.put(key, value);
            return this;
        }
        
        public mio_icif_multiblock_validation_result build() {
            return new mio_icif_multiblock_validation_result(valid, errorMessage, structureBlocks, structureData);
        }
    }
}


