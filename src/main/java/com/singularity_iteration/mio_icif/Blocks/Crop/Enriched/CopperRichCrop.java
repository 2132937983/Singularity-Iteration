package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import com.mojang.serialization.MapCodec;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富铜作物 - 产出铜粒
 */
public class CopperRichCrop extends UniformCropTemplate {
    public static final MapCodec<CopperRichCrop> CODEC = simpleCodec(CopperRichCrop::new);

    public CopperRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_COPPER_RICH.get(),
                () -> com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.COPPER_NUGGET.get());
    }

    public CopperRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}