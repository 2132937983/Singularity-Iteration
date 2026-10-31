package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: a power port of the Steam Tunnel. While its tunnel is formed it takes EU from the controller's buffer and sends it
 * as HV packets of 512 EU, up to 4 a tick (2,048 EU/t, the tunnel's full output); otherwise it takes and sends nothing. IC2 Classic
 * sent from the casings themselves; SI's energy network needs a block entity to send from.
 */
public class SteamTunnelPowerPortBlockEntity extends AbstractEnergyBlockEntity implements SteamTunnelBlockEntity.SteamTunnelPart {
    public static final long PACKET = 512L, PACKETS = 4L, RATE = PACKET * PACKETS, CAPACITY = 2 * RATE;
    private @Nullable BlockPos controller;
    private boolean sourceRole;

    public SteamTunnelPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.STEAM_TUNNEL_POWER_PORT_ENTITY_TYPE.get(), CAPACITY, 0, 0, CableTier.HV);
    }

    @Override public void link(BlockPos controller) { this.controller = controller.immutable(); setChanged(); }

    @Override public void unlink(BlockPos controller) { if (controller.equals(this.controller)) { this.controller = null; setChanged(); } }

    public @Nullable SteamTunnelBlockEntity tunnel() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof SteamTunnelBlockEntity tunnel && tunnel.serves(worldPosition) ? tunnel : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamTunnelPowerPortBlockEntity port) {
        if (level.isClientSide) return;
        SteamTunnelBlockEntity tunnel = port.tunnel();
        boolean formed = tunnel != null;
        if (formed != port.sourceRole) {
            port.sourceRole = formed;
            if (formed) port.setAsPowerSource(PACKET);
            else port.setAsConsumer();
            port.getEnergyStorageInternal().setMaxExtract(formed ? RATE : 0);
            port.setChanged();
            IndependentSiEnergy.changed(port);   // republish its faces: source <-> nothing
        } else if (formed && !port.isPowerSource()) {
            port.setAsPowerSource(PACKET);       // restored from a save
        }
        if (!formed) return;
        long space = CAPACITY - port.apiGetStoredEnergy();
        if (space <= 0) return;
        long taken = tunnel.takeEnergy(Math.min(space, RATE));
        if (taken <= 0) return;
        long stored = port.apiGenerateEnergy(taken, false);
        if (stored < taken) tunnel.returnEnergy(taken - stored);   // never lost
        port.setChanged();
    }

    /** Up to 4 HV packets a tick (the reactor does the same with IV packets). */
    @Override public int getPacketCount() { return (int) PACKETS; }

    // ---- legacy grid API (used only when the independent engine is disabled)
    @Override public double getOfferedEnergy() { return isPowerSource() ? Math.min(RATE, apiGetStoredEnergy()) : 0.0D; }
    @Override public void drawEnergy(double amount) { if (isPowerSource() && amount > 0) apiUseEnergy((long) amount, false); }
    @Override public int getSourceTier() { return isPowerSource() ? EnergyNetGlobal.cableTierToSourceTier(CableTier.HV) : -1; }
    @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) { return isPowerSource(); }
    @Override public double getDemandedEnergy() { return 0.0D; }
    @Override public double injectEnergy(Direction direction, double amount, double voltage) { return amount; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) { return false; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) tag.put("Controller", NbtUtils.writeBlockPos(controller));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
    }
}
