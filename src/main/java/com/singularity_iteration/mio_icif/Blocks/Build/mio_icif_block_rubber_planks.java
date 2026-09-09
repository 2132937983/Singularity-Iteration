package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_planks extends Block {
    public mio_icif_block_rubber_planks(Properties properties) {
        super(properties);
    }
    
    public mio_icif_block_rubber_planks() {
        super(Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava());
    }
}