// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_Semifluid_generator;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_steam_turbine;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 0.1.10.36: the steam turbine block (the semifluid generator block with the turbine's block entity). */
public class mio_icif_block_steam_turbine extends mio_icif_Block_Semifluid_Generator {
    public mio_icif_block_steam_turbine(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(mio_icif_block_steam_turbine::new);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.STEAM_TURBINE_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_steam_turbine turbine) mio_icif_Semifluid_generator.tick(l, p, s, turbine);
            } : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_steam_turbine(pos, state);
    }
}
