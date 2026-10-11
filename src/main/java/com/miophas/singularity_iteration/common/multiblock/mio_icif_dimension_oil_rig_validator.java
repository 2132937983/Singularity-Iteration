package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_base;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_input;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_output;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_panel;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_titanium_drill_frame;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_dimension_oil_rig_core;
import com.miophas.singularity_iteration.common.blockentity.oilrig.mio_icif_dimension_oil_rig_core_entity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Shape check for the Dimension Oil Rig multiblock.
 *
 * <p>The Dimension Oil Rig has 51 blocks in a 7x8x7 box: the core, 26 modules and
 * 24 frame blocks. The box covers X and Z offsets -3 to +3 and Y offsets -1 to +6.
 * It contains the full Oil Rig shape and adds blocks to it.
 *
 * <p>Layout per layer. X goes to the right. Z goes down (north is at the top).
 * {@code C} is the core at {@code (0, 0, 0)}. {@code M} is a module position.
 * {@code F} is a frame position. The validator does not check the positions marked {@code .}.
 * Each sketch shows X and Z from -3 to +3.
 * <pre>
 *   Y = 6           Y = 5           Y = 3 and 4
 *   . . . . . . .   . . . . . . .   . . . . . . .
 *   . . . . . . .   . . . . . . .   . . . . . . .
 *   . . . . . . .   . . . F . . .   . . . . . . .
 *   . . . F . . .   . . F F F . .   . . . F . . .
 *   . . . . . . .   . . . F . . .   . . . . . . .
 *   . . . . . . .   . . . . . . .   . . . . . . .
 *   . . . . . . .   . . . . . . .   . . . . . . .
 *
 *   Y = 2           Y = 1           Y = 0           Y = -1
 *   . . . . . . .   . . . . . . .   . . . F . . .   . . . . . . .
 *   . . . . . . .   . . . F . . .   . . . . . . .   . M M M M M .
 *   . . . F . . .   . . . F . . .   . . M M M . .   . M . . . M .
 *   . . F M F . .   . F F M F F .   F . M C M . F   . M . . . M .
 *   . . . F . . .   . . . F . . .   . . M M M . .   . M . . . M .
 *   . . . . . . .   . . . F . . .   . . . . . . .   . M M M M M .
 *   . . . . . . .   . . . . . . .   . . . F . . .   . . . . . . .
 * </pre>
 * <p>At Y = -1 the 16 modules form the edge of a 5x5 square. The shape has no facing.
 *
 * <p>Module types. Each module position accepts each module type:
 * <ul>
 *   <li>{@code mio_icif_block_oil_rig_input}: energy input module. At least 1 is necessary.</li>
 *   <li>{@code mio_icif_block_oil_rig_output}: output module for the drill product.
 *       At least 1 is necessary.</li>
 *   <li>{@code mio_icif_block_oil_rig_panel}: panel module. No count rule.</li>
 *   <li>{@code mio_icif_block_oil_rig_base}: base module. No count rule.</li>
 * </ul>
 * <p>Each frame position must hold a {@code mio_icif_block_titanium_drill_frame}.
 *
 * <p>Structure data keys: {@code "inputCount"}, {@code "outputCount"},
 * {@code "panelCount"} and {@code "baseCount"} (all {@code Integer}).
 *
 * <p>This structure is not in the {@code MultiblockControllers} registry.
 * {@code mio_icif_dimension_oil_rig_core_entity} forms it from its tick.
 */
@SuppressWarnings("null")
public class mio_icif_dimension_oil_rig_validator implements mio_icif_multiblock_validator {


