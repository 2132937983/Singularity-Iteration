package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 黑刺李植 - 产出墨囊
 */
@SuppressWarnings("null")
public class PlantBlackthorn extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "blackthorn";
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
        return new String[]{"Black", "Flower", "Rose"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 1, 1, 0, 5, 1);
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
        return "mio_icif:block/crop/blackthorn_" + stage;
    }
}

