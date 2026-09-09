package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 铁叶草 - 产出小堆铁粉
 * IC2: 需要下方有铁矿石或铁块才能从size3长到size4
 */
@SuppressWarnings("null")
public class PlantFerru extends PlantType {

    @Override
    public String getTypeId() {
        return "ferru";
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
        return new String[]{"Gray", "Leaves", "Metal"};
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
        return new ItemStack[]{new ItemStack(mio_icif_resources.IRON_DUST_SMALL.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/ferru_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        if (planter.getGrowthStage() < 3) return true;
        if (planter.getGrowthStage() == 3) {
            return planter.isBlockBelow(net.minecraft.world.level.block.Blocks.IRON_ORE) ||
                   planter.isBlockBelow(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE) ||
                   planter.isBlockBelow(net.minecraft.world.level.block.Blocks.IRON_BLOCK);
        }
        return false;
    }
}