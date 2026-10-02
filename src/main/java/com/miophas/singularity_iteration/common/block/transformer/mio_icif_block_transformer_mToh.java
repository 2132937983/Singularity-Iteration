package com.miophas.singularity_iteration.common.block.transformer;

import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer_mToh;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * MV-HV 变压器方法? * 用于�?MV（中压）�?HV（高压）之间进行电压转换
 */
@SuppressWarnings("null")
public class mio_icif_block_transformer_mToh extends com.miophas.singularity_iteration.common.block.transformer.mio_icif_block_transformer {

    public static final MapCodec<mio_icif_block_transformer_mToh> CODEC = simpleCodec(mio_icif_block_transformer_mToh::new);

    public mio_icif_block_transformer_mToh(Properties properties) {
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
            com.miophas.singularity_iteration.common.blockentity.transformer.TransformerEntities.Tier.MV_HV, pos, state);
    }
}


