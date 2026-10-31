package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: a fluid port of the Steam Tunnel (IC2 Classic's valve): steam goes in, water comes out. Its controller links it while
 * the structure is formed; unlinked it holds nothing. Every 10 ticks it pushes up to 1,000 mB of the tunnel's water into each
 * neighbour outside the tunnel that takes water.
 */
public class SteamTunnelFluidPortBlockEntity extends BlockEntity implements IFluidHandler, SteamTunnelBlockEntity.SteamTunnelPart {
    public static final int PUSH_TICKS = 10, PUSH_AMOUNT = 1_000;
    private @Nullable BlockPos controller;

    public SteamTunnelFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.STEAM_TUNNEL_FLUID_PORT_ENTITY_TYPE.get(), pos, state);
    }

    @Override public void link(BlockPos controller) { this.controller = controller.immutable(); setChanged(); }

    @Override public void unlink(BlockPos controller) { if (controller.equals(this.controller)) { this.controller = null; setChanged(); } }

    /** The formed tunnel this port serves, or null. */
    public @Nullable SteamTunnelBlockEntity tunnel() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof SteamTunnelBlockEntity tunnel && tunnel.serves(worldPosition) ? tunnel : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamTunnelFluidPortBlockEntity port) {
        if (level.isClientSide || level.getGameTime() % PUSH_TICKS != 0) return;
        SteamTunnelBlockEntity tunnel = port.tunnel();
        if (tunnel == null || tunnel.water().isEmpty()) return;
        for (Direction side : Direction.values()) {
            BlockPos at = pos.relative(side);
            if (!level.isLoaded(at) || level.getBlockEntity(at) instanceof SteamTunnelBlockEntity.SteamTunnelPart
                    || level.getBlockEntity(at) instanceof SteamTunnelBlockEntity) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, at, side.getOpposite());
            if (target == null) continue;
            FluidStack offer = tunnel.drain(PUSH_AMOUNT, FluidAction.SIMULATE);
            if (offer.isEmpty()) return;
            int taken = target.fill(offer, FluidAction.EXECUTE);
            if (taken > 0) tunnel.drain(taken, FluidAction.EXECUTE);
        }
    }

    // ------------------------------------------------------------------ fluids, through the tunnel

    @Override public int getTanks() { var t = tunnel(); return t == null ? 0 : t.getTanks(); }

    @Override public FluidStack getFluidInTank(int tank) { var t = tunnel(); return t == null ? FluidStack.EMPTY : t.getFluidInTank(tank); }

    @Override public int getTankCapacity(int tank) { var t = tunnel(); return t == null ? 0 : t.getTankCapacity(tank); }

    @Override public boolean isFluidValid(int tank, FluidStack stack) { var t = tunnel(); return t != null && t.isFluidValid(tank, stack); }

    @Override public int fill(FluidStack resource, FluidAction action) { var t = tunnel(); return t == null ? 0 : t.fill(resource, action); }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) { var t = tunnel(); return t == null ? FluidStack.EMPTY : t.drain(resource, action); }

    @Override public FluidStack drain(int maxDrain, FluidAction action) { var t = tunnel(); return t == null ? FluidStack.EMPTY : t.drain(maxDrain, action); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) tag.put("Controller", NbtUtils.writeBlockPos(controller));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
    }
}
