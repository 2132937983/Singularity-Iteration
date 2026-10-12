package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import net.minecraft.world.level.block.Block;

/**
 * <p><b>EN:</b> Uranium leaves: small uranium-238. The last stage needs uranium ore or a uranium block under the crop.
 * <p><b>ZH:</b> 铀叶：产出小撮铀-238。最后阶段需要作物下方有铀矿石或铀块。
 * <p><b>JA:</b> ウランの葉：小さなウラン 238 を出す。最終段階には作物の下にウラン鉱石かウランブロックが必要。
 */
public class PlantUranium extends OreRootCrop {
    private static final PlantStats STATS = new PlantStats(4, 0, 3, 0, 1, 0);

    public PlantUranium() {
        super("uranium", new String[]{"Green", "Leaves", "Metal", "Radioactive"}, STATS, mio_icif_resources.URAN_238_SMALL::get,
            () -> new Block[]{mio_icif_blocks.BLOCK_ORE_URAN.get(), mio_icif_blocks.BLOCK_ORE_URAN_IN_DEEP.get(),
                mio_icif_blocks.BLOCK_URAN.get()});
    }
}
