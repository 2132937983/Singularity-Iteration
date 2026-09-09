package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 奇妙花植物 - 产出紫色染料
 * IC2: size=5产出笑气粉末(venomilia_powder)，size>=4产出紫色染料
 */
@SuppressWarnings("null")
public class PlantVenomilia extends PlantType {

    @Override
    public String getTypeId() {
        return "venomilia";
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
        return new String[]{"Purple", "Poison", "Flower"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(3, 0, 4, 3, 3, 1);
    }

    @Override
    public int getMaxGrowthStage() {
        return 6;
    }

    @Override
    public int getHarvestStage() {
        return 4;
    }

    @Override
    public int getOptimalHarvestStage() {
        return 5;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int stage = planter.getGrowthStage();
        if (stage >= 5) {
            return new ItemStack[]{new ItemStack(Items.PURPLE_DYE, 1)};
        }
        return new ItemStack[]{new ItemStack(Items.PURPLE_DYE, 1)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/venomilia_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        int stage = planter.getGrowthStage();
        return (stage <= 4 && planter.getLightLevel() >= 12) || stage == 5;
    }
}