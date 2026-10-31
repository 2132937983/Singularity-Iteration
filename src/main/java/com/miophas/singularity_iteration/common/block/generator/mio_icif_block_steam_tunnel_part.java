package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelFluidPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelPowerPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: a shell block of the Steam Tunnel: the casing, and the fluid and power ports ({@link Port}). Removing one makes the
 * controllers in reach check their structure at once.
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_tunnel_part extends Block {
    public mio_icif_block_steam_tunnel_part(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) SteamTunnelBlockEntity.partRemoved(level, pos);
    }

    /** A fluid port ({@code fluid}) or a power port. */
    public static class Port extends mio_icif_block_steam_tunnel_part implements EntityBlock {
        private final boolean fluid;

        public Port(Properties properties, boolean fluid) {
            super(properties);
            this.fluid = fluid;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return fluid ? new SteamTunnelFluidPortBlockEntity(pos, state) : new SteamTunnelPowerPortBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) return null;
            return fluid ? (l, p, s, be) -> { if (be instanceof SteamTunnelFluidPortBlockEntity port) SteamTunnelFluidPortBlockEntity.tick(l, p, s, port); }
                : (l, p, s, be) -> { if (be instanceof SteamTunnelPowerPortBlockEntity port) SteamTunnelPowerPortBlockEntity.tick(l, p, s, port); };
        }
    }
}
