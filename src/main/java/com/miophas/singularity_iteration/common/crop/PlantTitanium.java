package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 钛叶子 - 产出小堆钛粉
 * IC2: 需要下方有钛矿石/钛块才能从size3长到size4
 */
@SuppressWarnings("null")
public class PlantTitanium extends PlantType {

    @Override
    public String getTypeId() {
        return "titanium";
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
        return new String[]{"White", "Leaves", "Metal"};
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
        return new ItemStack[]{new ItemStack(mio_icif_resources.TITANIUM_DUST_SMALL.get(), count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/titanium_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        if (planter.getGrowthStage() < 3) return true;
        if (planter.getGrowthStage() == 3) {
            return planter.isBlockBelow(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.BLOCK_ORE_TITANIUM.get()) ||
                   planter.isBlockBelow(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.BLOCK_ORE_TITANIUM_IN_DEEP.get()) ||
                   planter.isBlockBelow(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.BLOCK_TITANIUM.get());
        }
        return false;
    }
    // SI extension: retain legacy growth/output, but support the shared root and seed lifecycle.
    @Override public int getRootDepth(IPlanter planter) { return 5; }
    @Override public ItemStack getSeedItem(IPlanter planter) {
        return planter.makeSeeds(this, 1, planter.getGrowthSpeed(), planter.getYield(), planter.getResilience());
    }
}
