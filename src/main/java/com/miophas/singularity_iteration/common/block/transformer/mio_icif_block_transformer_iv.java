package com.miophas.singularity_iteration.common.block.transformer;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer_iv;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_block_transformer_iv extends mio_icif_block_transformer {

    public static final MapCodec<mio_icif_block_transformer_iv> CODEC = simpleCodec(mio_icif_block_transformer_iv::new);

    public mio_icif_block_transformer_iv(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_transformer_iv(pos, state);
    }
}
