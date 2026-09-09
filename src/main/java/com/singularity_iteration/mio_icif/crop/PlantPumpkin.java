package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 南瓜植物
 */
@SuppressWarnings("null")
public class PlantPumpkin extends PlantType {

    @Override
    public String getTypeId() {
        return "pumpkin";
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
        return new String[]{"Orange", "Food", "Pumpkin"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 0, 3, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 4;
    }

    @Override
    public int getHarvestStage() {
        return 4;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 5;
        int seedCount = 1 + (int)(Math.random() * 3);
        return new ItemStack[]{
                new ItemStack(Items.PUMPKIN, count),
                new ItemStack(Items.PUMPKIN_SEEDS, seedCount)
        };
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/pumpkin_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}

