package com.singularity_iteration.mio_icif.multiblock;

import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_core;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_input_iv;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_output_iv;
import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_gesu_output_luv;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_gesu_core_entity;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

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

@SuppressWarnings("null")
public class mio_icif_gesu_validator implements mio_icif_multiblock_validator {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        LOGGER.info("[GESU] Validating at " + controllerPos);

        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof mio_icif_gesu_core)) {
            return mio_icif_multiblock_validation_result.failure("Center must be GESU Core");
        }

        Set<BlockPos> structureBlocks = new HashSet<>();
        structureBlocks.add(controllerPos);

        int inputModuleCount = 0;
        int outputIvModuleCount = 0;
        int outputLuvModuleCount = 0;

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

        LOGGER.info("[GESU] Valid! Input(IV): " + inputModuleCount
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

    public static boolean isValidStructureBlock(Block block) {
        return block instanceof mio_icif_gesu_core
                || block instanceof mio_icif_gesu_input_iv
                || block instanceof mio_icif_gesu_output_iv
                || block instanceof mio_icif_gesu_output_luv;
    }
}