package com.miophas.singularity_iteration.common.block.transformer;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer_eTos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * EV-SC transformer block.
 * It converts voltage between EV (extra-high voltage) and SC (superconductor).
 */
@SuppressWarnings("null")
public class mio_icif_block_transformer_eTos extends com.miophas.singularity_iteration.common.block.transformer.mio_icif_block_transformer {

    public static final MapCodec<mio_icif_block_transformer_eTos> CODEC = simpleCodec(mio_icif_block_transformer_eTos::new);

    public mio_icif_block_transformer_eTos(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return com.miophas.singularity_iteration.common.blockentity.transformer.TransformerEntities.create(
            com.miophas.singularity_iteration.common.blockentity.transformer.TransformerEntities.Tier.EV_SC, pos, state);
    }
}


