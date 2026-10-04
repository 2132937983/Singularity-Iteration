package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericEnergyContainerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_eesu_entity extends GenericEnergyContainerBlockEntity {

    private static final long DEFAULT_CAPACITY = 400000000L;
    private static final ICableTier IV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv");
    private static final long IV_IO_RATE = IV_TIER.getPowerRating();

    public mio_icif_eesu_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.EESU.get(), DEFAULT_CAPACITY, IV_IO_RATE, IV_IO_RATE, IV_TIER);
    }

    public mio_icif_eesu_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, DEFAULT_CAPACITY, IV_IO_RATE, IV_IO_RATE, IV_TIER);
    }
}