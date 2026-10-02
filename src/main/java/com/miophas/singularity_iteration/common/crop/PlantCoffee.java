package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 咖啡植物
 * IC2: size=4不产出，size=5才产出咖啡豆
 */
@SuppressWarnings("null")
public class PlantCoffee extends BuiltinCrop {

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
        return new String[]{ "Leaves", "Ingredient", "Beans" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(7, 1, 4, 1, 2, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/coffee_" + stage;
    }
}

