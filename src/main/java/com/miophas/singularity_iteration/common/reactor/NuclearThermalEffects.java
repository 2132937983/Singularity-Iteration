package com.miophas.singularity_iteration.common.reactor;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared heat conversion for nuclear pressure damage and deferred terrain cleanup. */
public final class NuclearThermalEffects {
    private NuclearThermalEffects() { }

    public static boolean frozen(BlockState state) {
        return state.is(BlockTags.ICE) || state.getBlock() instanceof IceBlock
            || state.is(BlockTags.SNOW) || state.getBlock() instanceof SnowLayerBlock
            || state.is(Blocks.POWDER_SNOW);
    }

    /** Thin snow disappears; full snow/ice melts to water, which evaporates in ultra-warm dimensions. */
    public static BlockState melted(Level level,BlockState state) {
        if(!frozen(state))throw new IllegalArgumentException("Block is not snow or ice");
        return state.getBlock() instanceof SnowLayerBlock || level.dimensionType().ultraWarm()
            ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
    }
}
