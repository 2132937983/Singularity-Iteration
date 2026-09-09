package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 甜瓜植物
 */
@SuppressWarnings("null")
public class PlantMelon extends PlantType {

    @Override
    public String getTypeId() {
        return "melon";
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
        return new String[]{"Green", "Food", "Melon"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 0, 2, 0);
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
        if (Math.random() < 0.33) {
            return new ItemStack[]{
                    new ItemStack(Items.MELON, 1),
                    new ItemStack(Items.MELON_SEEDS, 1 + (int)(Math.random() * 2))
            };
        }
        int count = 2 + Math.max(0, yield) / 2;
        return new ItemStack[]{
                new ItemStack(Items.MELON_SLICE, count),
                new ItemStack(Items.MELON_SEEDS, 1 + (int)(Math.random() * 2))
        };
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/melon_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}

