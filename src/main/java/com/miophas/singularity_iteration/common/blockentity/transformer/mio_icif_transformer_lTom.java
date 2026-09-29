package com.miophas.singularity_iteration.common.blockentity.transformer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LV/MV ???
 * - ????LV?32 EU/t ???
 * - ????MV?128 EU/t ???
 */
@SuppressWarnings("null")
public class mio_icif_transformer_lTom extends mio_icif_transformer {

    public mio_icif_transformer_lTom(BlockPos pos, BlockState state) {
        this(pos, state, TransformerMode.STEP_DOWN);
    }

    public mio_icif_transformer_lTom(BlockPos pos, BlockState state, TransformerMode initialMode) {
        super(
            mio_icif_block_entities.TRANSFORMER_LV_MV.get(),
            pos,
            state,
            CableTier.LV,
            CableTier.MV,
            initialMode
        );
    }
}


