package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_trapdoor extends TrapDoorBlock {
    public mio_icif_block_rubber_trapdoor(Properties properties, BlockSetType blockSetType) {
        super(blockSetType, properties);
    }
    
    public mio_icif_block_rubber_trapdoor() {
        super(BlockSetType.OAK, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .isValidSpawn((state, level, pos, entityType) -> false)
            .ignitedByLava());
    }
}