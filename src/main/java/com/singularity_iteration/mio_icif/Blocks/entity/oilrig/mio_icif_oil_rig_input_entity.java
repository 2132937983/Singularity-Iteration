package com.singularity_iteration.mio_icif.Blocks.entity.oilrig;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_oil_rig_input_entity extends MultiblockEnergyPart {

    private static final long CAPACITY = 50000L;
    private static final long MAX_RECEIVE = 512L;
    private static final ICableTier TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("hv");

    private static final int[][] MODULE_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 0}, {0, 2, 0}
    };

    public mio_icif_oil_rig_input_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.OIL_RIG_INPUT.get(), CAPACITY, MAX_RECEIVE, 0, TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_rig_input_entity blockEntity) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 10 != 0) return;

        BlockPos corePos = blockEntity.getCorePosition();
        if (corePos == null || !blockEntity.isStructureCompleted()) return;

        List<mio_icif_oil_rig_input_entity> others = findOtherInputs(level, corePos, pos);
        for (mio_icif_oil_rig_input_entity other : others) {
            long myEnergy = blockEntity.getStoredEnergy();
            long otherEnergy = other.getStoredEnergy();
            if (myEnergy > otherEnergy + 1) {
                long diff = myEnergy - otherEnergy;
                long transfer = diff / 2;
                if (transfer > 0) {
                    long consumed = blockEntity.consumeEnergy(transfer, false);
                    if (consumed > 0) {
                        other.generateEnergyInternal(consumed, false);
                    }
                }
            }
        }
        blockEntity.setChanged();
    }

    private static List<mio_icif_oil_rig_input_entity> findOtherInputs(Level level, BlockPos corePos, BlockPos selfPos) {
        List<mio_icif_oil_rig_input_entity> result = new ArrayList<>();
        for (int[] offset : MODULE_OFFSETS) {
            BlockPos checkPos = corePos.offset(offset[0], offset[1], offset[2]);
            if (checkPos.equals(selfPos)) continue;
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof mio_icif_oil_rig_input_entity input) {
                result.add(input);
            }
        }
        return result;
    }
}