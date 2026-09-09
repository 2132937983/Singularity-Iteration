package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

public class mio_icif_block_rubber_sign extends StandingSignBlock {
    // 使用橡木的 BlockSetType，但创建我们自己的 WoodType
    public static final WoodType RUBBER_WOOD_TYPE = WoodType.register(
        new WoodType("mio_icif:rubber", BlockSetType.OAK)
    );

    public mio_icif_block_rubber_sign(Properties properties) {
        super(RUBBER_WOOD_TYPE, properties);
    }
    
    public mio_icif_block_rubber_sign() {
        super(RUBBER_WOOD_TYPE, Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(1.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava()
            .noCollission()
            .forceSolidOn());
    }
}