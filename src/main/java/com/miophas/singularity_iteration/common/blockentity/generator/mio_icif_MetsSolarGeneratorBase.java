package com.miophas.singularity_iteration.common.blockentity.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The METS solar generators (advanced, photon resonance, ultimate photon resonance). 0.1.11.14: only their numbers; the
 * panel itself is {@link mio_icif_solar_generator}. Their minimum night brightness is a night core of that share of the
 * day power. One charge slot; the three hidden slots of older saves are emptied.
 */
@SuppressWarnings("null")
public class mio_icif_MetsSolarGeneratorBase extends mio_icif_solar_generator {

    protected final int tier;

    public mio_icif_MetsSolarGeneratorBase(BlockPos pos, BlockState state, int dayPower, long capacity, int tier,
                                           int skyUpdateInterval, float minSkyBrightness, BlockEntityType<?> type, BooleanProperty litProperty) {
        this(pos, state, dayPower, dayPower, capacity, tier, skyUpdateInterval, minSkyBrightness, type, litProperty);
    }

    /** {@code production} was a second figure some subclasses passed; the day power is what they make. */
    public mio_icif_MetsSolarGeneratorBase(BlockPos pos, BlockState state, int dayPower, int production, long capacity, int tier,
                                           int skyUpdateInterval, float minSkyBrightness, BlockEntityType<?> type, BooleanProperty litProperty) {
        super(pos, state, type, tierOf(tier), dayPower, Math.round(dayPower * minSkyBrightness), capacity, tierOf(tier).powerRating, 4, 1, false);
        this.tier = tier;
    }

    public int getTier() { return tier; }
}
