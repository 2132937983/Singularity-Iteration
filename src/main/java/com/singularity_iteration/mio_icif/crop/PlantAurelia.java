package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 金叶 - 产出小堆金粉
 * IC2: Uncommon级别，maxSize=5，需要下方有金矿石或金块才能从size4长到size5
 */
@SuppressWarnings("null")
public class PlantAurelia extends PlantType {

    @Override
    public String getTypeId() {
        return "aurelia";
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
        return new String[]{"Gold", "Leaves", "Metal"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(5, 0, 3, 0, 2, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 5;
    }

    @Override
    public int getHarvestStage() {
        return 5;
    }

    @Override
    public int getStageAfterHarvest() {
        return 1;
    }

    @Override
    public ItemStack[] getHarvest(IPlanter planter) {
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 4;
        return new ItemStack[]{new ItemStack(mio_icif_resources.GOLDEN_DUST_SMALL.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/aurelia_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        if (planter.getGrowthStage() < 4) return true;
        if (planter.getGrowthStage() == 4) {
            return planter.isBlockBelow(net.minecraft.world.level.block.Blocks.GOLD_ORE) ||
                   planter.isBlockBelow(net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE) ||
                   planter.isBlockBelow(net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
        }
        return false;
    }
}