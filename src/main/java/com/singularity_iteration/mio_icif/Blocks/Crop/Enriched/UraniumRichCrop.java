package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import com.mojang.serialization.MapCodec;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 富铀作物 - 产出铀238，不支持骨粉催熟
 */
public class UraniumRichCrop extends UniformCropTemplate {
    public static final MapCodec<UraniumRichCrop> CODEC = simpleCodec(UraniumRichCrop::new);

    public UraniumRichCrop(BlockBehaviour.Properties properties) {
        super(properties,
                () -> mio_icif_normal.SEED_URANIUM_RICH.get(),
                () -> com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.URAN_238_SMALL.get());
    }

    public UraniumRichCrop() {
        this(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.PLANT).strength(0.0F).sound(net.minecraft.world.level.block.SoundType.CROP));
    }

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return false;
    }

    @Override
    public boolean isBonemealSuccess(net.minecraft.world.level.Level level, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return false;
    }

    @Override
    public void performBonemeal(net.minecraft.server.level.ServerLevel level, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // 铀作物不支持骨粉催熟
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.CropBlock> codec() {
        return CODEC;
    }
}