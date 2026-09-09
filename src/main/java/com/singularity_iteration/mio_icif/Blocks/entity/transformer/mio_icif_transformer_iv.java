package com.singularity_iteration.mio_icif.Blocks.entity.transformer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_transformer_iv extends mio_icif_transformer {

    private static final ICableTier IV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv");
    private static final ICableTier LUV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv");

    public mio_icif_transformer_iv(BlockPos pos, BlockState state) {
        this(pos, state, TransformerMode.STEP_DOWN);
    }

    public mio_icif_transformer_iv(BlockPos pos, BlockState state, TransformerMode initialMode) {
        super(
            mio_icif_block_entities.TRANSFORMER_IV_LUV.get(),
            pos,
            state,
            IV_TIER,
            LUV_TIER,
            initialMode
        );
    }
}