package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_button extends ButtonBlock {
    public mio_icif_block_rubber_button(Properties properties, BlockSetType blockSetType, int ticksToStayPressed) {
        super(blockSetType, ticksToStayPressed, properties);
    }
    
    public mio_icif_block_rubber_button() {
        super(BlockSetType.OAK, 30, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .ignitedByLava()
            .noCollission());
    }
}