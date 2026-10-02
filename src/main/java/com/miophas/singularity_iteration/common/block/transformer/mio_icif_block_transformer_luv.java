package com.miophas.singularity_iteration.common.block.transformer;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer_luv;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_block_transformer_luv extends mio_icif_block_transformer {

    public static final MapCodec<mio_icif_block_transformer_luv> CODEC = simpleCodec(mio_icif_block_transformer_luv::new);

    public mio_icif_block_transformer_luv(Properties properties) {
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
            com.miophas.singularity_iteration.common.blockentity.transformer.TransformerEntities.Tier.LUV_ZPMV, pos, state);
    }
}
