package com.singularity_iteration.mio_icif.multiblock;

import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_base;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_core;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_input;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_output;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_panel;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_titanium_drill_frame;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_dimension_oil_rig_core;
import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_core_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_dimension_oil_rig_core_entity;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@SuppressWarnings("null")
public class mio_icif_oil_rig_validator implements mio_icif_multiblock_validator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[][] MODULE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 0}, {0, 2, 0}
    };

    private static final int[][] SCAFFOLD_OFFSETS = {
        {0, 3, 0}, {0, 4, 0}, {0, 5, 0},
        {1, 1, 0}, {1, 2, 0}, {-1, 1, 0}, {-1, 2, 0},
        {0, 1, 1}, {0, 2, 1}, {0, 1, -1}, {0, 2, -1}
    };

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