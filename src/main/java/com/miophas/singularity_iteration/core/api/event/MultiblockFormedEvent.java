package com.miophas.singularity_iteration.core.api.event;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.Set;

public class MultiblockFormedEvent extends Event {

    private final Level level;
    private final BlockPos controllerPos;
    private final IMultiblockStructure structure;
    private final String structureName;

    public MultiblockFormedEvent(Level level, BlockPos controllerPos, IMultiblockStructure structure, String structureName) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.structure = structure;
        this.structureName = structureName;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public IMultiblockStructure getStructure() {
        return structure;
    }

    public String getStructureName() {
        return structureName;
    }

    public Set<BlockPos> getStructureBlocks() {
        return structure.getStructureBlocks();
    }
}