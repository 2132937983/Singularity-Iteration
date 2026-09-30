package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 棕蘑菇
 */
@SuppressWarnings("null")
public class PlantBrownMushroom extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "brownMushroom";
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
        return new String[]{"Brown", "Food", "Mushroom"};
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
        return "mio_icif:block/crop/brown_mushroom_" + stage;
    }
}