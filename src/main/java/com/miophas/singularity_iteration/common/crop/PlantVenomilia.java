package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 奇妙花植物 - 产出紫色染料
 * IC2: size=5产出笑气粉末(venomilia_powder)，size>=4产出紫色染料
 */
@SuppressWarnings("null")
public class PlantVenomilia extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "venomilia";
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
        return new String[]{ "Purple", "Flower", "Tulip", "Poison" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 3, 1, 3, 3, 3);
    }

    @Override
    public int getMaxGrowthStage() {
        return 6;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/venomilia_" + stage;
    }
}