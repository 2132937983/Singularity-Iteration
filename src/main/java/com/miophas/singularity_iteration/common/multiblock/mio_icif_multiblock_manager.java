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

/**
 * Live state of one builtin multiblock structure around one controller.
 *
 * <p>See {@link mio_icif_multiblock_validator} for the package overview.
 *
 * <p>Life cycle:
 * <ol>
 *   <li>A caller makes a new manager for a controller position and a validator.
 *       The manager starts as not valid.</li>
 *   <li>{@link #tryForm} runs the validator. On success the manager stores the
 *       members and the structure data, registers itself in {@code ActiveMultiblocks},
 *       calls the formed callback and posts a {@code MultiblockFormedEvent}.</li>
 *   <li>{@link #validateStructure} (quick) and {@link #validateStructureFull} (full)
 *       check the formed structure again.</li>
 *   <li>{@link #invalidateStructure} ends the life cycle. A manager does not form
 *       again after this in normal use. Callers make a new manager.</li>
 * </ol>
 *
 * <p>{@code ActiveMultiblocks} keys the structures by controller position. A second
 * manager that forms at the same position replaces the first in the registry.
 * The first manager then stays valid but is not registered.
 *
 * <p>Thread rule: call all methods on the server thread.
 *
 * @param <T> the validator type
 */
@SuppressWarnings("null")
public class mio_icif_multiblock_manager<T extends mio_icif_multiblock_validator> implements IMultiblockStructure {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Origin of the shape. It does not change.
    private final BlockPos controllerPos;
    // Members from the last valid result, the controller included. Empty while not valid.
    private final Set<BlockPos> structureBlocks = new HashSet<>();
    private final T validator;
    private boolean isValid = false;
    // Game time of the last formation, in ticks.
    private long formationTime = 0;
    // Structure data from the last valid result. Empty while not valid.
    private final Map<String, Object> structureData = new HashMap<>();

    /**
     * Makes a manager that is not valid yet. It does not read the world.
     *
     * @param controllerPos the position of the controller block
     * @param validator the shape check for this structure type
     */
    public mio_icif_multiblock_manager(BlockPos controllerPos, T validator) {
        this.controllerPos = controllerPos;
        this.validator = validator;
    }

    /**
     * Runs the validator and forms the structure if the shape is complete.
     *
     * <p>If the manager is already valid, this method returns {@code true} and
     * does not run the validator. A failure does not change the manager.
     *
     * <p>On success the sequence is: copy the members and data, set the valid flag
     * and the formation time, register in {@code ActiveMultiblocks}, call
     * {@link mio_icif_multiblock_validator#onStructureFormed}, post the event.
     *
     * @param level the server world
     * @return {@code true} if the structure is formed after the call
     */
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

    /**
     * Runs the quick check and breaks the structure if the check fails.
     *
     * <p>The controller block entities call this method on a fixed interval.
     * The quick check does not run the validator. See {@link #quickValidationCheck}
     * for its limits.
     *
     * @param level the server world
     * @return {@code true} if the structure is still valid. {@code false} if the
     *         structure was not valid or the check broke it.
     */
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

    /**
     * Looks for air at the controller and at its 6 face neighbors.
     *
     * <p>Only face neighbors that are members are checked. A neighbor in an
     * unloaded chunk is skipped. The check does not test the block type. It does not
     * test members that are farther away, for example the oil rig frames.
     * Thus a replaced block or a removed far member does not fail this check.
     * {@link #validateStructureFull} finds those changes.
     */
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

