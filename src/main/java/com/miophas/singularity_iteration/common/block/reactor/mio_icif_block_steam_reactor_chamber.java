package com.miophas.singularity_iteration.common.block.reactor;

import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_chamber;
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

/** 0.1.10.36: a steam reactor chamber: one more column for a steam reactor (never an electric one), and its fluid ports. */
@SuppressWarnings("null")
public class mio_icif_block_steam_reactor_chamber extends mio_icif_Block_Reactor_Chamber {

    public mio_icif_block_steam_reactor_chamber(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(mio_icif_block_steam_reactor_chamber::new);
    }

    @Override
    protected boolean steamChamber() {
        return true;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.STEAM_REACTOR_CHAMBER_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_reactor_chamber chamber) mio_icif_reactor_chamber.tick(l, p, s, chamber);
            } : null;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.STEAM_REACTOR_CHAMBER_ENTITY_TYPE.get().create(pos, state);
    }
}
