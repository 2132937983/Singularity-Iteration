package com.miophas.singularity_iteration.common.blockentity.transformer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_transformer_luv extends mio_icif_transformer {

    private static final ICableTier LUV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv");
    private static final ICableTier ZPMV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("zpmv");

    public mio_icif_transformer_luv(BlockPos pos, BlockState state) {
        this(pos, state, TransformerMode.STEP_DOWN);
    }

    public mio_icif_transformer_luv(BlockPos pos, BlockState state, TransformerMode initialMode) {
        super(
            mio_icif_block_entities.TRANSFORMER_LUV_ZPMV.get(),
            pos,
            state,
            LUV_TIER,
            ZPMV_TIER,
            initialMode
        );
    }
}