    /**
     * Module positions relative to the core.
     * <ul>
     *   <li>Row 1 and 2: the 8 blocks around the core at Y = 0, as in the Oil Rig.</li>
     *   <li>Row 3: the 2 blocks above the core at Y = +1 and Y = +2, as in the Oil Rig.</li>
     *   <li>Rows 4 to 7: the 16 edge blocks of a 5x5 square at Y = -1.
     *       The center 3x3 of that layer is not checked.</li>
     * </ul>
     */
    private static final int[][] MODULE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 0}, {0, 2, 0},
        {2, -1, 2}, {2, -1, 1}, {2, -1, 0}, {2, -1, -1}, {2, -1, -2},
        {-2, -1, 2}, {-2, -1, 1}, {-2, -1, 0}, {-2, -1, -1}, {-2, -1, -2},
        {1, -1, 2}, {0, -1, 2}, {-1, -1, 2},
        {1, -1, -2}, {0, -1, -2}, {-1, -1, -2}
    };

    /**
     * Frame positions relative to the core.
     * <ul>
     *   <li>Row 1: a column at Y = +3 to +6 above the core.</li>
     *   <li>Row 2: a cross of 4 blocks around the column at Y = +5.</li>
     *   <li>Row 3: 4 blocks at Y = 0, 3 blocks out from the core on each axis.</li>
     *   <li>Row 4: 4 blocks at Y = +1, 2 blocks out from the core on each axis.</li>
     *   <li>Rows 5 and 6: 4 legs of 2 blocks at Y = +1 and +2 on the 4 sides of the
     *       module column, as in the Oil Rig.</li>
     * </ul>
     */
    private static final int[][] SCAFFOLD_OFFSETS = {
        {0, 3, 0}, {0, 4, 0}, {0, 5, 0}, {0, 6, 0},
        {1, 5, 0}, {-1, 5, 0}, {0, 5, 1}, {0, 5, -1},
        {3, 0, 0}, {-3, 0, 0}, {0, 0, 3}, {0, 0, -3},
        {2, 1, 0}, {-2, 1, 0}, {0, 1, 2}, {0, 1, -2},
        {1, 1, 0}, {1, 2, 0}, {-1, 1, 0}, {-1, 2, 0},
        {0, 1, 1}, {0, 2, 1}, {0, 1, -1}, {0, 2, -1}
    };

    /**
     * Checks the core, the 26 module positions, the 24 frame positions and the
     * module counts.
     *
     * <p>The core must be a Dimension Oil Rig core. The check fails at the first
     * position that does not hold the correct block type. The count rules run only
     * after all positions are correct. The check reads 51 block states.
     * It does not check that the chunks are loaded.
     */
    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        BlockState controllerState = level.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock();

        if (!(controllerBlock instanceof mio_icif_block_dimension_oil_rig_core)) {
            return mio_icif_multiblock_validation_result.failure("Center must be Dimension Oil Rig Core");
        }

        Set<BlockPos> structureBlocks = new HashSet<>();
        structureBlocks.add(controllerPos);

        int inputCount = 0;
        int outputCount = 0;
        int panelCount = 0;
        int baseCount = 0;

        for (int[] offset : MODULE_OFFSETS) {
            BlockPos modulePos = controllerPos.offset(offset[0], offset[1], offset[2]);
            BlockState moduleState = level.getBlockState(modulePos);
            Block moduleBlock = moduleState.getBlock();

            if (moduleBlock instanceof mio_icif_block_oil_rig_input) {
                inputCount++;
                structureBlocks.add(modulePos);
            } else if (moduleBlock instanceof mio_icif_block_oil_rig_output) {
                outputCount++;
                structureBlocks.add(modulePos);
            } else if (moduleBlock instanceof mio_icif_block_oil_rig_panel) {
                panelCount++;
                structureBlocks.add(modulePos);
            } else if (moduleBlock instanceof mio_icif_block_oil_rig_base) {
                baseCount++;
                structureBlocks.add(modulePos);
            } else {
                return mio_icif_multiblock_validation_result.failure(
                        "Missing module at offset [" + offset[0] + "," + offset[1] + "," + offset[2] + "]");
            }
        }

        for (int[] offset : SCAFFOLD_OFFSETS) {
            BlockPos scaffoldPos = controllerPos.offset(offset[0], offset[1], offset[2]);
            BlockState scaffoldState = level.getBlockState(scaffoldPos);
            if (!(scaffoldState.getBlock() instanceof mio_icif_block_titanium_drill_frame)) {
                return mio_icif_multiblock_validation_result.failure(
                        "Missing titanium frame at offset [" + offset[0] + "," + offset[1] + "," + offset[2] + "]");
            }
            structureBlocks.add(scaffoldPos);
        }

        if (inputCount < 1) {
            return mio_icif_multiblock_validation_result.failure("At least 1 input module required");
        }
        if (outputCount < 1) {
            return mio_icif_multiblock_validation_result.failure("At least 1 output module required");
        }

        return mio_icif_multiblock_validation_result.builder()
                .setValid(true)
                .addBlocks(structureBlocks)
                .setStructureData("inputCount", inputCount)
                .setStructureData("outputCount", outputCount)
                .setStructureData("panelCount", panelCount)
                .setStructureData("baseCount", baseCount)
                .build();
    }

    /**
     * Connects the core block entity and the module block entities to the structure.
     *
     * <p>The frame blocks have no block entity. This method does not touch them.
     */
    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_dimension_oil_rig_core_entity coreEntity) {
            coreEntity.onMultiblockFormed(manager);
        }

        for (int[] offset : MODULE_OFFSETS) {
            BlockPos partPos = controllerPos.offset(offset[0], offset[1], offset[2]);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof MultiblockEnergyPart part) {
                part.setCorePosition(controllerPos);
                part.setStructureCompleted(true);
            }
        }
    }

    /**
     * Resets the core block entity and detaches each of the 26 modules that is
     * still present.
     */
    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_dimension_oil_rig_core_entity coreEntity) {
            coreEntity.onMultiblockBroken();
        }

        for (int[] offset : MODULE_OFFSETS) {
            BlockPos partPos = controllerPos.offset(offset[0], offset[1], offset[2]);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof MultiblockEnergyPart part) {
                part.detachFromCore();
            }
        }
    }

    @Override
    public String getStructureName() {
        return "DimensionOilRig";
    }

    /**
     * Tells if a block can be a member of a Dimension Oil Rig.
     *
     * @param block the block to test
     * @return {@code true} for the Dimension Oil Rig core, the 4 module types and
     *         the titanium frame. The plain Oil Rig core gives {@code false}.
     */
    public static boolean isValidStructureBlock(Block block) {
        return block instanceof mio_icif_block_dimension_oil_rig_core
                || block instanceof mio_icif_block_oil_rig_input
                || block instanceof mio_icif_block_oil_rig_output
                || block instanceof mio_icif_block_oil_rig_panel
                || block instanceof mio_icif_block_oil_rig_base
                || block instanceof mio_icif_block_titanium_drill_frame;
    }
}