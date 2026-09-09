package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 锡叶草 - 产出小堆锡粉
 * IC2: 需要下方有锡矿石或锡块才能从size3长到size4
 */
@SuppressWarnings("null")
public class PlantStagnium extends PlantType {

    @Override
    public String getTypeId() {
        return "stagnium";
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
        return new String[]{"Shiny", "Leaves", "Metal"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(4, 0, 3, 0, 1, 0);
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
        int count = 1 + Math.max(0, yield) / 4;
        return new ItemStack[]{new ItemStack(mio_icif_resources.TIN_DUST_SMALL.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/stagnium_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        if (planter.getGrowthStage() < 3) return true;
        if (planter.getGrowthStage() == 3) {
            return planter.isBlockBelow(mio_icif_blocks.BLOCK_ORE_TIN.get()) ||
                   planter.isBlockBelow(mio_icif_blocks.BLOCK_ORE_TIN_IN_DEEP.get()) ||
                   planter.isBlockBelow(mio_icif_blocks.BLOCK_TIN.get());
        }
        return false;
    }
}