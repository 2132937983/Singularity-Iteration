package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;

public class mio_icif_block_rubber_stairs extends StairBlock {
    public mio_icif_block_rubber_stairs(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }
    
    public mio_icif_block_rubber_stairs(BlockState baseState) {
        super(baseState, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava());
    }
}