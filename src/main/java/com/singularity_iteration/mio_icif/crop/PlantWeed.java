package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 杂草植物
 * 最基础的植物，会自然生成并扩散
 */
@SuppressWarnings("null")
public class PlantWeed extends PlantType {

    @Override
    public String getTypeId() {
        return "weed";
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
        return new String[]{"Weed", "Bad"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(0, 0, 0, 1, 0, 1);
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public int getGrowthTime(IPlanter planter) {
        return 50; // 生长很快
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }

    @Override
    public boolean isWeed(IPlanter planter) {
        return true;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        // 杂草掉落杂草物品
        return new ItemStack[]{new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.WEED.get())};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/weed_" + stage;
    }
}

