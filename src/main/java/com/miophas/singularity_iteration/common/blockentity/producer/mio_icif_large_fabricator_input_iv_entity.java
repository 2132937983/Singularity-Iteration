package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * An energy input module of the Large Fabricator. Like the IC2 Mass Fabricator ({@code TileEntityMatter}:
 * demand = capacity - energy) it takes any amount per tick up to the room in its buffer; the IV tier
 * only limits the packet voltage. 0.1.10.14: until then it took at most 8,192 EU/t and published no
 * Forge Energy, so Mekanism and other FE cables could not feed it. The core pools every module's buffer.
 */
@SuppressWarnings("null")
public class mio_icif_large_fabricator_input_iv_entity extends MultiblockEnergyPart {

    private static final long ENERGY_CAPACITY = 800000L;
    private static final ICableTier IV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv");
    /** No rate of its own: a tick can fill the whole buffer. */
    private static final long INPUT_RATE = ENERGY_CAPACITY;

    public mio_icif_large_fabricator_input_iv_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.LARGE_FABRICATOR_INPUT_IV.get(), ENERGY_CAPACITY, INPUT_RATE, 0, IV_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_large_fabricator_input_iv_entity blockEntity) {
        if (level.isClientSide()) return;
    }

    public long getEnergyCapacity() {
        return ENERGY_CAPACITY;
    }

    /** Forge Energy (and Mekanism) input on every face, through the same per-tick ledger as EU. */
    @Override
    protected boolean supportsFeCapability() {
        return true;
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