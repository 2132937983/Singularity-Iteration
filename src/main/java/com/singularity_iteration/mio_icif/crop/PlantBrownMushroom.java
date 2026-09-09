package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 棕蘑菇
 */
@SuppressWarnings("null")
public class PlantBrownMushroom extends PlantType {

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
        return new String[]{"Brown", "Mushroom", "Fungus"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(2, 0, 3, 0, 0, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 3;
    }

    @Override
    public int getHarvestStage() {
        return 3;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 3;
        return new ItemStack[]{new ItemStack(Items.BROWN_MUSHROOM, count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/brown_mushroom_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        // 蘑菇需要低光照（<=10）才能生长
    return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() <= 10;
    }
}