package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 南瓜植物
 */
@SuppressWarnings("null")
public class PlantPumpkin extends BuiltinCrop {

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
        return new String[]{ "Orange", "Decoration", "Stem" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 1, 0, 3, 1);
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
        return "mio_icif:block/crop/pumpkin_" + stage;
    }
}

