package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class PlantGenericCrop extends com.miophas.singularity_iteration.core.prefab.crop.Ic2PlantType {

    protected final String cropName;
    protected final String discoveredBy;
    protected final String[] attributes;
    protected final PlantStats stats;
    protected final int maxSize;
    protected final int harvestSize;
    protected final int optimalHarvestSize;
    protected final ItemStack[] drops;
    protected final ItemStack[] specialDrops;
    protected final int afterHarvestSize;
    protected final int growthSpeed;

    public PlantGenericCrop(String cropName, String discoveredBy, String[] attributes,
                            PlantStats stats, int maxSize, int harvestSize, int optimalHarvestSize,
                            ItemStack[] drops, ItemStack[] specialDrops,
                            int afterHarvestSize, int growthSpeed) {
        this.cropName = cropName;
        this.discoveredBy = discoveredBy;
        this.attributes = attributes;
        this.stats = stats;
        this.maxSize = maxSize;
        this.harvestSize = harvestSize;
        this.optimalHarvestSize = optimalHarvestSize;
        this.drops = drops;
        this.specialDrops = specialDrops;
        this.afterHarvestSize = afterHarvestSize;
        this.growthSpeed = growthSpeed;
    }

    @Override
    public String getTypeId() {
        return cropName;
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy() {
        return discoveredBy;
    }

    @Override
    public String[] getTraits() {
        return attributes;
    }

    @Override
    public PlantStats getStats() {
        return stats;
    }

    @Override
    public int getMaxGrowthStage() {
        return maxSize;
    }

    @Override
    public int getHarvestStage() {
        return harvestSize;
    }

    @Override
    public int getOptimalHarvestStage() {
        return optimalHarvestSize;
    }

    @Override
    public int getStageAfterHarvest() {
        return afterHarvestSize;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            result.add(drop.copy());
        }
        if (specialDrops != null && specialDrops.length > 0) {
            int roulette = planter == null || planter.getPlanterWorld() == null ? specialDrops.length * 2 + 1
                : planter.getPlanterWorld().random.nextInt(specialDrops.length * 2 + 2);
            if (roulette < specialDrops.length && !specialDrops[roulette].isEmpty()) {
                result.add(specialDrops[roulette].copy());
            }
        }
        return result.toArray(new ItemStack[0]);
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        if (growthSpeed < 200) return stats.getLevel() * 200;
        return stats.getLevel() * growthSpeed;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/" + cropName + "_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return super.canGrow(planter);
    }
    @Override public boolean canBeHarvested(IPlanter planter) { return planter.getGrowthStage() >= harvestSize; }
    @Override public boolean canHybridize(IPlanter planter) { return planter.getGrowthStage() + 2 > maxSize; }
    @Override public int getRootDepth(IPlanter planter) { return 5; }
}
