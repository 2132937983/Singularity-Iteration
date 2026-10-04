package com.miophas.singularity_iteration.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.event.MultiblockFormedEvent;
import com.miophas.singularity_iteration.core.api.event.MultiblockBrokenEvent;
import com.miophas.singularity_iteration.core.runtime.multiblock.ActiveMultiblocks;
import com.miophas.singularity_iteration.core.runtime.multiblock.MultiblockControllers;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

@SuppressWarnings("null")
public class mio_icif_multiblock_manager<T extends mio_icif_multiblock_validator> implements IMultiblockStructure {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BlockPos controllerPos;
    private final Set<BlockPos> structureBlocks = new HashSet<>();
    private final T validator;
    private boolean isValid = false;
    private long formationTime = 0;
    private final Map<String, Object> structureData = new HashMap<>();

    public mio_icif_multiblock_manager(BlockPos controllerPos, T validator) {
        this.controllerPos = controllerPos;
        this.validator = validator;
    }

    public boolean tryForm(Level level) {
        if (isValid) {
            return true;
        }

        mio_icif_multiblock_validation_result result = validator.validate(level, controllerPos);

        if (result.isValid()) {
            this.structureBlocks.clear();
            this.structureBlocks.addAll(result.getStructureBlocks());
            this.isValid = true;
            this.formationTime = level.getGameTime();
            this.structureData.putAll(result.getStructureData());
            registerStructure(level, this);
            onStructureFormed(level);
            return true;
        }

        return false;
    }

    public boolean validateStructure(Level level) {
        if (!isValid) {
            return false;
        }

        if (!quickValidationCheck(level)) {
            invalidateStructure(level);
            return false;
        }

        return true;
    }

