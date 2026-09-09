package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 地狱疣植物 - 产出地狱疣
 * 需要正下方有岩浆才能生长
 */
@SuppressWarnings("null")
public class PlantNetherWart extends PlantType {

    @Override
    public String getTypeId() {
        return "netherwart";
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
        return new String[]{"Red", "Nether", "Wart"};
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
        int yield = planter.getYield();
        int count = 1 + Math.max(0, yield) / 3;
        return new ItemStack[]{new ItemStack(Items.NETHER_WART, count)};
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/netherwart_" + stage;
    }

    @Override
    public boolean canGrow(IPlanter planter) {
        return planter.getGrowthStage() < getMaxGrowthStage();
    }

    @Override
    public void tick(IPlanter planter) {
        // 如果下方是灵魂沙，加速生长
    if (planter.isBlockBelow(net.minecraft.world.level.block.Blocks.SOUL_SAND) ||
            planter.isBlockBelow(net.minecraft.world.level.block.Blocks.SOUL_SOIL)) {
            if (planter.getPlanterWorld().getRandom().nextInt(30) == 0) {
                // 每30tick一次机会额外增加生长点
                planter.setProgress(planter.getProgress() + 100);
            }
        }
    }
}