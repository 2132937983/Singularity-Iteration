package com.miophas.singularity_iteration.common.block.transformer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * HV-EV 变压器方法? * 用于�?HV（高压）�?EV（超高压）之间进行电压转�? */
@SuppressWarnings("null")
public class mio_icif_block_transformer_hToe extends com.miophas.singularity_iteration.common.block.transformer.mio_icif_block_transformer {

    public static final MapCodec<mio_icif_block_transformer_hToe> CODEC = simpleCodec(mio_icif_block_transformer_hToe::new);

    public mio_icif_block_transformer_hToe(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity(
            com.miophas.singularity_iteration.common.registry.mio_icif_block_entities.TRANSFORMER_HV_EV.get(), pos, state, 512, 1);
    }
}


