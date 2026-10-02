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
    private double[] accIn = new double[0], accOut = new double[0];
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

    /** Walks the attached conductors and records every endpoint reached. */
    private void rescan(ServerLevel level) {
        devicePos.clear();
        deviceCat.clear();
        conductors = 0;
        truncated = false;
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet devices = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition.asLong());
        for (Direction side : Direction.values()) consider(level, null, worldPosition, side, visited, devices, queue);
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            if (!(level.getBlockEntity(at) instanceof ICableEnergyNode cable)) continue;
            for (Direction side : Direction.values()) {
                if (cable.isDirectionBlocked(side)) continue;
                consider(level, (BlockEntity) cable, at, side, visited, devices, queue);
            }
        }
        accIn = new double[devicePos.size()];
        accOut = new double[devicePos.size()];
    }

    private void consider(ServerLevel level, @Nullable BlockEntity from, BlockPos at, Direction side,
                          LongOpenHashSet visited, LongOpenHashSet devices, ArrayDeque<BlockPos> queue) {
        BlockPos next = at.relative(side);
        long key = next.asLong();
        if (visited.contains(key) || devices.contains(key)) return;
        if (!level.isLoaded(next)) return;
        BlockEntity be = level.getBlockEntity(next);
        if (be == null) return;
        if (be instanceof ICableEnergyNode cable) {
            // The terminal taps the cable physically, so the face toward the terminal need not be open.
            if (from != null && cable.isDirectionBlocked(side.getOpposite())) return;   // may still be reached from another side
            if (from != null && (from instanceof IColoredEnergyTile || be instanceof IColoredEnergyTile)
                    && !IColoredEnergyTile.connects(from, be, side)) return;
            if (conductors >= MAX_CONDUCTORS) { truncated = true; return; }
            visited.add(key);
            conductors++;
            queue.add(next.immutable());
            return;
        }
        if (from == null) return;                 // only cables attach the terminal to a network
        EnergyNetworkSnapshot.Category category = categorize(be);
        if (category == null) return;
        if (devicePos.size() >= EnergyNetworkSnapshot.MAX_DEVICES) { truncated = true; return; }
        devices.add(key);
        devicePos.add(next.immutable());
        deviceCat.add(category);
    }

    @Nullable
    private static EnergyNetworkSnapshot.Category categorize(BlockEntity be) {
        if (be instanceof EnergyTerminalBlockEntity) return null;
        if (be instanceof AbstractEnergyStorageBlockEntity) return EnergyNetworkSnapshot.Category.STORAGE;
        if (be instanceof mio_icif_transformer) return EnergyNetworkSnapshot.Category.TRANSFORMER;
        if (be instanceof DemandEnergySource) return EnergyNetworkSnapshot.Category.GENERATOR;
        if (be instanceof AbstractEnergyBlockEntity machine) {
            return machine.isPowerSource() ? EnergyNetworkSnapshot.Category.GENERATOR : EnergyNetworkSnapshot.Category.CONSUMER;
        }
        if (be instanceof EnergyComponentHost) return EnergyNetworkSnapshot.Category.CONSUMER;
        return null;
    }

    /** Turns the accumulated window into averages, totals and history. */
    private void publish(ServerLevel level) {
        EnergyNetworkSnapshot s = new EnergyNetworkSnapshot();
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
            switch (cat) {
                case GENERATOR -> s.generation += out;
                case CONSUMER -> s.consumption += in;
                case STORAGE -> { s.storageIn += in; s.storageOut += out; storageStored += stored; storageCap += cap; hasStorage = true; }
                default -> { }
            }
            bufferStored += stored;
            bufferCap += cap;
            s.devices.add(new EnergyNetworkSnapshot.Device(at, BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString(),
                cat, in, out, stored, cap, switchable, disabled));
        }
        java.util.Arrays.fill(accIn, 0);
        java.util.Arrays.fill(accOut, 0);
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
                    d.stored(), d.capacity(), true, switchable.isRemotelyDisabled()));
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        needsRescan = true;
    }
}
