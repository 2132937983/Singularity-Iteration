package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("null")
public class PlantBaseSapling extends PlantType {

    protected final String cropName;
    protected final String[] traits;
    protected final ItemStack cropDrop;
    protected final ItemStack saplingDrop;
    protected final ItemStack logDrop; // 次要作物：原木
    protected final boolean isOak;

    public PlantBaseSapling(String cropName, String[] traits, ItemStack cropDrop, ItemStack saplingDrop, boolean isOak) {
        this(cropName, traits, cropDrop, saplingDrop, null, isOak);
    }

    public PlantBaseSapling(String cropName, String[] traits, ItemStack cropDrop, ItemStack saplingDrop, ItemStack logDrop, boolean isOak) {
        this.cropName = cropName;
        this.traits = traits;
        this.cropDrop = cropDrop;
        this.saplingDrop = saplingDrop;
        this.logDrop = logDrop;
        this.isOak = isOak;
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
        return "Speiger";
    }

    @Override
    public String[] getTraits() {
        return traits;
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 1, 0, 4, 4, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public int getHarvestStage() {
        return 5;
    }

    @Override
    public int getOptimalHarvestStage() {
        return 5;
    }

    @Override
    public int getStageAfterHarvest() {
        return 4;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        drops.add(cropDrop.copy());
        if (Math.random() >= 0.75) {
            drops.add(saplingDrop.copy());
        }
        // 次要作物：原木（25%概率掉落）
        if (logDrop != null && Math.random() >= 0.75) {
            drops.add(logDrop.copy());
        }
        if (isOak && Math.random() >= 0.75) {
            drops.add(new ItemStack(Items.APPLE, 1));
        }
        return drops.toArray(new ItemStack[0]);
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        if (planter.getGrowthStage() >= 4) return 150;
        return 600;
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
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}