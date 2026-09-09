package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_pressure_plate extends PressurePlateBlock {
    public mio_icif_block_rubber_pressure_plate(Properties properties, BlockSetType blockSetType) {
        super(blockSetType, properties);
    }
    
    public mio_icif_block_rubber_pressure_plate() {
        super(BlockSetType.OAK, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .ignitedByLava()
            .noCollission());
    }
}