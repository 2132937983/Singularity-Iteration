package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GenericEnergyBlockEntity extends AbstractEnergyBlockEntity {

    public GenericEnergyBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                    long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    @Override
    public IEnergyStorageAccess getEnergyStorage() {
        return super.getEnergyStorage();
    }
}