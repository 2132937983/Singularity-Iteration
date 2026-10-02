package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;

/**
 * 铜叶草 - 产出小堆铜粉
 * IC2: 需要下方有铜矿石或铜块才能从size3长到size4
 */
@SuppressWarnings("null")
public class PlantCyprium extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "cyprium";
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
        return new String[]{"Orange", "Leaves", "Metal"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(6, 2, 0, 0, 1, 0);
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
        return "mio_icif:block/crop/cyprium_" + stage;
    }
}