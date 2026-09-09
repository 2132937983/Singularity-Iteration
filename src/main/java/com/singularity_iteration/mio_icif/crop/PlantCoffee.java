package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 咖啡植物
 * IC2: size=4不产出，size=5才产出咖啡豆
 */
@SuppressWarnings("null")
public class PlantCoffee extends PlantType {

    @Override
    public String getTypeId() {
        return "coffee";
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
        return new String[]{"Brown", "Drink", "Coffee"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 1, 2, 0);
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
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int stage = planter.getGrowthStage();
        if (stage < 5) {
            return new ItemStack[0];
        }
        int count = 1 + Math.max(0, yield) / 4;
        return new ItemStack[]{new ItemStack(mio_icif_resources.COFFEE_BEAN.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/coffee_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}

