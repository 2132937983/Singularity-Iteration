package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 小麦植物
 * 基础粮食作物
 */
@SuppressWarnings("null")
public class PlantWheat extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "wheat";
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy() {
        return "unknown";
    }

    @Override
    public String getFoundBy(String playerName) {
        return playerName != null ? playerName : super.getFoundBy(playerName);
    }

    @Override
    public String[] getTraits() {
        return new String[]{ "Yellow", "Food", "Wheat" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 4, 0, 0, 2);
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
        return "mio_icif:block/crop/wheat_" + stage;
    }
}
