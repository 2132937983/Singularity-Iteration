package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 可可植物 - 产出可可豆
 * 需要正下方3格都是泥土才能种植
 */
@SuppressWarnings("null")
public class PlantCocoa extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "cocoa";
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
        return new String[]{ "Brown", "Food", "Stem" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 1, 3, 0, 4, 0);
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
        return "mio_icif:block/crop/cocoa_" + stage;
    }
}