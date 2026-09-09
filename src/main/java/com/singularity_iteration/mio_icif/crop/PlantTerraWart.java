package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class PlantTerraWart extends PlantType {

    @Override
    public String getTypeId() {
        return "terrawart";
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
        return new String[]{"Blue", "Aether", "Wart"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 0, 2, 0, 2, 0);
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
        return new ItemStack[]{new ItemStack(mio_icif_resources.TERRA_WART.get(), 1)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/terra_wart_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }
}