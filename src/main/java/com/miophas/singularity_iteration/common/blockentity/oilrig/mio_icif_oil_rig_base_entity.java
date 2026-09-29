package com.miophas.singularity_iteration.common.blockentity.oilrig;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_oil_rig_base_entity extends BlockEntity {

    public mio_icif_oil_rig_base_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.OIL_RIG_BASE.get(), pos, state);
    }
}