package com.miophas.singularity_iteration.common.blockentity.transformer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MV/HV ???
 * - ????MV?128 EU/t ???
 * - ????HV?512 EU/t ???
 */
@SuppressWarnings("null")
public class mio_icif_transformer_mToh extends mio_icif_transformer {

    public mio_icif_transformer_mToh(BlockPos pos, BlockState state) {
        this(pos, state, TransformerMode.STEP_DOWN);
    }

    public mio_icif_transformer_mToh(BlockPos pos, BlockState state, TransformerMode initialMode) {
        super(
            mio_icif_block_entities.TRANSFORMER_MV_HV.get(),
            pos,
            state,
            CableTier.MV,
            CableTier.HV,
            initialMode
        );
    }
}


