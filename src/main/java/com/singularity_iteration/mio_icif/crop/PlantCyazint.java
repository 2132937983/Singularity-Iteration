package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 青色花植物
 */
@SuppressWarnings("null")
public class PlantCyazint extends PlantType {

    @Override
    public String getTypeId() {
        return "cyazint";
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
        return new String[]{"Cyan", "Flower", "Cyazint"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 2, 0, 5, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 4;
    }

    @Override
    public int getHarvestStage() {
        return 4;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        return new ItemStack[]{new ItemStack(Items.BLUE_ORCHID, 1)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/cyazint_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() <= 3 && planter.getLightLevel() >= 12;
    }
}