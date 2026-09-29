package com.miophas.singularity_iteration.common.blockentity.pipe;

import com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** IC2 connections are explicit ports, opened by placement or a wrench. */
public final class FluidPipeBlockEntity extends mio_icif_pipe_fluid {
    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.FLUID_PIPE_ENTITY_TYPE.get(), pos, state,
            ((FluidPipeBlock) state.getBlock()).grade().capacity(state.getValue(FluidPipeBlock.SIZE)),
            ((FluidPipeBlock) state.getBlock()).grade().transferPerTick(state.getValue(FluidPipeBlock.SIZE)));
        blockedDirections.addAll(java.util.EnumSet.allOf(Direction.class));
    }

    @Override protected boolean includeEqualPipes() { return true; }

    @Override public int getTransferRate() {
        var state = getBlockState();
        return ((FluidPipeBlock) state.getBlock()).grade().transferPerTick(state.getValue(FluidPipeBlock.SIZE));
    }

    @Override protected void updateConnections() {
        if (level == null || level.isClientSide) return;
        boolean changed = false;
        for (Direction side : Direction.values()) {
            boolean connected = !isDirectionBlocked(side);
            if (connections[side.ordinal()] != connected) {
                connections[side.ordinal()] = connected;
                changed = true;
            }
        }
        if (changed) {
            invalidateCapabilities();
            setChanged();
        }
        updateBlockState();
    }

    @Override public void blockDirection(Direction side) {
        super.blockDirection(side);
        updateConnections();
    }

    @Override public void unblockDirection(Direction side) {
        super.unblockDirection(side);
        updateConnections();
    }

    public void toggleConnection(Direction side) {
        boolean open = isDirectionBlocked(side);
        if (open) unblockDirection(side); else blockDirection(side);
        // A pipe-to-pipe joint has two ends. Toggle both, without recursive callbacks.
        BlockPos adjacent = worldPosition.relative(side);
        if (level != null && level.getChunkSource().hasChunk(adjacent.getX() >> 4, adjacent.getZ() >> 4)
                && level.getBlockEntity(adjacent) instanceof mio_icif_pipe_fluid other) {
            if (open) other.unblockDirection(side.getOpposite()); else other.blockDirection(side.getOpposite());
        }
    }

    @Override @Nullable public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return side == null || isDirectionBlocked(side) ? null : super.getFluidHandlerCapability(side);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("BlockedDirections")) blockedDirections.addAll(java.util.EnumSet.allOf(Direction.class));
    }
}
