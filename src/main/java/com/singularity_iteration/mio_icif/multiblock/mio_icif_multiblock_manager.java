package com.singularity_iteration.mio_icif.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.singularity_iteration.mio_icif.api.machine.IMultiblockStructure;
import com.singularity_iteration.mio_icif.api.event.MultiblockFormedEvent;
import com.singularity_iteration.mio_icif.api.event.MultiblockBrokenEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

@SuppressWarnings("null")
public class mio_icif_multiblock_manager<T extends mio_icif_multiblock_validator> implements IMultiblockStructure {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Level, Map<BlockPos, mio_icif_multiblock_manager<?>>> ACTIVE_STRUCTURES = new WeakHashMap<>();

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
        ACTIVE_STRUCTURES.computeIfAbsent(level, k -> new HashMap<>())
            .put(structure.getControllerPos(), structure);
    }

    private static void unregisterStructure(Level level, mio_icif_multiblock_manager<?> structure) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        if (structures != null) {
            structures.remove(structure.getControllerPos());
        }
    }

    public static mio_icif_multiblock_manager<?> getStructureAt(Level level, BlockPos pos) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        if (structures == null) {
            return null;
        }

        mio_icif_multiblock_manager<?> structure = structures.get(pos);
        if (structure != null) {
            return structure;
        }

        for (mio_icif_multiblock_manager<?> s : structures.values()) {
            if (s.isPartOfStructure(pos)) {
                return s;
            }
        }

        return null;
    }

    public static mio_icif_multiblock_manager<?> getStructureByController(Level level, BlockPos controllerPos) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        return structures != null ? structures.get(controllerPos) : null;
    }

    public static Collection<mio_icif_multiblock_manager<?>> getAllStructures(Level level) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        return structures != null ? Collections.unmodifiableCollection(structures.values()) : Collections.emptyList();
    }

    public static boolean isControllerAt(Level level, BlockPos pos) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        return structures != null && structures.containsKey(pos);
    }

    public static boolean isPartOfAnyStructure(Level level, BlockPos pos) {
        return getStructureAt(level, pos) != null;
    }

    public static void notifyBlockChanged(Level level, BlockPos changedPos) {
        Map<BlockPos, mio_icif_multiblock_manager<?>> structures = ACTIVE_STRUCTURES.get(level);
        if (structures == null) {
            tryFormStructureAt(level, changedPos);
            return;
        }

        boolean affectedExistingStructure = false;
        for (mio_icif_multiblock_manager<?> structure : new ArrayList<>(structures.values())) {
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
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_core) {
            mio_icif_multiblock_manager<mio_icif_gesu_validator> manager =
                    new mio_icif_multiblock_manager<>(pos, new mio_icif_gesu_validator());
            boolean formed = manager.tryForm(level);
            LOGGER.info("[Multiblock] GESU at " + pos + ": " + (formed ? "formed" : "failed"));
            if (formed) return;
        }

        if (state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_core) {
            mio_icif_multiblock_manager<mio_icif_large_fabricator_validator> manager =
                    new mio_icif_multiblock_manager<>(pos, new mio_icif_large_fabricator_validator());
            boolean formed = manager.tryForm(level);
            LOGGER.info("[Multiblock] LargeFabricator at " + pos + ": " + (formed ? "formed" : "failed"));
            if (formed) return;
        }

        if (state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
            mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> manager =
                    new mio_icif_multiblock_manager<>(pos, new mio_icif_fluid_reactor_validator());
            boolean formed = manager.tryForm(level);
            LOGGER.info("[Multiblock] Reactor at " + pos + ": " + (formed ? "formed" : "failed"));
            if (formed) return;
        }

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!level.isLoaded(checkPos)) continue;

                    BlockState checkState = level.getBlockState(checkPos);
                    mio_icif_multiblock_manager<?> existingStructure = getStructureByController(level, checkPos);
                    if (existingStructure != null && existingStructure.isValid()) continue;

                    if (checkState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_core) {
                        mio_icif_multiblock_manager<mio_icif_gesu_validator> manager =
                                new mio_icif_multiblock_manager<>(checkPos, new mio_icif_gesu_validator());
                        boolean formed = manager.tryForm(level);
                        LOGGER.info("[Multiblock] GESU at " + checkPos + ": " + (formed ? "formed" : "failed"));
                        if (formed) return;
                    }

                    if (checkState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_large_fabricator_core) {
                        mio_icif_multiblock_manager<mio_icif_large_fabricator_validator> manager =
                                new mio_icif_multiblock_manager<>(checkPos, new mio_icif_large_fabricator_validator());
                        boolean formed = manager.tryForm(level);
                        LOGGER.info("[Multiblock] LargeFabricator at " + checkPos + ": " + (formed ? "formed" : "failed"));
                        if (formed) return;
                    }

                    if (checkState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
                        mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> manager =
                                new mio_icif_multiblock_manager<>(checkPos, new mio_icif_fluid_reactor_validator());
                        boolean formed = manager.tryForm(level);
                        LOGGER.info("[Multiblock] Reactor at " + checkPos + ": " + (formed ? "formed" : "failed"));
                        if (formed) return;
                    }
                }
            }
        }
    }

    private static boolean isWithinStructureRange(BlockPos center, BlockPos pos) {
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= 2 && dy <= 2 && dz <= 2;
    }
}