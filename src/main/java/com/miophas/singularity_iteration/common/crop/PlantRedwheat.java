package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 红小麦 - 特殊小麦变种
 */
@SuppressWarnings("null")
public class PlantRedwheat extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "redwheat";
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
        return new String[]{ "Red", "Redstone", "Wheat" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(6, 3, 0, 0, 2, 0);
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
        return "mio_icif:block/crop/redwheat_" + stage;
    }
}
