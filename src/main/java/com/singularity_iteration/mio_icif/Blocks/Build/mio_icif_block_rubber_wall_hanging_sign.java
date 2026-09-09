package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_wall_hanging_sign extends WallHangingSignBlock {
    public mio_icif_block_rubber_wall_hanging_sign(Properties properties) {
        super(mio_icif_block_rubber_sign.RUBBER_WOOD_TYPE, properties);
    }
    
    public mio_icif_block_rubber_wall_hanging_sign() {
        super(mio_icif_block_rubber_sign.RUBBER_WOOD_TYPE, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava()
            .noCollission()
            .forceSolidOn());
    }
}