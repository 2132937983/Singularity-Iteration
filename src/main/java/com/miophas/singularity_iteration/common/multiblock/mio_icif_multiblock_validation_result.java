package com.miophas.singularity_iteration.common.multiblock;

import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * Result of one multiblock shape check.
 *
 * <p>{@link mio_icif_multiblock_validator#validate} makes this object.
 * {@link mio_icif_multiblock_manager#tryForm} reads it. If the result is valid,
 * the manager copies the member positions and the structure data into its own state.
 *
 * <p>Contents:
 * <ul>
 *   <li>valid flag: {@code true} if the shape is complete.</li>
 *   <li>error message: the reason for a failure. It is for logs and debugging.
 *       It is {@code null} for a result from {@link #success}.</li>
 *   <li>member positions: all world positions of the structure, the controller
 *       included. It is empty for a failure result.</li>
 *   <li>structure data: extra values for the controller block entity, for example
 *       module counts ({@code "inputModuleCount"}) or the redstone port positions
 *       ({@code "redstonePortPositions"}). It is empty for a failure result.</li>
 * </ul>
 *
 * <p>The getters return read-only views. The views are not copies. If the caller
 * changes the source set or map after it makes the result, the result also changes.
 */
@SuppressWarnings("null")
public class mio_icif_multiblock_validation_result {
    
    // True if the shape is complete.
    private final boolean valid;
    
    // Reason for the validation failure. Null for a result from success().
private final String errorMessage;
    
    // All member positions, the controller included. Read-only view.
private final Set<BlockPos> structureBlocks;
    
    // Extra structure values, for example module counts. Read-only view.
    private final Map<String, Object> structureData;
    
    // Private constructor. Use the static factory methods or the Builder.
    // A null set or map becomes an empty read-only collection.
    private mio_icif_multiblock_validation_result(boolean valid, String errorMessage, 
                                                   Set<BlockPos> structureBlocks, 
                                                   Map<String, Object> structureData) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.structureBlocks = structureBlocks != null ? Collections.unmodifiableSet(structureBlocks) : Collections.emptySet();
        this.structureData = structureData != null ? Collections.unmodifiableMap(structureData) : Collections.emptyMap();
    }
    
    /**
     * Makes a valid result with no structure data.
     *
     * @param structureBlocks all member positions, the controller included
     * @return a valid result
     */
    public static mio_icif_multiblock_validation_result success(Set<BlockPos> structureBlocks) {
        return new mio_icif_multiblock_validation_result(true, null, structureBlocks, new HashMap<>());
    }
    
    /**
     * Makes a valid result with structure data.
     *
     * @param structureBlocks all member positions, the controller included
     * @param structureData the extra structure values, or {@code null} for none
     * @return a valid result
     */
    public static mio_icif_multiblock_validation_result success(Set<BlockPos> structureBlocks, 
                                                                 Map<String, Object> structureData) {
        return new mio_icif_multiblock_validation_result(true, null, structureBlocks, structureData);
    }
    
    /**
     * Makes a failure result.
     *
     * @param errorMessage the reason for the failure
     * @return a failure result with no members and no structure data
     */
    public static mio_icif_multiblock_validation_result failure(String errorMessage) {
        return new mio_icif_multiblock_validation_result(false, errorMessage, null, null);
    }
    
    /**
     * Tells if the shape is complete.
     *
     * @return {@code true} for a valid result
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the reason for the failure.
     *
     * @return the error message. It is {@code null} for a result from
     *         {@link #success}. It is {@code ""} for a Builder result with no message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Gets all member positions.
     *
     * @return a read-only view of the positions, the controller included
     */
    public Set<BlockPos> getStructureBlocks() {
        return structureBlocks;
    }
    
    /**
     * Gets the extra structure values.
     *
     * @return a read-only view of the map
     */
    public Map<String, Object> getStructureData() {
        return structureData;
    }
    
    /**
     * Makes a new Builder.
     *
     * @return a Builder with the valid flag set to {@code false}
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for the validation result.
     *
     * <p>The valid flag starts as {@code false}. Call {@link #setValid} with
     * {@code true} for a valid result. Do not change the Builder after
     * {@link #build}. The result keeps a view of the Builder collections, so a
     * later change also changes the result.
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


