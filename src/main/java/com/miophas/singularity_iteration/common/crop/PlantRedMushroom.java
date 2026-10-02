package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 红蘑菇植物
 */
@SuppressWarnings("null")
public class PlantRedMushroom extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "redMushroom";
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
        return new String[]{"Red", "Food", "Mushroom"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 0, 0, 4);
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
        return "mio_icif:block/crop/red_mushroom_" + stage;
    }
}