package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_redstone_reactor_coolant_injector;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 红石冷却液注入器 (RSH): see {@link mio_icif_block_reactor_coolant_injector}. */
public class mio_icif_block_redstone_reactor_coolant_injector extends mio_icif_block_reactor_coolant_injector {

    public static final MapCodec<mio_icif_block_redstone_reactor_coolant_injector> CODEC = simpleCodec(mio_icif_block_redstone_reactor_coolant_injector::new);

    public mio_icif_block_redstone_reactor_coolant_injector(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_redstone_reactor_coolant_injector(pos, state);
    }
}
