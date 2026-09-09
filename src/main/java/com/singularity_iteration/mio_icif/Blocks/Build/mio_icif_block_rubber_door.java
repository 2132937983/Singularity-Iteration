package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_door extends DoorBlock {
    public mio_icif_block_rubber_door(Properties properties, BlockSetType blockSetType) {
        super(blockSetType, properties);
    }
    
    public mio_icif_block_rubber_door() {
        super(BlockSetType.OAK, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .ignitedByLava());
    }
}