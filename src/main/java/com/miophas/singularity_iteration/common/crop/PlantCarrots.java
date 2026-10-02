package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 胡萝卜植物
 */
@SuppressWarnings("null")
public class PlantCarrots extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "carrots";
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
        return new String[]{ "Orange", "Food", "Carrots" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 0, 0, 2);
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
        return "mio_icif:block/crop/carrots_" + stage;
    }
}