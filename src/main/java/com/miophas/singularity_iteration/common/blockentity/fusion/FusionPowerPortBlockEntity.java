package com.miophas.singularity_iteration.common.blockentity.fusion;

import com.miophas.singularity_iteration.common.fusion.FusionPlasma;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: a power output port of the fusion reactor. While its controller is formed it takes its share of the output (the
 * output divided among the power output ports) and sends it in up to 64 packets a tick, of the lowest voltage that carries the
 * share: IV (8,192 EU) up to 524,288 EU/t, then LuV, ZPMV, UV, and UHV (2,097,152 EU) for one port at the full 39,200,000 EU/t.
 * Its tier follows the share as the kinetic generators' and the fission reactor's do (a dynamic-tier generator in
 * BuiltinEnergyProfiles): cables and machines on it must take that voltage (the superconducting cable takes any).
 */
public class FusionPowerPortBlockEntity extends AbstractEnergyBlockEntity implements FusionControllerBlockEntity.FusionPart {
    public static final int MIN_TIER = 5, MAX_TIER = 9;   // IV .. UHV
    public static final long CAPACITY = FusionPlasma.OUTPUT_CAP;
    private @Nullable BlockPos controller;
    private boolean sourceRole;
    private int tier = MIN_TIER;
    private long rate;

    public FusionPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.FUSION_POWER_PORT_ENTITY_TYPE.get(), CAPACITY, 0, 0, CableTier.UHV);
    }

    /** The packet of a source tier: 8 x 4^tier EU. */
    public static long packet(int tier) { return 8L << (2 * tier); }

    /** The lowest tier (IV at least) whose 64 packets carry {@code perTick}. */
    public static int tierFor(long perTick) {
        int t = MIN_TIER;
        while (t < MAX_TIER && packet(t) * EnergyPacketPolicy.MAX_PACKETS_PER_ROUND < perTick) t++;
        return t;
    }

    @Override public void link(BlockPos controller) { this.controller = controller.immutable(); setChanged(); }

    @Override public void unlink(BlockPos controller) { if (controller.equals(this.controller)) { this.controller = null; setChanged(); } }

    public @Nullable FusionControllerBlockEntity controller() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof FusionControllerBlockEntity c && c.serves(worldPosition) ? c : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionPowerPortBlockEntity port) {
        if (level.isClientSide) return;
        FusionControllerBlockEntity c = port.controller();
        boolean formed = c != null;
        long share = formed ? c.portShare() : 0;
        boolean source = formed || port.apiGetStoredEnergy() > 0;   // an unformed port still sends out what it holds
        long rate = Math.max(1, formed ? share : port.apiGetStoredEnergy());
        int tier = tierFor(rate);
        if (source != port.sourceRole) {
            port.sourceRole = source;
            if (source) port.setAsPowerSource(packet(tier));
            else port.setAsConsumer();
            port.getEnergyStorageInternal().setMaxExtract(source ? rate : 0);
            port.rate = rate;
            port.tier = tier;
            port.setChanged();
            IndependentSiEnergy.changed(port);
        } else if (source && !port.isPowerSource()) {
            port.setAsPowerSource(packet(tier));   // restored from a save
        }
        if (source && (rate != port.rate || tier != port.tier)) {
            port.rate = rate;
            port.tier = tier;
            port.getEnergyStorageInternal().setMaxExtract(rate);
        }
        if (!formed) return;
        long space = CAPACITY - port.apiGetStoredEnergy();
        if (space <= 0 || share <= 0) return;
        long taken = c.takeEnergy(Math.min(space, share));
        if (taken <= 0) return;
        long stored = port.apiGenerateEnergy(taken, false);
        if (stored < taken) c.returnEnergy(taken - stored);
        port.setChanged();
    }

    public long rate() { return rate; }

    @Override public int getPacketCount() { return EnergyPacketPolicy.MAX_PACKETS_PER_ROUND; }

    @Override public int getSourceTier() { return isPowerSource() ? tier : -1; }

    // ---- legacy grid API (used only when the independent engine is disabled)
    @Override public double getOfferedEnergy() { return isPowerSource() ? Math.min(rate, apiGetStoredEnergy()) : 0.0D; }
    @Override public void drawEnergy(double amount) { if (isPowerSource() && amount > 0) apiUseEnergy((long) amount, false); }
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