    /**
     * Runs the full check and breaks the structure if the check fails.
     *
     * <p>Step 1 looks for air at each member position in a loaded chunk. Step 2 runs
     * the validator again. The method does not update the members or the structure
     * data after a passed check. Thus the stored module counts stay at the values
     * from the formation.
     *
     * @param level the server world
     * @return {@code true} if the structure is still valid
     */
    public boolean validateStructureFull(Level level) {
        if (!isValid) {
            return false;
        }

        for (BlockPos pos : structureBlocks) {
            // Skip a member in an unloaded chunk. This avoids a chunk load in this step.
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

    /**
     * Breaks the structure.
     *
     * <p>If the manager is not valid, this method does nothing. Otherwise the sequence
     * is: clear the valid flag, unregister from {@code ActiveMultiblocks}, call
     * {@link mio_icif_multiblock_validator#onStructureBroken}, post the event, clear
     * the members and the data. The callback still sees the old members.
     *
     * @param level the server world
     */
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

    /**
     * Core API form of {@link #invalidateStructure}.
     *
     * @param level the server world
     * @return {@code true} if the structure was valid before the call
     */
    @Override
    public boolean invalidate(Level level) {
        boolean wasValid = isValid;
        invalidateStructure(level);
        return wasValid;
    }

    /** Calls the validator callback, then posts a {@code MultiblockFormedEvent}. */
    protected void onStructureFormed(Level level) {
        validator.onStructureFormed(level, controllerPos, this);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockFormedEvent(level, controllerPos, this, validator.getStructureName()));
    }

    /**
     * Calls the validator callback, then posts a {@code MultiblockBrokenEvent}.
     *
     * <p>The event gets a copy of the members, because the caller clears the set after this call.
     */
    protected void onStructureBroken(Level level) {
        Set<BlockPos> formerBlocks = new HashSet<>(structureBlocks);
        validator.onStructureBroken(level, controllerPos, this);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockBrokenEvent(level, controllerPos, validator.getStructureName(), formerBlocks));
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    /** @return a read-only view of the members. It is empty while the structure is not valid. */
    @Override
    public Set<BlockPos> getStructureBlocks() {
        return Collections.unmodifiableSet(structureBlocks);
    }

    /** @return {@code true} if {@code pos} is a member, the controller included */
    public boolean isPartOfStructure(BlockPos pos) {
        return structureBlocks.contains(pos);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    /** @return the game time of the last formation, in ticks. It is 0 before the first formation. */
    @Override
    public long getFormationTime() {
        return formationTime;
    }

    /** @return a read-only view of the structure data */
    @Override
    public Map<String, Object> getStructureData() {
        return Collections.unmodifiableMap(structureData);
    }

    /**
     * Gets the redstone port positions from the structure data key {@code "redstonePortPositions"}.
     *
     * <p>Only the Fluid Reactor validator sets this key. The method returns a
     * read-only view of the stored set (0.1.7.36).
     *
     * @return the redstone port positions, or an empty set
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<BlockPos> getRedstonePorts() {
        Object positions = structureData.get("redstonePortPositions");
        if (positions instanceof Set) {
            // Read-only view: a caller cannot change the structure data (0.1.7.36).
            return Collections.unmodifiableSet((Set<BlockPos>) positions);
        }
        return Collections.emptySet();
    }

    /**
     * Adds or replaces one structure data value.
     *
     * <p>{@link #invalidateStructure} removes all values.
     */
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

    /**
     * Finds the formed builtin structure that contains {@code pos}.
     *
     * @return the manager, or {@code null} if no structure contains {@code pos}
     *         or if the structure is not a builtin manager
     */
    public static mio_icif_multiblock_manager<?> getStructureAt(Level level, BlockPos pos) {
        IMultiblockStructure structure = ActiveMultiblocks.getStructureAt(level, pos);
        return structure instanceof mio_icif_multiblock_manager<?> manager ? manager : null;
    }

    /**
     * Finds the formed builtin structure with its controller at {@code controllerPos}.
     *
     * @return the manager, or {@code null}
     */
    public static mio_icif_multiblock_manager<?> getStructureByController(Level level, BlockPos controllerPos) {
        IMultiblockStructure structure = ActiveMultiblocks.getStructureByController(level, controllerPos);
        return structure instanceof mio_icif_multiblock_manager<?> manager ? manager : null;
    }

    /** @return a new list of all formed builtin structures in {@code level} */
    public static Collection<mio_icif_multiblock_manager<?>> getAllStructures(Level level) {
        var managers = new ArrayList<mio_icif_multiblock_manager<?>>();
        for (IMultiblockStructure structure : ActiveMultiblocks.getAllStructures(level)) {
            if (structure instanceof mio_icif_multiblock_manager<?> manager) {
                managers.add(manager);
            }
        }
        return managers;
    }

    /** @return {@code true} if a formed builtin structure has its controller at {@code pos} */
    public static boolean isControllerAt(Level level, BlockPos pos) {
        return getStructureByController(level, pos) != null;
    }

    /** @return {@code true} if any formed structure contains {@code pos}, addon structures included */
    public static boolean isPartOfAnyStructure(Level level, BlockPos pos) {
        return ActiveMultiblocks.isPartOfAnyStructure(level, pos);
    }

    /**
     * Updates the structures after a block change at {@code changedPos}.
     *
     * <p>The multiblock blocks call this method from {@code onPlace} and {@code onRemove}
     * on the server. The new block state is already in the world.
     *
     * <ol>
     *   <li>If no builtin structure is formed in {@code level}, try to form one near
     *       {@code changedPos}.</li>
     *   <li>Else run {@link #validateStructureFull} for each structure with its
     *       controller within 2 blocks of {@code changedPos} on each axis.</li>
     *   <li>If no structure was in that range, try to form one near {@code changedPos}.</li>
     * </ol>
     *
     * <p>Limit: the range test uses 2 blocks for all structure types. The Oil Rig and
     * the Dimension Oil Rig reach farther than 2 blocks. A change at those far
     * positions does not run the full check. Also, a change near a formed structure
     * does not try to form a different structure.
     *
     * @param level the server world
     * @param changedPos the position of the changed block
     */
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
                    // The full check already broke the structure. No more action is necessary.
                }
            }
        }

        if (!affectedExistingStructure) {
            tryFormStructureAt(level, changedPos);
        }
    }

    /**
     * Tries to form a structure at {@code pos} and then in the 5x5x5 box around it.
     *
     * <p>The box covers offsets -2 to +2 on each axis. The search skips positions in
     * unloaded chunks and controllers that already have a valid structure. The search
     * stops at the first structure that forms. Thus one block change forms at most
     * one structure from the box. Only registered controller blocks can form here.
     */
    private static void tryFormStructureAt(Level level, BlockPos pos) {
        tryFormControllerAt(level, pos);

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    // The center was already tried before the loop.
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

    /**
     * Tries to form a structure with its controller at {@code pos}.
     *
     * <p>The validator comes from the {@code MultiblockControllers} registry. The
     * registry matches the exact class of the block at {@code pos}. If the block is
     * not a registered controller, this method returns {@code false} and does
     * nothing. The new manager wraps the registry validator in a
     * {@link CoreValidatorAdapter}.
     *
     * @return {@code true} if a structure formed
     */
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

    /**
     * Tells if {@code pos} is within 2 blocks of {@code center} on each axis.
     *
     * <p>This is a 5x5x5 box test. It matches the GESU, Large Fabricator and Fluid
     * Reactor shapes. It is too small for the two Oil Rig shapes.
     */
    private static boolean isWithinStructureRange(BlockPos center, BlockPos pos) {
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= 2 && dy <= 2 && dz <= 2;
    }
}