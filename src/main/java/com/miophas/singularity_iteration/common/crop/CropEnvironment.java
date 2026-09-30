// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Modern biome tags replacing IC2's BiomeDictionary categories. */
public final class CropEnvironment {
    private CropEnvironment() {}
    public static boolean has(Holder<Biome> biome, String tag) {
        return biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(tag)));
    }
    public static int nutrientBonus(Holder<Biome> biome) {
        // Original starts at zero and takes the maximum, so negative bonuses have no effect.
        if (has(biome, "minecraft:is_jungle") || has(biome, "c:is_swamp")) return 10;
        if (has(biome, "minecraft:is_forest") || has(biome, "c:is_mushroom")) return 5;
        if (has(biome, "minecraft:is_river")) return 2;
        return 0;
    }
}
