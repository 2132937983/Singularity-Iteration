package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import com.mojang.serialization.MapCodec;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富铁作物 - 产出铁粒
 */
public class IronRichCrop extends UniformCropTemplate {
    public static final MapCodec<IronRichCrop> CODEC = simpleCodec(IronRichCrop::new);

    public IronRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_IRON_RICH.get(),
                () -> net.minecraft.world.item.Items.IRON_NUGGET);
    }

    public IronRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}