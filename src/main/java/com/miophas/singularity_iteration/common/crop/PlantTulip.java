package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 郁金香植物
 */
@SuppressWarnings("null")
public class PlantTulip extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "tulip";
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
        return new String[]{"Purple", "Flower", "Tulip"};
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
        return "mio_icif:block/crop/tulip_" + stage;
    }
}