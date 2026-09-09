package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_large_fabricator_input_iv_entity extends MultiblockEnergyPart {

    private static final long ENERGY_CAPACITY = 800000L;
    private static final ICableTier IV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv");
    private static final long INPUT_RATE = IV_TIER.getPowerRating();

    public mio_icif_large_fabricator_input_iv_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.LARGE_FABRICATOR_INPUT_IV.get(), ENERGY_CAPACITY, INPUT_RATE, 0, IV_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_large_fabricator_input_iv_entity blockEntity) {
        if (level.isClientSide()) return;
    }

    public long getEnergyCapacity() {
        return ENERGY_CAPACITY;
    }

    @Override
    @Nullable
    public BlockEntity getJadeDisplayTarget() {
        return this;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.large_fabricator_input_iv");
    }
}