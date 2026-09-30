package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 地狱疣植物 - 产出地狱疣
 * 根系范围内的灵魂沙可加速生长；无需岩浆
 */
@SuppressWarnings("null")
public class PlantNetherWart extends BuiltinCrop {

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
        return new String[]{ "Red", "Nether", "Ingredient", "Soulsand" };
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(5, 4, 2, 0, 2, 1);
    }

    @Override
    public int getMaxGrowthStage() {
        return 3;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/netherwart_" + stage;
    }
}