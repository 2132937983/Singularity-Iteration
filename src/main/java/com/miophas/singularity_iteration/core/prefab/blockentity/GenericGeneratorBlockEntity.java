package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GenericGeneratorBlockEntity extends AbstractGeneratorBlockEntity {

    public GenericGeneratorBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                       SlotLayout layout, long energyGenerationRate,
                                       long capacity, long maxReceive, long maxExtract,
                                       ICableTier cableTier) {
        super(pos, state, type, layout, energyGenerationRate, capacity, maxReceive, maxExtract,
            CableTier.fromICableTier(cableTier));
    }

    public GenericGeneratorBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                       SlotLayout layout, long energyGenerationRate,
                                       long capacity, long maxReceive, long maxExtract,
                                       CableTier cableTier) {
        super(pos, state, type, layout, energyGenerationRate, capacity, maxReceive, maxExtract, cableTier);
    }

    public GenericGeneratorBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                       SlotLayout layout, long energyGenerationRate) {
        super(pos, state, type, layout, energyGenerationRate);
    }
}