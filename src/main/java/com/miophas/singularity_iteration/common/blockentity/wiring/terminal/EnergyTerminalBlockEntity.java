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
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
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
    private boolean globalMode = true;
    private int conductors;
    private boolean truncated;
    private int windowTick, windowCount;
    private boolean needsRescan = true;
    private final float[] genHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private final float[] useHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private final float[] chargeHistory = new float[EnergyNetworkSnapshot.HISTORY];
    /** 0.1.10.22 (G7): engine cost history, the walk's links and bounding box, the cost counters of the last window. */
    private final float[] costHistory = new float[EnergyNetworkSnapshot.HISTORY];
    private List<int[]> links = new ArrayList<>();
    @Nullable private NetworkWalker.Result lastWalk;
    private long[] lastNanos = new long[0];
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
        NetworkWalker.Result r = NetworkWalker.walk(level, worldPosition,
            globalMode ? NetworkWalker.Scope.SYSTEM : NetworkWalker.Scope.SEGMENT, MAX_CONDUCTORS, EnergyNetworkSnapshot.MAX_DEVICES);
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
        links = r.links;
        lastWalk = r;
        lastNanos = new long[devicePos.size()];
        IndependentSiEnergy engine = IndependentSiEnergy.current(level.getServer());
        if (engine != null) for (int i = 0; i < devicePos.size(); i++) lastNanos[i] = engine.settleNanos(level, devicePos.get(i));
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
        it.unimi.dsi.fastutil.longs.LongOpenHashSet countedOwners = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (int i = 0; i < devicePos.size(); i++) {
            BlockPos at = devicePos.get(i);
            BlockEntity be = level.isLoaded(at) ? level.getBlockEntity(at) : null;
            if (be == null || be.isRemoved()) continue;
            EnergyNetworkSnapshot.Category cat = deviceCat.get(i);
            float in = (float) (accIn[i] / WINDOW), out = (float) (accOut[i] / WINDOW);
            long stored = 0, cap = 0;
            // storage ports report their multiblock core's store, counted once per core
            BlockEntity owner = cat == EnergyNetworkSnapshot.Category.STORAGE ? NetworkWalker.storageOwner(be) : be;
            if (owner instanceof AbstractEnergyBlockEntity energy) {
                stored = energy.getEnergyStorage().getAmount();
                cap = energy.getEnergyStorage().getCapacity();
            }
            boolean firstOfOwner = owner == null || countedOwners.add(owner.getBlockPos().asLong());
            boolean switchable = be instanceof IRemoteSwitchable;
            boolean disabled = switchable && ((IRemoteSwitchable) be).isRemotelyDisabled();
            float rated = rated(be, cat, in, out, disabled);
            int subnetIndex = i < deviceSubnet.size() ? deviceSubnet.get(i) : -1;
            EnergyNetworkSnapshot.Subnet sub = subnetIndex >= 0 && subnetIndex < s.subnets.size() ? s.subnets.get(subnetIndex) : null;
            float voltage = i < accV.length ? (float) accV[i] : 0F;
            // 0.1.10.22 (G3 / G4): dispatch class of generators, priority of loads and storage
            int dispatch = -1, priority = -1;
            if (cat == EnergyNetworkSnapshot.Category.GENERATOR) {
                boolean fuel = be instanceof com.miophas.singularity_iteration.core.api.energy.grid.IGridDispatch g
                    && g.dispatchClass() == com.miophas.singularity_iteration.core.api.energy.grid.DispatchClass.DISPATCHABLE;
                dispatch = fuel ? 1 : 0;
                if (fuel) s.generationFuel += out; else s.generationMustRun += out;
            }
            if ((cat == EnergyNetworkSnapshot.Category.CONSUMER || cat == EnergyNetworkSnapshot.Category.STORAGE)
                    && be instanceof com.miophas.singularity_iteration.core.api.energy.grid.ILoadPriority p) {
                priority = p.loadPriority().ordinal();
                if (priority == 0 && cat == EnergyNetworkSnapshot.Category.CONSUMER && !disabled) {
                    s.criticalDemand += rated;
                    float missing = Math.max(0F, rated - in);
                    // more than 5 % and 1 EU/t short: the critical load did not get its energy
                    if (missing > 1F && missing > rated * 0.05F) {
                        s.criticalShortfall += missing;
                        com.miophas.singularity_iteration.core.runtime.energy.GridAlarms.shortfall(level, at, missing);
                    }
                }
            }
            if (cat == EnergyNetworkSnapshot.Category.PROTECTION && be instanceof IndependentSpecialCableBlockEntity cable
                    && cable.protective() && s.breakers.size() < EnergyNetworkSnapshot.MAX_BREAKERS) {
                var relay = cable.relay();
                s.breakers.add(new EnergyNetworkSnapshot.Breaker(at, relay.kind().ordinal(), relay.state().ordinal(), relay.lastCause().ordinal(),
                    (float) relay.lastFlow(), relay.ratedFlow(), relay.ratedVoltage(), relay.delay(), relay.autoReclose(), relay.recloseIn(),
                    relay.recentTrips(), (float) relay.heat()));
            }
            switch (cat) {
                case GENERATOR -> { s.generation += out; s.generationCapacity += rated;
                    if (sub != null) { sub.generation += out; sub.generationCapacity += rated; } }
                case CONSUMER -> { s.consumption += in; s.demand += rated;
                    if (sub != null) { sub.consumption += in; sub.demand += rated; } }
                case STORAGE -> { s.storageIn += in; s.storageOut += out; if (firstOfOwner) { storageStored += stored; storageCap += cap; } hasStorage = true;
                    if (sub != null) { sub.storageIn += in; sub.storageOut += out; } }
                default -> { }
            }
            if (sub != null) { sub.devices++; sub.measuredPacket = Math.max(sub.measuredPacket, voltage); }
            if (firstOfOwner) { bufferStored += stored; bufferCap += cap; }
            s.devices.add(new EnergyNetworkSnapshot.Device(at, BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString(),
                cat, in, out, stored, cap, switchable, disabled, rated, subnetIndex, voltage, priority, dispatch));
        }
        // 0.1.10.22 (G7): links of the diagram, alarms near this network, engine cost of its windows
        for (int[] link : links) if (link[0] < devicePos.size()) s.links.add(new EnergyNetworkSnapshot.Link(link[0], link[1]));
        var walk = lastWalk;
        if (walk != null) {
            var all = com.miophas.singularity_iteration.core.runtime.energy.GridAlarms.recent(level);
            for (int i = all.size() - 1; i >= 0 && s.alarms.size() < EnergyNetworkSnapshot.MAX_ALARMS; i--) {
                var alarm = all.get(i);
                BlockPos at = BlockPos.of(alarm.pos());
                if (!walk.near(at, 2)) continue;
                s.alarms.add(new EnergyNetworkSnapshot.Alarm(alarm.time(), alarm.type().ordinal(), at, alarm.detail(), (float) alarm.value()));
            }
        }
        IndependentSiEnergy costEngine = IndependentSiEnergy.current(level.getServer());
        long nanos = 0;
        if (costEngine != null && lastNanos.length == devicePos.size()) {
            for (int i = 0; i < devicePos.size(); i++) {
                long now = costEngine.settleNanos(level, devicePos.get(i));
                if (now >= lastNanos[i]) nanos += now - lastNanos[i];
                lastNanos[i] = now;
            }
        }
        s.costMicros = nanos / 1000F / WINDOW;
        shift(costHistory, s.costMicros);
        System.arraycopy(costHistory, 0, s.costHistory, 0, costHistory.length);
        s.gameTime = level.getGameTime();
        s.dispatchMode = com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.dispatchMode().ordinal();
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
                    d.stored(), d.capacity(), true, switchable.isRemotelyDisabled(), d.rated(), d.subnet(), d.voltage(), d.priority(), d.dispatch()));
            }
        }
        version++;
        return true;
    }

    /** 0.1.10.22 (G4): GUI request: the load priority of a consumer or storage block on this network. */
    public boolean setPriority(BlockPos target, int priority) {
        if (!(level instanceof ServerLevel server) || !devicePos.contains(target) || !server.isLoaded(target)) return false;
        if (priority < 0 || priority >= com.miophas.singularity_iteration.core.api.energy.grid.LoadPriority.values().length) return false;
        if (!(server.getBlockEntity(target) instanceof com.miophas.singularity_iteration.core.api.energy.grid.ILoadPriority prioritized)) return false;
        var value = com.miophas.singularity_iteration.core.api.energy.grid.LoadPriority.values()[priority];
        prioritized.setLoadPriority(value);
        server.playSound(null, worldPosition, mio_icif_sounds.UI_TOGGLE.get(), SoundSource.BLOCKS, 0.6F, 1.0F + 0.2F * (1 - priority));
        List<EnergyNetworkSnapshot.Device> rows = latest.devices;
        for (int i = 0; i < rows.size(); i++) {
            var d = rows.get(i);
            if (d.pos().equals(target)) rows.set(i, new EnergyNetworkSnapshot.Device(d.pos(), d.blockId(), d.category(), d.input(), d.output(),
                d.stored(), d.capacity(), d.switchable(), d.disabled(), d.rated(), d.subnet(), d.voltage(), priority, d.dispatch()));
        }
        version++;
        return true;
    }

    /** 0.1.10.22 (G5): GUI request: close an open breaker of this network (a fuse needs a new fuse item). */
    public boolean resetProtection(BlockPos target) {
        if (!(level instanceof ServerLevel server) || !devicePos.contains(target) || !server.isLoaded(target)) return false;
        if (!(server.getBlockEntity(target) instanceof IndependentSpecialCableBlockEntity cable) || !cable.protective()
                || cable.relay().kind() != com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay.Kind.BREAKER) return false;
        boolean changed = cable.resetProtection();
        if (changed) { windowTick = WINDOW - 1; version++; }
        return changed;
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
        globalMode = !tag.contains("Global") || tag.getBoolean("Global");
        needsRescan = true;
    }
}
