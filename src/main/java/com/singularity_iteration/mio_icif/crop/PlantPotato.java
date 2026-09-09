package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 土豆植物
 */
@SuppressWarnings("null")
public class PlantPotato extends PlantType {

    @Override
    public String getTypeId() {
        return "potato";
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
        return new String[]{"Brown", "Food", "Potato"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 3, 0, 0, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 4;
    }

    @Override
    public int getHarvestStage() {
        return 3; // 第3阶段可收获普通马铃薯
    }

    @Override
    public int getOptimalHarvestStage() {
        return 3; // 最佳收获阶段是第3阶段（避免毒马铃薯）
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 3;

        if (planter.getGrowthStage() >= 4) {
            if (Math.random() < 0.05) {
                return new ItemStack[]{
                        new ItemStack(Items.POTATO, count),
                        new ItemStack(Items.POISONOUS_POTATO, 1)
                };
            }
        }

        return new ItemStack[]{new ItemStack(Items.POTATO, count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/potato_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }
}