package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

@SuppressWarnings("null")
public class PlantBeetroot extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "beetroot";
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
        return new String[]{ "Red", "Food", "Beetroot" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 4, 0, 1, 2);
    }

    @Override
    public int getMaxGrowthStage() {
        return 3;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/beetroots_" + stage;
    }
}