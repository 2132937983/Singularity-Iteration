package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.item.ItemStack;

/**
 * 啤酒花植物
 */
@SuppressWarnings("null")
public class PlantHops extends PlantType {

    @Override
    public String getTypeId() {
        return "hops";
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
        return new String[]{"Green", "Brewing", "Hops"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 4, 0, 1, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 7;
    }

    @Override
    public int getHarvestStage() {
        return 6;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 4;
        // 啤酒花收获啤酒花
        return new ItemStack[]{new ItemStack(mio_icif_normal.HOPS.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/hops_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}