package com.singularity_iteration.mio_icif.api.internal.energy;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GenericEnergyBlockEntity extends mio_icif_Energy_Block {

    public GenericEnergyBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                    long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    @Override
    public IEnergyStorageAccess getEnergyStorage() {
        return super.getEnergyStorage();
    }
}