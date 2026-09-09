package com.singularity_iteration.mio_icif.Blocks.OilRig;

import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_core_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_dimension_oil_rig_core_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public abstract class mio_icif_Block_OilRig_Multiblock_Base extends mio_icif_entity_block {

    private static final int[][] MODULE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 0}, {0, 2, 0}
    };

    protected mio_icif_Block_OilRig_Multiblock_Base(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockEnergyPart part) {
                BlockPos corePos = part.getCorePosition();
                if (corePos != null) {
                    BlockEntity coreBe = level.getBlockEntity(corePos);
                    if (coreBe instanceof mio_icif_oil_rig_core_entity core) {
                        core.onMultiblockBroken();
                        detachAllPartsFromCore(level, corePos);
                    } else if (coreBe instanceof mio_icif_dimension_oil_rig_core_entity core) {
                        core.onMultiblockBroken();
                        detachAllPartsFromCore(level, corePos);
                    }
                }
                part.detachFromCore();
            } else if (be instanceof mio_icif_oil_rig_core_entity core) {
                core.onMultiblockBroken();
                detachAllPartsFromCore(level, pos);
            } else if (be instanceof mio_icif_dimension_oil_rig_core_entity core) {
                core.onMultiblockBroken();
                detachAllPartsFromCore(level, pos);
            }
            mio_icif_multiblock_manager.notifyBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void detachAllPartsFromCore(Level level, BlockPos corePos) {
        for (int[] offset : MODULE_OFFSETS) {
            BlockPos partPos = corePos.offset(offset[0], offset[1], offset[2]);
            BlockEntity partBe = level.getBlockEntity(partPos);
            if (partBe instanceof MultiblockEnergyPart part) {
                part.detachFromCore();
            }
        }
    }
}