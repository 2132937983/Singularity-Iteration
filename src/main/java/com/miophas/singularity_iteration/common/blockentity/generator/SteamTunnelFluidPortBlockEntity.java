package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_fluid_port;
import com.miophas.singularity_iteration.common.menu.generator.ReactorFluidPortMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * neighbour outside the tunnel that takes water. Its menu is the fluid reactor port's: one slot for a Fluid Ejector Upgrade (water
 * out, with its directions and rate) or a Fluid Pulling Upgrade (steam in from tanks next to it).
 */
public class SteamTunnelFluidPortBlockEntity extends BlockEntity implements IFluidHandler, SteamTunnelBlockEntity.SteamTunnelPart,
        MenuProvider, ReactorFluidPortMenu.Host {
    public static final int PUSH_TICKS = 10, PUSH_AMOUNT = 1_000;
    private @Nullable BlockPos controller;

    private final MachineItemHandler itemHandler = new MachineItemHandler(SlotLayout.builder().extra(1).build()) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };
    private MachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();
    private final FluidAutomationComponent fluidAutomation = new FluidAutomationComponent(new FluidAutomationComponent.Host() {
        @Override public Level level() { return level; }
        @Override public BlockPos worldPosition() { return worldPosition; }
        @Override public IFluidHandler ownFluidHandler() { return tunnel(); }
        @Override public IFluidHandler adjacentFluidHandler(BlockPos pos, @Nullable Direction side) { return outside(pos, side); }
        @Override public IMachineUpgradeStats upgradeStats() { return upgradeStats; }
        @Override public void markUnsaved() { ContainerToTank.markUnsaved(SteamTunnelFluidPortBlockEntity.this); }
        @Override public void markNeighborUnsaved(BlockPos pos) { FluidAutomationComponent.markNeighborUnsaved(level, pos); }
    });

    public SteamTunnelFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.STEAM_TUNNEL_FLUID_PORT_ENTITY_TYPE.get(), pos, state);
        itemHandler.setValidator((slot, stack, slotType) -> mio_icif_reactor_fluid_port.isSuitablePlugin(stack));
    }

    @Override public MachineItemHandler getItemHandler() { return itemHandler; }

    @Override public boolean hasPlugin() { return !itemHandler.getStackInSlot(0).isEmpty(); }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorFluidPortMenu(containerId, playerInventory, this);
    }

    @Override public void link(BlockPos controller) { this.controller = controller.immutable(); setChanged(); }

    @Override public void unlink(BlockPos controller) { if (controller.equals(this.controller)) { this.controller = null; setChanged(); } }

    /** The formed tunnel this port serves, or null. */
    public @Nullable SteamTunnelBlockEntity tunnel() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof SteamTunnelBlockEntity tunnel && tunnel.serves(worldPosition) ? tunnel : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamTunnelFluidPortBlockEntity port) {
        if (level.isClientSide) return;
        port.upgradeStats = MachineUpgradeStats.fromInventory(port.itemHandler, 0, 1);
        if (port.upgradeStats.getFluidEjectorCount() > 0 || port.upgradeStats.getFluidPullingCount() > 0) port.fluidAutomation.runAutomation();
        if (level.getGameTime() % PUSH_TICKS != 0) return;
        SteamTunnelBlockEntity tunnel = port.tunnel();
        if (tunnel == null || tunnel.water().isEmpty()) return;
        for (Direction side : Direction.values()) {
            IFluidHandler target = port.outside(pos.relative(side), side.getOpposite());
            if (target == null) continue;
            FluidStack offer = tunnel.drain(PUSH_AMOUNT, FluidAction.SIMULATE);
            if (offer.isEmpty()) return;
            int taken = target.fill(offer, FluidAction.EXECUTE);
            if (taken > 0) tunnel.drain(taken, FluidAction.EXECUTE);
        }
    }

    /** The fluid handler of the block at {@code pos} next to this port, unless it is part of a tunnel. */
    private @Nullable IFluidHandler outside(BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.isLoaded(pos)) return null;
        BlockEntity there = level.getBlockEntity(pos);
        if (there instanceof SteamTunnelBlockEntity.SteamTunnelPart || there instanceof SteamTunnelBlockEntity) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
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
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        fluidAutomation.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        fluidAutomation.load(tag, registries);
    }
}
