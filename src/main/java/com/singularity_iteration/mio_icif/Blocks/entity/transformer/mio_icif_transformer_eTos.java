package com.singularity_iteration.mio_icif.Blocks.entity.transformer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
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