    private boolean quickValidationCheck(Level level) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.isAir()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = controllerPos.relative(direction);
            if (!structureBlocks.contains(neighborPos)) {
                continue;
            }
            if (!level.isLoaded(neighborPos)) {
                continue;
            }
            BlockState state = level.getBlockState(neighborPos);
            if (state.isAir()) {
                return false;
            }
        }

        return true;
    }

    public boolean validateStructureFull(Level level) {
        if (!isValid) {
            return false;
        }

        for (BlockPos pos : structureBlocks) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                invalidateStructure(level);
                return false;
            }
        }

        mio_icif_multiblock_validation_result result = validator.validate(level, controllerPos);
        if (!result.isValid()) {
            invalidateStructure(level);
            return false;
        }

        return true;
    }

    public void invalidateStructure(Level level) {
        if (!isValid) {
            return;
        }

        isValid = false;
        unregisterStructure(level, this);
        onStructureBroken(level);
        structureBlocks.clear();
        structureData.clear();
    }

    @Override
    public boolean invalidate(Level level) {
        boolean wasValid = isValid;
        invalidateStructure(level);
        return wasValid;
    }

    protected void onStructureFormed(Level level) {
        validator.onStructureFormed(level, controllerPos, this);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockFormedEvent(level, controllerPos, this, validator.getStructureName()));
    }

    protected void onStructureBroken(Level level) {
        Set<BlockPos> formerBlocks = new HashSet<>(structureBlocks);
        validator.onStructureBroken(level, controllerPos, this);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockBrokenEvent(level, controllerPos, validator.getStructureName(), formerBlocks));
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public Set<BlockPos> getStructureBlocks() {
        return Collections.unmodifiableSet(structureBlocks);
    }

    public boolean isPartOfStructure(BlockPos pos) {
        return structureBlocks.contains(pos);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public long getFormationTime() {
        return formationTime;
    }

    @Override
    public Map<String, Object> getStructureData() {
        return Collections.unmodifiableMap(structureData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<BlockPos> getRedstonePorts() {
        Object positions = structureData.get("redstonePortPositions");
        if (positions instanceof Set) {
            return (Set<BlockPos>) positions;
        }
        return Collections.emptySet();
    }

    public void setStructureData(String key, Object value) {
        structureData.put(key, value);
    }

    public T getValidator() {
        return validator;
    }

    private static void registerStructure(Level level, mio_icif_multiblock_manager<?> structure) {
        ActiveMultiblocks.register(level, structure);
    }

    private static void unregisterStructure(Level level, mio_icif_multiblock_manager<?> structure) {
        ActiveMultiblocks.unregister(level, structure);
    }

    public static mio_icif_multiblock_manager<?> getStructureAt(Level level, BlockPos pos) {
        IMultiblockStructure structure = ActiveMultiblocks.getStructureAt(level, pos);
        return structure instanceof mio_icif_multiblock_manager<?> manager ? manager : null;
    }

    public static mio_icif_multiblock_manager<?> getStructureByController(Level level, BlockPos controllerPos) {
        IMultiblockStructure structure = ActiveMultiblocks.getStructureByController(level, controllerPos);
        return structure instanceof mio_icif_multiblock_manager<?> manager ? manager : null;
    }

    public static Collection<mio_icif_multiblock_manager<?>> getAllStructures(Level level) {
        var managers = new ArrayList<mio_icif_multiblock_manager<?>>();
        for (IMultiblockStructure structure : ActiveMultiblocks.getAllStructures(level)) {
            if (structure instanceof mio_icif_multiblock_manager<?> manager) {
                managers.add(manager);
            }
        }
        return managers;
    }

    public static boolean isControllerAt(Level level, BlockPos pos) {
        return getStructureByController(level, pos) != null;
    }

    public static boolean isPartOfAnyStructure(Level level, BlockPos pos) {
        return ActiveMultiblocks.isPartOfAnyStructure(level, pos);
    }

    public static void notifyBlockChanged(Level level, BlockPos changedPos) {
        var managers = new ArrayList<mio_icif_multiblock_manager<?>>();
        for (IMultiblockStructure structure : ActiveMultiblocks.getAllStructures(level)) {
            if (structure instanceof mio_icif_multiblock_manager<?> manager) {
                managers.add(manager);
            }
        }
        if (managers.isEmpty()) {
            tryFormStructureAt(level, changedPos);
            return;
        }

        boolean affectedExistingStructure = false;
        for (mio_icif_multiblock_manager<?> structure : managers) {
            BlockPos controllerPos = structure.getControllerPos();
            if (isWithinStructureRange(controllerPos, changedPos)) {
                affectedExistingStructure = true;
                if (!structure.validateStructureFull(level)) {
                    // invalidated
                }
            }
        }

        if (!affectedExistingStructure) {
            tryFormStructureAt(level, changedPos);
        }
    }

    private static void tryFormStructureAt(Level level, BlockPos pos) {
        tryFormControllerAt(level, pos);

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!level.isLoaded(checkPos)) continue;

                    mio_icif_multiblock_manager<?> existingStructure = getStructureByController(level, checkPos);
                    if (existingStructure != null && existingStructure.isValid()) continue;

                    if (tryFormControllerAt(level, checkPos)) return;
                }
            }
        }
    }

    /** 控制方块类型到验证器的映射来自 MultiblockControllers 注册表。 */
    private static boolean tryFormControllerAt(Level level, BlockPos pos) {
        var descriptor = MultiblockControllers.descriptorFor(level.getBlockState(pos).getBlock());
        if (descriptor == null) {
            return false;
        }

        mio_icif_multiblock_validator validator = new CoreValidatorAdapter(descriptor.validatorFactory().get());
        mio_icif_multiblock_manager<mio_icif_multiblock_validator> manager =
                new mio_icif_multiblock_manager<>(pos, validator);
        boolean formed = manager.tryForm(level);
        if (formed) LOGGER.info("[Multiblock] {} at {}: formed", descriptor.name(), pos);
        else LOGGER.debug("[Multiblock] {} at {}: failed", descriptor.name(), pos);
        return formed;
    }

    private static boolean isWithinStructureRange(BlockPos center, BlockPos pos) {
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= 2 && dy <= 2 && dz <= 2;
    }
}