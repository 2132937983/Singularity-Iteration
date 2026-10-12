package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import net.minecraft.world.level.block.Block;

/**
 * <p><b>EN:</b> Titanium leaves: small titanium dust. The last stage needs titanium ore or a titanium block under the crop.
 * <p><b>ZH:</b> 钛叶：产出小撮钛粉。最后阶段需要作物下方有钛矿石或钛块。
 * <p><b>JA:</b> チタンの葉：小さなチタン粉を出す。最終段階には作物の下にチタン鉱石かチタンブロックが必要。
 */
public class PlantTitanium extends OreRootCrop {
    private static final PlantStats STATS = new PlantStats(4, 0, 3, 0, 1, 0);

    public PlantTitanium() {
        super("titanium", new String[]{"White", "Leaves", "Metal"}, STATS, mio_icif_resources.TITANIUM_DUST_SMALL::get,
            () -> new Block[]{mio_icif_blocks.BLOCK_ORE_TITANIUM.get(), mio_icif_blocks.BLOCK_ORE_TITANIUM_IN_DEEP.get(),
                mio_icif_blocks.BLOCK_TITANIUM.get()});
    }
}
