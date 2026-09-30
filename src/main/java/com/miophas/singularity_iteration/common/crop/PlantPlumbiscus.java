package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import net.minecraft.world.item.ItemStack;

/**
 * 铅叶草 - 产出小堆铅粉
 * IC2: 需要下方有铅矿石或铅块才能从size3长到size4
 */
@SuppressWarnings("null")
public class PlantPlumbiscus extends BuiltinCrop {

    @Override
    public String getTypeId() {
        return "plumbiscus";
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
        return new String[]{"Dense", "Leaves", "Metal"};
    }

    @Override
    public PlantStats getStats() {
        return new PlantStats(6, 2, 0, 0, 1, 0);
    }

    @Override
    public int getMaxGrowthStage() {
        return 4;
    }

    @Override
    public String getTexture(int stage) {
        if (stage < 1 || stage > getMaxGrowthStage()) {
            stage = 1;
        }
        return "mio_icif:block/crop/plumbiscus_" + stage;
    }
}