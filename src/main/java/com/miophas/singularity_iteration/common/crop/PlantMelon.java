package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 甜瓜植物
 */
@SuppressWarnings("null")
public class PlantMelon extends BuiltinCrop {

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
        return new String[]{ "Green", "Food", "Stem" };
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
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/melon_" + stage;
    }
}

