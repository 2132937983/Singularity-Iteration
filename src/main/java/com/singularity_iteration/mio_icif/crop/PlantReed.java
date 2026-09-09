package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 芦苇植物
 */
@SuppressWarnings("null")
public class PlantReed extends PlantType {

    @Override
    public String getTypeId() {
        return "reed";
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
        return new String[]{"Brown", "Sugar", "Reed"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 3, 1, 0, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 3;
    }

    @Override
    public int getHarvestStage() {
        return 3;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 3;
        return new ItemStack[]{new ItemStack(Items.SUGAR_CANE, count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/reed_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }
}

