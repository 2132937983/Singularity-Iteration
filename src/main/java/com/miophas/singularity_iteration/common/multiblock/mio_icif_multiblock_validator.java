package com.miophas.singularity_iteration.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Shape check for one type of builtin multiblock structure.
 *
 * <h2>Package overview</h2>
 *
 * <h3>What a multiblock is</h3>
 * <p>A multiblock is a group of blocks that operates as one machine.
 * One block is the <b>controller</b>. The controller holds the machine logic
 * and the main block entity. All other blocks are <b>parts</b>. A part that
 * adds a function to the machine is a <b>module</b>. A part that only completes
 * the shape is a <b>frame block</b>.
 *
 * <h3>Types in this package</h3>
 * <ul>
 *   <li>{@code mio_icif_multiblock_validator} (this interface): it reads the
 *       world and decides if the shape around one controller is complete.</li>
 *   <li>{@link mio_icif_multiblock_validation_result}: the immutable result of
 *       one check. It holds the valid flag, the error message, the set of member
 *       positions and a map of structure data (module counts and port positions).</li>
 *   <li>{@link mio_icif_multiblock_manager}: the live state of one formed structure.
 *       It stores the result data, registers the structure in
 *       {@code ActiveMultiblocks}, sends the formed and broken events and runs
 *       the revalidation.</li>
 *   <li>{@link BuiltinMultiblockControllers}, {@link ToCoreValidatorAdapter} and
 *       {@link CoreValidatorAdapter}: they connect the builtin validators to the
 *       core registry {@code MultiblockControllers}.</li>
 * </ul>
 *
 * <h3>Builtin structures</h3>
 * <ul>
 *   <li>GESU ({@link mio_icif_gesu_validator}): a 3x3x3 cross of 7 blocks.</li>
 *   <li>Large Fabricator ({@link mio_icif_large_fabricator_validator}):
 *       a 3x3x3 cross of 7 blocks.</li>
 *   <li>Oil Rig ({@link mio_icif_oil_rig_validator}): 22 blocks in a 3x6x3 box
 *       (Y offset 0 to 5).</li>
 *   <li>Dimension Oil Rig ({@link mio_icif_dimension_oil_rig_validator}):
 *       51 blocks in a 7x8x7 box (Y offset -1 to 6).</li>
 *   <li>Fluid Reactor ({@link mio_icif_fluid_reactor_validator}): a 5x5x5 cube.</li>
 * </ul>
 * <p>The Javadoc of each validator gives the exact layout.
 *
 * <h3>Coordinate frame</h3>
 * <p>All shapes use the controller position as the origin {@code (0, 0, 0)}.
 * All offsets are world-axis offsets {@code (dx, dy, dz)}. Positive X is east.
 * Positive Y is up. Positive Z is south. The validators do not read the facing
 * of the controller. The validators do not rotate or mirror a shape. Thus each
 * shape has one fixed orientation in the world. All builtin shapes are
 * symmetric in the horizontal plane, so a player sees no difference.
 *
 * <h3>When validation runs</h3>
 * <ul>
 *   <li><b>Placement and removal.</b> The multiblock block classes call
 *       {@link mio_icif_multiblock_manager#notifyBlockChanged} from
 *       {@code onPlace} and {@code onRemove} on the server. If the changed position
 *       is within 2 blocks (on each axis) of a formed controller, the manager runs
 *       {@link mio_icif_multiblock_manager#validateStructureFull}. If no formed
 *       structure is near, the manager searches a 5x5x5 box around the changed
 *       position for a registered controller and tries to form it.</li>
 *   <li><b>Periodic check.</b> The controller block entity calls
 *       {@link mio_icif_multiblock_manager#validateStructure} on a fixed interval
 *       (GESU: 20 ticks, Large Fabricator: 15 ticks, Oil Rig: 25 ticks, Dimension
 *       Oil Rig: 30 ticks). This quick check only looks for air
 *       at the controller and at its 6 face neighbors. The Fluid Reactor runs
 *       its validator directly once per 20-tick cycle.</li>
 *   <li><b>After world load.</b> {@code ActiveMultiblocks} is not saved. The
 *       GESU, Large Fabricator and Oil Rig controller block entities set a flag
 *       on load. On its first server tick it
 *       calls {@link mio_icif_multiblock_manager#tryForm} again.</li>
 * </ul>
 *
 * <h3>Result flow</h3>
 * <p>The manager copies the member positions and the structure data of a valid
 * result. Then it registers the structure, calls {@link #onStructureFormed} and
 * posts a {@code MultiblockFormedEvent}. The controller block entity reads the
 * structure data (for example {@code "inputModuleCount"}) in its
 * {@code onMultiblockFormed} method. Other systems find the structure through
 * {@code ActiveMultiblocks} or through the static lookups of the manager.
 *
 * <h3>When a structure breaks</h3>
 * <p>A failed revalidation calls {@link mio_icif_multiblock_manager#invalidateStructure}.
 * That method unregisters the structure, calls {@link #onStructureBroken}, posts
 * a {@code MultiblockBrokenEvent} with the former member positions, and then
 * clears the member set and the structure data. The validators use
 * {@link #onStructureBroken} to reset the controller block entity and to detach
 * each part ({@code MultiblockEnergyPart#detachFromCore}). The manager object
 * stays invalid after this. A new manager must form the structure again.
 *
 * <h3>Rules for implementations</h3>
 * <ul>
 *   <li>{@link #validate} must only read the world. It must not place, remove or
 *       change blocks. It must not change block entities.</li>
 *   <li>{@link #validate} must include the controller position in the member set.</li>
 *   <li>Make the instance stateless. The registry makes a new instance for each
 *       attempt, but block entities also keep their own instances.</li>
 *   <li>Call the validators on the server thread only.</li>
 * </ul>
 */
public interface mio_icif_multiblock_validator {

    /**
     * Checks if the blocks around {@code controllerPos} form a complete structure.
     *
     * <p>Precondition: the caller is on the server thread. Only the Fluid Reactor
     * validator checks that the chunks are loaded. The other validators read the
     * block states without a load check. Thus the caller must make sure that the
     * full shape is in loaded chunks, or accept that the read can load a chunk.
     *
     * <p>Postcondition: the world is not changed. A valid result contains the
     * controller position and all member positions.
     *
     * @param level the world
     * @param controllerPos the position of the controller block, which is the origin of the shape
     * @return a valid result with the members and the structure data, or a failure result with an error message
     */
    mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos);

    /**
     * Called one time after the manager registers a newly formed structure.
     *
     * <p>Use this method to connect the controller block entity and the part block
     * entities to the structure. The manager already holds the member set and the
     * structure data when it calls this method.
     *
     * @param level the world
     * @param controllerPos the position of the controller block
     * @param manager the manager of the formed structure
     */
    default void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // Default: no action.
}

    /**
     * Called one time after the manager unregisters a broken structure.
     *
     * <p>Use this method to reset the controller block entity and to detach the parts.
     * The manager still holds the old member set during this call. It clears the
     * member set after this call. A block in the shape can already be missing or
     * replaced, so check the type of each block entity before use.
     *
     * @param level the world
     * @param controllerPos the position of the controller block
     * @param manager the manager of the broken structure
     */
    default void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // Default: no action.
}

    /**
     * Gets the structure name for debugging and logs.
     *
     * <p>The formed and broken events also carry this name.
     *
     * @return the structure name
     */
    default String getStructureName() {
        return getClass().getSimpleName();
    }
}


