package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_fence_gate extends FenceGateBlock {
    public mio_icif_block_rubber_fence_gate(Properties properties, WoodType woodType) {
        super(woodType, properties);
    }
    
    public mio_icif_block_rubber_fence_gate() {
        super(mio_icif_block_rubber_sign.RUBBER_WOOD_TYPE, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava());
    }
}