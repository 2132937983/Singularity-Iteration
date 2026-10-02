package com.miophas.singularity_iteration.common.blockentity.wiring.terminal;

import com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_energy_terminal;
import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
import com.miophas.singularity_iteration.common.menu.wiring.EnergyTerminalMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile;
import com.miophas.singularity_iteration.core.api.energy.IRemoteSwitchable;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Energy Management Terminal: monitors the cable network it is attached to.
 *
 * <p>Every {@link #RESCAN_WINDOWS} seconds a bounded breadth-first walk over the
 * attached conductors (respecting blocked faces and cable colours) collects the
 * endpoints on the network. Every tick the engine's per-node samples of those
 * endpoints are accumulated; once per second they become EU/t averages, network
 * totals (generation, consumption, storage flow and charge) and a rolling history.
 * The terminal is passive: it draws no power and adds no load to the grid.
 *
 * <p>Endpoints implementing {@link IRemoteSwitchable} can be switched off from the
 * GUI, which cuts them from the grid (no input, no output, machines stop).
 */
@SuppressWarnings("null")
public class EnergyTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int WINDOW = 20;            // ticks per sample
    public static final int RESCAN_WINDOWS = 5;     // topology walk every 5 s
    public static final int MAX_CONDUCTORS = 8192;

    private final List<BlockPos> devicePos = new ArrayList<>();
    private final List<EnergyNetworkSnapshot.Category> deviceCat = new ArrayList<>();
    private final List<Integer> deviceSubnet = new ArrayList<>();
    private List<NetworkWalker.Subnet> subnets = new ArrayList<>();
    private double[] accIn = new double[0], accOut = new double[0], accV = new double[0];
    /** Global mode: the walk crosses transformers and reports the whole multi-voltage system. */
    private boolean globalMode;
    private int conductors;
    private boolean truncated;
    private int windowTick, windowCount;
    private boolean needsRescan = true;
    private final float[] genHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private final float[] useHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private final float[] chargeHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private EnergyNetworkSnapshot latest = new EnergyNetworkSnapshot();
    private int version;

    public EnergyTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.ENERGY_TERMINAL.get(), pos, state);
    }

    public EnergyNetworkSnapshot snapshot() { return latest; }
    public int version() { return version; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.energy_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EnergyTerminalMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------ server tick
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyTerminalBlockEntity terminal) {
        if (!(level instanceof ServerLevel server)) return;
        terminal.tickServer(server, state);
    }

    private void tickServer(ServerLevel level, BlockState state) {
        if (needsRescan) {
            rescan(level);
            needsRescan = false;
        }
        IndependentSiEnergy engine = IndependentSiEnergy.current(level.getServer());
        if (engine != null) {
            for (int i = 0; i < devicePos.size(); i++) {
                NodeStats stats = engine.nodeStats(level, devicePos.get(i));
                if (stats != null) {
                    accIn[i] += stats.energyIn();
                    accOut[i] += stats.energyOut();
                    accV[i] = Math.max(accV[i], stats.voltage());
                }
            }
        }
        if (++windowTick < WINDOW) return;
        windowTick = 0;
        publish(level);
        if (++windowCount >= RESCAN_WINDOWS) {
            windowCount = 0;
            needsRescan = true;
        }
        boolean active = !devicePos.isEmpty();
        if (state.hasProperty(mio_icif_block_energy_terminal.ACTIVE) && state.getValue(mio_icif_block_energy_terminal.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(mio_icif_block_energy_terminal.ACTIVE, active), 3);
        }
    }

    /** Walks the attached conductors (and, in global mode, across transformers) and records every endpoint reached. */
    private void rescan(ServerLevel level) {
        NetworkWalker.Result r = NetworkWalker.walk(level, worldPosition, globalMode, MAX_CONDUCTORS, EnergyNetworkSnapshot.MAX_DEVICES);
        devicePos.clear();
        deviceCat.clear();
        deviceSubnet.clear();
        devicePos.addAll(r.devices);
        deviceCat.addAll(r.categories);
        deviceSubnet.addAll(r.deviceSubnet);
        subnets = r.subnets;
        conductors = r.conductors;
        truncated = r.truncated;
        accIn = new double[devicePos.size()];
        accOut = new double[devicePos.size()];
        accV = new double[devicePos.size()];
    }

    public boolean globalMode() { return globalMode; }

    /** GUI request: switch between this sub-network and the whole system across transformers. */
    public void setGlobalMode(boolean global) {
        if (globalMode == global) return;
        globalMode = global;
        needsRescan = true;
        windowTick = WINDOW - 1;          // publish on the next tick
        setChanged();
    }

    /** Turns the accumulated window into averages, totals and history. */
    private void publish(ServerLevel level) {
        EnergyNetworkSnapshot s = new EnergyNetworkSnapshot();
        s.global = globalMode;
        for (NetworkWalker.Subnet n : subnets) {
            EnergyNetworkSnapshot.Subnet out = new EnergyNetworkSnapshot.Subnet();
            out.conductors = n.conductors;
            out.ratedPacket = n.ratedPacket;
            s.subnets.add(out);
        }
        long storageStored = 0, storageCap = 0, bufferStored = 0, bufferCap = 0;
        boolean hasStorage = false;
        for (int i = 0; i < devicePos.size(); i++) {
            BlockPos at = devicePos.get(i);
            BlockEntity be = level.isLoaded(at) ? level.getBlockEntity(at) : null;
            if (be == null || be.isRemoved()) continue;
            EnergyNetworkSnapshot.Category cat = deviceCat.get(i);
            float in = (float) (accIn[i] / WINDOW), out = (float) (accOut[i] / WINDOW);
            long stored = 0, cap = 0;
            if (be instanceof AbstractEnergyBlockEntity energy) {
                stored = energy.getEnergyStorage().getAmount();
                cap = energy.getEnergyStorage().getCapacity();
            }
            boolean switchable = be instanceof IRemoteSwitchable;
            boolean disabled = switchable && ((IRemoteSwitchable) be).isRemotelyDisabled();
            float rated = rated(be, cat, in, out, disabled);
            int subnetIndex = i < deviceSubnet.size() ? deviceSubnet.get(i) : -1;
            EnergyNetworkSnapshot.Subnet sub = subnetIndex >= 0 && subnetIndex < s.subnets.size() ? s.subnets.get(subnetIndex) : null;
            float voltage = i < accV.length ? (float) accV[i] : 0F;
            switch (cat) {
                case GENERATOR -> { s.generation += out; s.generationCapacity += rated;
                    if (sub != null) { sub.generation += out; sub.generationCapacity += rated; } }
                case CONSUMER -> { s.consumption += in; s.demand += rated;
                    if (sub != null) { sub.consumption += in; sub.demand += rated; } }
                case STORAGE -> { s.storageIn += in; s.storageOut += out; storageStored += stored; storageCap += cap; hasStorage = true;
                    if (sub != null) { sub.storageIn += in; sub.storageOut += out; } }
                default -> { }
            }
            if (sub != null) { sub.devices++; sub.measuredPacket = Math.max(sub.measuredPacket, voltage); }
            bufferStored += stored;
            bufferCap += cap;
            s.devices.add(new EnergyNetworkSnapshot.Device(at, BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString(),
                cat, in, out, stored, cap, switchable, disabled, rated, subnetIndex, voltage));
        }
        java.util.Arrays.fill(accIn, 0);
        java.util.Arrays.fill(accOut, 0);
        java.util.Arrays.fill(accV, 0);
        s.stored = hasStorage ? storageStored : bufferStored;
        s.capacity = hasStorage ? storageCap : bufferCap;
        s.conductors = conductors;
        s.truncated = truncated;
        shift(genHistory, s.generation);
        shift(useHistory, s.consumption);
        shift(chargeHistory, s.chargeFraction());
        System.arraycopy(genHistory, 0, s.genHistory, 0, genHistory.length);
        System.arraycopy(useHistory, 0, s.useHistory, 0, useHistory.length);
        System.arraycopy(chargeHistory, 0, s.chargeHistory, 0, chargeHistory.length);
        latest = s;
        version++;
    }

    /**
     * Theoretical EU/t of a device: a generator's nameplate output, a processing machine's
     * draw while working (upgrades included). Never below what was actually measured.
     */
    private static float rated(BlockEntity be, EnergyNetworkSnapshot.Category cat, float in, float out, boolean disabled) {
        if (disabled) return 0F;
        try {
            if (cat == EnergyNetworkSnapshot.Category.GENERATOR) {
                float nameplate = be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity gen
                    ? gen.getEnergyGenerationRate() : 0F;
                return Math.max(nameplate, out);
            }
            if (cat == EnergyNetworkSnapshot.Category.CONSUMER) {
                // a machine requests power while it works, or while its buffer is starved (it would work if fed);
                // an idle machine with a full buffer requests nothing, so idle machines never read as "unmet demand"
                if (be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity machine) {
                    float draw = machine.getEffectiveEnergyPerTick();
                    long stored = machine.getEnergyStorage().getAmount();
                    boolean requesting = machine.isWorking() || stored < Math.max(1F, draw) * 4;
                    return requesting ? Math.max(draw, in) : in;
                }
                return in;
            }
        } catch (RuntimeException ignored) {
            // telemetry must never break the terminal
        }
        return Math.max(in, out);
    }

    private static void shift(float[] history, float value) {
        System.arraycopy(history, 1, history, 0, history.length - 1);
        history[history.length - 1] = value;
    }

    /** GUI request: flip the remote switch of a device that is on this network. */
    public boolean toggle(BlockPos target) {
        if (!(level instanceof ServerLevel server) || !devicePos.contains(target) || !server.isLoaded(target)) return false;
        if (!(server.getBlockEntity(target) instanceof IRemoteSwitchable switchable)) return false;
        switchable.setRemotelyDisabled(!switchable.isRemotelyDisabled());
        server.playSound(null, worldPosition, mio_icif_sounds.UI_TOGGLE.get(), SoundSource.BLOCKS, 0.6F,
            switchable.isRemotelyDisabled() ? 0.8F : 1.2F);
        // reflect the new state right away instead of waiting for the next window
        List<EnergyNetworkSnapshot.Device> rows = latest.devices;
        for (int i = 0; i < rows.size(); i++) {
            var d = rows.get(i);
            if (d.pos().equals(target)) {
                rows.set(i, new EnergyNetworkSnapshot.Device(d.pos(), d.blockId(), d.category(), d.input(), d.output(),
                    d.stored(), d.capacity(), true, switchable.isRemotelyDisabled(), d.rated(), d.subnet(), d.voltage()));
            }
        }
        version++;
        return true;
    }

    /** Neighbour or cable change near the terminal: walk the network again soon. */
    public void invalidateTopology() {
        needsRescan = true;
    }

    // ------------------------------------------------------------------ client tick (ambient hum)
    /** Client-side state for the ambient hum, driven by {@code EnergyTerminalClient}. */
    public int clientAmbientTimer;

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Global", globalMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        globalMode = tag.getBoolean("Global");
        needsRescan = true;
    }
}
