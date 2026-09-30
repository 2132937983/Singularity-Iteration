package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 杂草植物
 * 最基础的植物，会自然生成并扩散
 */
@SuppressWarnings("null")
public class PlantWeed extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "weed";
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
        return new String[]{ "Weed", "Bad" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(0, 0, 0, 1, 0, 5);
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/weed_" + stage;
    }
}

