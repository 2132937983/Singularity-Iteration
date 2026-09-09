package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.internal.energy.GenericEnergyContainerBlockEntity;

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

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.eesu");
    }
}