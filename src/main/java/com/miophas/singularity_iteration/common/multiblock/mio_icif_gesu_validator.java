package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_core;
import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_input_iv;
import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_output_iv;
import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_output_luv;
import com.miophas.singularity_iteration.common.blockentity.batbox.mio_icif_gesu_core_entity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Shape check for the GESU energy storage multiblock.
 *
 * <p>The GESU is a cross of 7 blocks in a 3x3x3 box.
 * <p>Layout (offsets from the core, world axes):
 * <pre>
 *   Y = +1        Y = 0         Y = -1
 *   . . .         . M .         . . .
 *   . M .         M C M         . M .
 *   . . .         . M .         . . .
 * </pre>
 * <p>{@code C} is the core at {@code (0, 0, 0)}. {@code M} is a module. The 6 module
 * positions are the face neighbors of the core: {@code (+-1, 0, 0)},
 * {@code (0, +-1, 0)} and {@code (0, 0, +-1)}. The validator does not check the
 * positions marked {@code .}. Any block can be there. The shape has no facing.
 * Thus each module type can be at any of the 6 positions.
 *
 * <p>Module types:
 * <ul>
 *   <li>{@code mio_icif_gesu_input_iv}: input module. It receives energy.</li>
 *   <li>{@code mio_icif_gesu_output_iv}: IV output module. It sends energy.</li>
 *   <li>{@code mio_icif_gesu_output_luv}: LuV output module. It sends energy.</li>
 * </ul>
 * <p>The core stores the energy. The validator sets no minimum or maximum count
 * for a module type. All 6 faces must hold a module.
 *
 * <p>Structure data keys: {@code "inputModuleCount"}, {@code "outputIvModuleCount"}
 * and {@code "outputLuvModuleCount"} (all {@code Integer}).
 * {@code mio_icif_gesu_core_entity#onMultiblockFormed} reads these keys.
 */
@SuppressWarnings("null")
public class mio_icif_gesu_validator implements mio_icif_multiblock_validator {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Checks the 6 face neighbors of the GESU core.
     *
     * <p>The check fails at the first face that does not hold a GESU module.
     * The check reads 7 block states. It does not check that the chunks are loaded.
     */
    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        LOGGER.debug("[GESU] Validating at " + controllerPos);

        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof mio_icif_gesu_core)) {
            return mio_icif_multiblock_validation_result.failure("Center must be GESU Core");
        }

        Set<BlockPos> structureBlocks = new HashSet<>();
        structureBlocks.add(controllerPos);

        int inputModuleCount = 0;
        int outputIvModuleCount = 0;
        int outputLuvModuleCount = 0;

        // Direction.values() gives the 6 faces: down, up, north, south, west, east.
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = controllerPos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Block adjacentBlock = adjacentState.getBlock();

            if (adjacentBlock instanceof mio_icif_gesu_input_iv) {
                inputModuleCount++;
                structureBlocks.add(adjacentPos);
            } else if (adjacentBlock instanceof mio_icif_gesu_output_iv) {
                outputIvModuleCount++;
                structureBlocks.add(adjacentPos);
            } else if (adjacentBlock instanceof mio_icif_gesu_output_luv) {
                outputLuvModuleCount++;
                structureBlocks.add(adjacentPos);
            } else {
                return mio_icif_multiblock_validation_result.failure(
                        "All 6 faces of GESU Core must have a module (missing: " + direction.getName() + ")");
            }
        }

        LOGGER.debug("[GESU] Valid! Input(IV): " + inputModuleCount
                + ", Output(IV): " + outputIvModuleCount
                + ", Output(LuV): " + outputLuvModuleCount);

        return mio_icif_multiblock_validation_result.builder()
                .setValid(true)
                .addBlocks(structureBlocks)
                .setStructureData("inputModuleCount", inputModuleCount)
                .setStructureData("outputIvModuleCount", outputIvModuleCount)
                .setStructureData("outputLuvModuleCount", outputLuvModuleCount)
                .build();
    }

    /**
     * Connects the core block entity and the 6 module block entities to the structure.
     *
     * <p>Each module that is a {@code MultiblockEnergyPart} gets the core position
     * and the completed flag.
     */
    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_gesu_core_entity coreEntity) {
            coreEntity.onMultiblockFormed(manager);
        }

        for (Direction direction : Direction.values()) {
            BlockPos partPos = controllerPos.relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof MultiblockEnergyPart part) {
                part.setCorePosition(controllerPos);
                part.setStructureCompleted(true);
            }
        }
    }

    /**
     * Resets the core block entity and detaches each module that is still present.
     */
    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_gesu_core_entity coreEntity) {
            coreEntity.onMultiblockBroken();
        }

        for (Direction direction : Direction.values()) {
            BlockPos partPos = controllerPos.relative(direction);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof MultiblockEnergyPart part) {
                part.detachFromCore();
            }
        }
    }

    @Override
    public String getStructureName() {
        return "GESUCross";
    }

    /**
     * Tells if a block can be a member of a GESU.
     *
     * @param block the block to test
     * @return {@code true} for the GESU core and the 3 GESU module types
     */
    public static boolean isValidStructureBlock(Block block) {
        return block instanceof mio_icif_gesu_core
                || block instanceof mio_icif_gesu_input_iv
                || block instanceof mio_icif_gesu_output_iv
                || block instanceof mio_icif_gesu_output_luv;
    }
}