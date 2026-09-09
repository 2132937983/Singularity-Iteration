package com.singularity_iteration.mio_icif.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.Set;

public class MultiblockBrokenEvent extends Event {

    private final Level level;
    private final BlockPos controllerPos;
    private final String structureName;
    private final Set<BlockPos> formerStructureBlocks;

    public MultiblockBrokenEvent(Level level, BlockPos controllerPos, String structureName, Set<BlockPos> formerStructureBlocks) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.structureName = structureName;
        this.formerStructureBlocks = formerStructureBlocks;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public String getStructureName() {
        return structureName;
    }

    public Set<BlockPos> getFormerStructureBlocks() {
        return formerStructureBlocks;
    }
}