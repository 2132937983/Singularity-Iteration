package com.miophas.singularity_iteration.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Positional temperature includes altitude and the biome's temperature modifier. */
@Mixin(Biome.class)
public interface CropBiomeAccessor {
    @Invoker("getTemperature") float mio_icif$cropTemperature(BlockPos pos);
}
