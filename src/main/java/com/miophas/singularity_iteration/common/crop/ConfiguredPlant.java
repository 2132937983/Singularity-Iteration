// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

/** Data-driven independent plant used for legacy catalog entries without a zero-arg class. */
public final class ConfiguredPlant extends PlantType {
    private final String id;
    private final String foundBy;
    private final String[] traits;
    private final PlantStats stats;
    private final int maxStage;
    private final int harvestStage;
    private final int optimalStage;
    private final int afterHarvest;
    private final int growthTime;
    private final ItemStack[] drops;

    public ConfiguredPlant(String id, String foundBy, String[] traits, PlantStats stats,
            int maxStage, int harvestStage, int optimalStage, int afterHarvest,
            int growthTime, ItemStack... drops) {
        this.id = id;
        this.foundBy = foundBy;
        this.traits = traits.clone();
        this.stats = stats;
        this.maxStage = maxStage;
        this.harvestStage = harvestStage;
        this.optimalStage = optimalStage;
        this.afterHarvest = afterHarvest;
        this.growthTime = growthTime;
        this.drops = Arrays.stream(drops).map(ItemStack::copy).toArray(ItemStack[]::new);
    }

    @Override public String getTypeId() { return id; }
    @Override public String getModId() { return "mio_icif"; }
    @Override public String getFoundBy() { return foundBy; }
    @Override public String[] getTraits() { return traits.clone(); }
    @Override public PlantStats getStats() { return stats; }
    @Override public int getMaxGrowthStage() { return maxStage; }
    @Override public int getHarvestStage() { return harvestStage; }
    @Override public int getOptimalHarvestStage() { return optimalStage; }
    @Override public int getStageAfterHarvest() { return afterHarvest; }
    @Override public int getGrowthTime(IPlanter planter) { return growthTime; }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        if (!isHarvestableStage(planter)) return new ItemStack[0];
        return Arrays.stream(drops).map(ItemStack::copy).toArray(ItemStack[]::new);
    }

    private boolean isHarvestableStage(IPlanter planter) {
        return planter != null && planter.getGrowthStage() >= harvestStage;
    }

    @Override public boolean canBeHarvested(IPlanter planter) { return isHarvestableStage(planter) && drops.length > 0; }
    @Override public String getTexture(int stage) {
        int frame = Math.max(1, Math.min(maxStage, stage));
        return "mio_icif:block/crop/" + id + "_" + frame;
    }

    @Override public ItemStack getSeedItem(IPlanter planter) { return ItemStack.EMPTY; }
}
