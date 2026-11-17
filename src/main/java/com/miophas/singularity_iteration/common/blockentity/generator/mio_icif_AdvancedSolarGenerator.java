package com.miophas.singularity_iteration.common.blockentity.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The advanced solar panels (advanced, hybrid, ultimate hybrid, quantum). 0.1.11.14: only their numbers; the panel itself
 * is {@link mio_icif_solar_generator}. The night power is a night core: it runs at night while the panel sees the sky.
 * Four charge slots.
 */
@SuppressWarnings("null")
public class mio_icif_AdvancedSolarGenerator extends mio_icif_solar_generator {

    protected final int tier;

    public mio_icif_AdvancedSolarGenerator(BlockPos pos, BlockState state, int dayPower, int nightPower, long capacity, int tier) {
        this(pos, state, dayPower, nightPower, capacity, tier, null);
    }

    public mio_icif_AdvancedSolarGenerator(BlockPos pos, BlockState state, int dayPower, int nightPower, long capacity, int tier,
                                           BlockEntityType<?> type) {
        super(pos, state, type, tierOf(tier), dayPower, nightPower, capacity, tierOf(tier).powerRating, 4, 4, false);
        this.tier = tier;
    }

    public int getTier() { return tier; }
    public int getNightPower() { return corePower; }
}
