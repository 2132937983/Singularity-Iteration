package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 食人花 - IC2 CropEating
 * maxSize=6, harvestSize=4, optimalHarvestSize=4
 * stage<3: 仅需光照>10
 * stage>=3: 需光照>10 + 下方岩浆
 * stage 4-5可收获产物:仙人掌, stage 6过熟不可收获
 * stage>=2: 吞噬靠近的生物拉扯+伤害+药效+加速生长
 */
@SuppressWarnings("null")
public class PlantEatingPlant extends PlantType {

    @Override
    public String getTypeId() {
        return "eatingplant";
    }

    @Override
    public String getModId() {
        return Singularity_Iteration.MOD_ID;
    }

    @Override
    public String getFoundBy() {
        return "Hasudako";
    }

    @Override
    public String[] getTraits() {
        return new String[]{"Bad", "Food"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(6, 1, 1, 3, 1, 4);
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
        return 4;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int stage = planter.getGrowthStage();
        if (stage >= 4 && stage < 6) {
            return new ItemStack[]{new ItemStack(Items.CACTUS, 1)};
        }
        return new ItemStack[0];
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/eatingplant_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        int stage = planter.getGrowthStage();
        if (stage < 3) {
            return planter.getLightLevel() > 10;
        }
        return planter.getLightLevel() > 10
                && planter.isBlockBelow(net.minecraft.world.level.block.Blocks.LAVA);
    }

    @Override
    public boolean isHarvestable(IPlanter planter) {
        int stage = planter.getGrowthStage();
        return stage >= 4 && stage < 6;
    }
}