// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Independent catalog implementation of the SI eating plant. */
public class PlantEatingPlant extends PlantType {
    private static final PlantStats STATS = new PlantStats(6, 1, 1, 3, 1, 4);

    @Override public String getTypeId() { return "eatingplant"; }
    @Override public String getModId() { return "mio_icif"; }
    @Override public String getFoundBy() { return "Hasudako"; }
    @Override public String[] getTraits() { return new String[]{"Bad", "Food"}; }
    @Override public PlantStats getStats() { return STATS; }
    @Override public int getMaxGrowthStage() { return 6; }
    @Override public int getHarvestStage() { return 4; }
    @Override public int getOptimalHarvestStage() { return 4; }
    @Override public int getStageAfterHarvest() { return 1; }
    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        return planter != null && planter.getGrowthStage() == getHarvestStage()
            ? new ItemStack[]{new ItemStack(Items.CACTUS)} : new ItemStack[0];
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        int stage = planter == null ? 0 : Math.max(0, Math.min(getMaxGrowthStage(), planter.getGrowthStage()));
        return 74_400 + stage * 14_400;
    }

    @Override
    public String getTexture(int stage) {
        int frame = Math.max(1, Math.min(6, stage));
        return "mio_icif:block/crop/eatingplant_" + frame;
    }

    @Override public boolean canGrow(IPlanter planter) { return super.canGrow(planter); }
    @Override public boolean isHarvestable(IPlanter planter) { return planter != null && planter.getGrowthStage() == getHarvestStage(); }
    @Override public boolean canBeHarvested(IPlanter planter) { return isHarvestable(planter); }
    @Override public double dropGainChance() { return Math.pow(0.95D, STATS.getLevel()); }
    @Override public float dropSeedChance(IPlanter planter) { return 0.13107201F; }
    @Override public int calculateDropCount(IPlanter planter) { return 1; }
    @Override public int getRootDepth(IPlanter planter) { return 5; }
}
