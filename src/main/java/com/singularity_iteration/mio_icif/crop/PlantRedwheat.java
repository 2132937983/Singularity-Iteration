package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 红小麦 - 特殊小麦变种
 */
@SuppressWarnings("null")
public class PlantRedwheat extends PlantType {

    @Override
    public String getTypeId() {
        return "redwheat";
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy() {
        return "Notch";
    }

    @Override
    public String[] getTraits() {
        return new String[]{"Red", "Food", "Wheat"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 0, 4, 0, 2, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 7;
    }

    @Override
    public int getHarvestStage() {
        return 6;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 4;
        if (Math.random() < 0.5) {
            return new ItemStack[]{new ItemStack(Items.REDSTONE, count)};
        }
        return new ItemStack[]{new ItemStack(Items.WHEAT, count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/redwheat_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        int light = planter.getLightLevel();
        return planter.getGrowthStage() < getMaxGrowthStage() && light <= 10 && light >= 5;
    }
}