// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Independent catalog implementation of the SI eating plant. */
public class PlantEatingPlant extends BuiltinCrop {
    private static final PlantStats STATS = new PlantStats(6, 1, 1, 3, 1, 4);

    @Override public String getTypeId() { return "eatingplant"; }
    @Override public String getModId() { return "mio_icif"; }
    @Override public String getFoundBy() { return "Hasudako"; }
    @Override public String[] getTraits() { return new String[]{ "Bad", "Food" }; }
    @Override public PlantStats getStats() { return STATS; }
    @Override public int getMaxGrowthStage() { return 6; }

    @Override
    public String getTexture(int stage) {
        int frame = Math.max(1, Math.min(6, stage));
        return "mio_icif:block/crop/eatingplant_" + frame;
    }
}
