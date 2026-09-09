package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import com.mojang.serialization.MapCodec;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富钛作物 - 产出钛粒
 */
public class TitaniumRichCrop extends UniformCropTemplate {
    public static final MapCodec<TitaniumRichCrop> CODEC = simpleCodec(TitaniumRichCrop::new);

    public TitaniumRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_TITANIUM_RICH.get(),
                () -> com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.TITANIUM_NUGGET.get());
    }

    public TitaniumRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}