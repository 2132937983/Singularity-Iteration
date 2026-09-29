package com.miophas.singularity_iteration.common.block.crop.enriched;

import com.mojang.serialization.MapCodec;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富铅作物 - 产出铅粒
 */
public class LeadRichCrop extends UniformCropTemplate {
    public static final MapCodec<LeadRichCrop> CODEC = simpleCodec(LeadRichCrop::new);

    public LeadRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_LEAD_RICH.get(),
                () -> com.miophas.singularity_iteration.common.item.resource.mio_icif_resources.LEAD_NUGGET.get());
    }

    public LeadRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}