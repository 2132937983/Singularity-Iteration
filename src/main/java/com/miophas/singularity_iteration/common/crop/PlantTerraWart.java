package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

@SuppressWarnings("null")
public class PlantTerraWart extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "terrawart";
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
        return new String[]{ "Blue", "Aether", "Consumable", "Snow" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(5, 2, 4, 0, 3, 0);
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
        return "mio_icif:block/crop/terra_wart_" + stage;
    }
}