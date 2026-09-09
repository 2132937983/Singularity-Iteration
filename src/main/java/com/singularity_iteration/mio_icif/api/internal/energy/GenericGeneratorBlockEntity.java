package com.singularity_iteration.mio_icif.api.internal.energy;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GenericGeneratorBlockEntity extends mio_icif_Energy_Generator {

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