package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_base;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_core;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_input;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_output;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_oil_rig_panel;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_titanium_drill_frame;
import com.miophas.singularity_iteration.common.block.oilrig.mio_icif_block_dimension_oil_rig_core;
import com.miophas.singularity_iteration.common.blockentity.oilrig.mio_icif_oil_rig_core_entity;
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
 * Shape check for the Oil Rig multiblock.
 *
 * <p>The Oil Rig has 22 blocks in a 3x6x3 box: the core, 10 modules and
 * 11 frame blocks. The box covers X and Z offsets -1 to +1 and Y offsets 0 to +5.
 *
 * <p>Layout per layer. X goes to the right. Z goes down (north is at the top).
 * {@code C} is the core at {@code (0, 0, 0)}. {@code M} is a module position.
 * {@code F} is a frame position. The validator does not check the positions marked {@code .}.
 * <pre>
 *   Y = 3 to 5    Y = 1 and 2   Y = 0
 *   . . .         . F .         M M M
 *   . F .         F M F         M C M
 *   . . .         . F .         M M M
 * </pre>
 * <p>The shape has no facing.
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
 * {@code mio_icif_oil_rig_core_entity} forms it from its tick.
 */
@SuppressWarnings("null")
public class mio_icif_oil_rig_validator implements mio_icif_multiblock_validator {


    /**
     * Module positions relative to the core: the 8 blocks around the core at Y = 0,
     * then the 2 blocks above the core at Y = +1 and Y = +2.
     */
    private static final int[][] MODULE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 0}, {0, 2, 0}
    };

    /**
     * Frame positions relative to the core: a column at Y = +3 to +5 above the
     * core, then 4 legs of 2 blocks at Y = +1 and +2 on the 4 sides of the module column.
     */
    private static final int[][] SCAFFOLD_OFFSETS = {
        {0, 3, 0}, {0, 4, 0}, {0, 5, 0},
        {1, 1, 0}, {1, 2, 0}, {-1, 1, 0}, {-1, 2, 0},
        {0, 1, 1}, {0, 2, 1}, {0, 1, -1}, {0, 2, -1}
    };

    /**
     * Checks the core, the 10 module positions, the 11 frame positions and the
     * module counts.
     *
     * <p>The core can be an Oil Rig core or a Dimension Oil Rig core. The check
     * fails at the first position that does not hold the correct block type.
     * The count rules run only after all positions are correct.
     * The check reads 22 block states. It does not check that the chunks are loaded.
     */
    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        BlockState controllerState = level.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock();

        boolean isCore = controllerBlock instanceof mio_icif_block_oil_rig_core
                || controllerBlock instanceof mio_icif_block_dimension_oil_rig_core;
        if (!isCore) {
            return mio_icif_multiblock_validation_result.failure("Center must be Oil Rig Core");
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
        if (be instanceof mio_icif_oil_rig_core_entity coreEntity) {
            coreEntity.onMultiblockFormed(manager);
        } else if (be instanceof mio_icif_dimension_oil_rig_core_entity coreEntity) {
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
     * Resets the core block entity and detaches each module that is still present.
     */
    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_oil_rig_core_entity coreEntity) {
            coreEntity.onMultiblockBroken();
        } else if (be instanceof mio_icif_dimension_oil_rig_core_entity coreEntity) {
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
        return "OilRig";
    }

    /**
     * Tells if a block can be a member of an Oil Rig.
     *
     * @param block the block to test
     * @return {@code true} for both core types, the 4 module types and the titanium frame
     */
    public static boolean isValidStructureBlock(Block block) {
        return block instanceof mio_icif_block_oil_rig_core
                || block instanceof mio_icif_block_dimension_oil_rig_core
                || block instanceof mio_icif_block_oil_rig_input
                || block instanceof mio_icif_block_oil_rig_output
                || block instanceof mio_icif_block_oil_rig_panel
                || block instanceof mio_icif_block_oil_rig_base
                || block instanceof mio_icif_block_titanium_drill_frame;
    }
}