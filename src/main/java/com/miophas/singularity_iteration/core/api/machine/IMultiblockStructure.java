package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;

/**
 * Interface that multiblock structures must implement.
 *
 * <p>The API layer reads structure data through this interface. It does not depend directly on the
 * implementation class {@code mio_icif_multiblock_manager}.
 *
 * <p>Use {@link #find(Level, BlockPos)} to find the structure at a position.
 * Use {@link #builder()} to make a new multiblock structure.
 */
public interface IMultiblockStructure {

    boolean isValid();

    /** Gets the controller block position. The registry of active structures uses it as the primary key. */
    BlockPos getControllerPos();

    Set<BlockPos> getStructureBlocks();

    long getFormationTime();

    Map<String, Object> getStructureData();

    Set<BlockPos> getRedstonePorts();

    /** Checks if a position is part of this structure (the controller and the member blocks). */
    boolean isPartOfStructure(BlockPos pos);

    /**
     * Breaks this structure and runs the onBroken lifecycle callback.
     *
     * @return true if the structure was valid and is now broken
     */
    boolean invalidate(Level level);

    /**
     * Finds the multiblock structure that contains a position.
     *
     * <p>This is the only bridge between the API layer and the internal multiblock manager.
     * If the internal implementation class changes, change only this method.
     *
     * @param level the level
     * @param pos the block position (the controller or any block in the structure)
     * @return the structure, or null if there is none
     */
    static IMultiblockStructure find(Level level, BlockPos pos) {
        return MioIcifAPI.instance().getMultiblockAPI().findStructure(level, pos);
    }

    /**
     * Makes a multiblock structure builder.
     *
     * <p>Addon developers use this method to make custom multiblock structures. They do not need the internal implementation classes.
     *
     * <p>Example:
     * <pre>{@code
     * IMultiblockStructure structure = IMultiblockStructure.builder()
     *     .validator(new MyValidator())
     *     .onFormed((level, pos) -> { ... })
     *     .onBroken((level, pos) -> { ... })
     *     .buildAndRegister(level, controllerPos);
     * }</pre>
     *
     * @return a multiblock structure builder
     */
    static IMultiblockBuilder builder() {
        return MioIcifAPI.instance().getMultiblockAPI().createBuilder();
    }
}