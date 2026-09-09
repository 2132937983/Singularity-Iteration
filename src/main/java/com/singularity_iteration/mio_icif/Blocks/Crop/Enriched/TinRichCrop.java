package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import com.mojang.serialization.MapCodec;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富锡作物 - 产出锡粒
 */
public class TinRichCrop extends UniformCropTemplate {
    public static final MapCodec<TinRichCrop> CODEC = simpleCodec(TinRichCrop::new);

    public TinRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_TIN_RICH.get(),
                () -> com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.TIN_NUGGET.get());
    }

    public TinRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}