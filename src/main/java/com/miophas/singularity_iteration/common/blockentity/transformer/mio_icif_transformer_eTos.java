package com.miophas.singularity_iteration.common.blockentity.transformer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * EV/IV ???
 * - ????EV?2048 EU/t ???
 * - ????IV?8192 EU/t ???
 */
@SuppressWarnings("null")
public class mio_icif_transformer_eTos extends mio_icif_transformer {

    public mio_icif_transformer_eTos(BlockPos pos, BlockState state) {
        this(pos, state, TransformerMode.STEP_DOWN);
    }

    public mio_icif_transformer_eTos(BlockPos pos, BlockState state, TransformerMode initialMode) {
        super(
            mio_icif_block_entities.TRANSFORMER_EV_SC.get(),
            pos,
            state,
            CableTier.EV,
            CableTier.IV,
            initialMode
        );
    }
}

