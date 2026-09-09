package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;



/**
 * 竹芦植物 - 高级芦苇
 */
@SuppressWarnings("null")
public class PlantStickreed extends PlantType {

    @Override
    public String getTypeId() {
        return "stickreed";
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
        return new String[]{"Brown", "Sugar", "Resin", "Reed"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(4, 0, 4, 1, 0, 0);
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
        int yield = planter.getYield();
        int stage = planter.getGrowthStage();
        if (stage >= 4) {
            int resinCount = 1 + Math.max(0, yield) / 3;
            return new ItemStack[]{new ItemStack(mio_icif_resources.HARZ.get(), resinCount)};
        }
        int caneCount = 1 + Math.max(0, yield) / 3;
        return new ItemStack[]{new ItemStack(Items.SUGAR_CANE, caneCount)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/stickreed_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }
}