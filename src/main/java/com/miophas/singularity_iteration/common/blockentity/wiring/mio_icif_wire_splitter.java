package com.miophas.singularity_iteration.common.blockentity.wiring;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileLoadEvent;
import com.miophas.singularity_iteration.core.api.energy.grid.EnergyTileUnloadEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

@SuppressWarnings("null")
public class mio_icif_wire_splitter extends mio_icif_wire {

    public static final CableTier CABLE_TIER = CableTier.IV;
    public static final double CONDUCTION_LOSS = 0.5D;

    private boolean addedToEnergyNet = false;

    public mio_icif_wire_splitter(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.WIRE_SPLITTER.get(), CABLE_TIER, false,
              CABLE_TIER.conductorBreakdownEnergy,
              CABLE_TIER.insulationBreakdownEnergy,
              CABLE_TIER.insulationEnergyAbsorption,
              CONDUCTION_LOSS,
              0.0f);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wire_splitter blockEntity) {
        if (level.isClientSide) return;

        mio_icif_wire.tick(level, pos, state, blockEntity);

        boolean hasRedstoneInput = level.hasNeighborSignal(pos);

        if (hasRedstoneInput == blockEntity.addedToEnergyNet) {
            if (blockEntity.addedToEnergyNet) {
                NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(blockEntity, level));
                blockEntity.addedToEnergyNet = false;
                blockEntity.registered = false;
            } else {
                NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(blockEntity, level));
                blockEntity.addedToEnergyNet = true;
                blockEntity.registered = true;
            }
        }

        boolean isActive = blockEntity.addedToEnergyNet;
        boolean currentActive = state.getValue(com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_wire_splitter.ACTIVE);
        if (isActive != currentActive) {
            level.setBlockAndUpdate(pos, state.setValue(com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_wire_splitter.ACTIVE, isActive));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            if (level.hasNeighborSignal(worldPosition)) {
                if (registered) {
                    NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
                    registered = false;
                }
                addedToEnergyNet = false;
            } else {
                addedToEnergyNet = true;
                registered = true;
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && addedToEnergyNet) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            addedToEnergyNet = false;
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            if (!level.hasNeighborSignal(worldPosition) && !addedToEnergyNet) {
                NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
                addedToEnergyNet = true;
                registered = true;
            }
        }
    }

    public boolean isAddedToEnergyNet() {
        return addedToEnergyNet;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("AddedToEnergyNet", addedToEnergyNet);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        addedToEnergyNet = tag.getBoolean("AddedToEnergyNet");
    }
}