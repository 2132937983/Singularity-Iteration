package com.singularity_iteration.mio_icif.Blocks.Transformer;

import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer_iv;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_block_transformer_iv extends mio_icif_block_transformer {

    public mio_icif_block_transformer_iv(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_transformer_iv(pos, state);
    }
}