package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 啤酒花植物
 */
@SuppressWarnings("null")
public class PlantHops extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "hops";
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
        return new String[]{ "Green", "Ingredient", "Wheat" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(5, 2, 2, 0, 1, 1);
    }

    @Override
    public int getMaxGrowthStage() {
        return 7;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/hops_" + stage;
    }
}