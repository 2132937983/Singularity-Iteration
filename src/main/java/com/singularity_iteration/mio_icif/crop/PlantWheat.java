package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 小麦植物
 * 基础粮食作物
 */
@SuppressWarnings("null")
public class PlantWheat extends PlantType {

    @Override
    public String getTypeId() {
        return "wheat";
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy(String playerName) {
        return playerName != null ? playerName : super.getFoundBy(playerName);
    }

    @Override
    public String[] getTraits() {
        return new String[]{"Yellow", "Food", "Metal", "Wheat"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(1, 0, 4, 0, 0, 0);
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

        // 基础收获：小麦 + 种子
        ItemStack[] harvest;

        if (yield <= 0) {
            harvest = new ItemStack[]{new ItemStack(Items.WHEAT)};
        } else if (yield <= 7) {
            harvest = new ItemStack[]{
                    new ItemStack(Items.WHEAT, 1 + yield / 4),
                    new ItemStack(Items.WHEAT_SEEDS, yield / 4)
            };
        } else {
            harvest = new ItemStack[]{
                    new ItemStack(Items.WHEAT, 1 + yield / 3),
                    new ItemStack(Items.WHEAT_SEEDS, yield / 3)
            };
        }

        return harvest;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/wheat_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage() && planter.getLightLevel() >= 9;
    }

    @Override
    public ItemStack getSeedItem(IPlanter planter) {
        return new ItemStack(Items.WHEAT_SEEDS);
    }
}