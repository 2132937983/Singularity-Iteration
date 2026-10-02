package com.miophas.singularity_iteration.common.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side of the front status lamps. Loaded machines register themselves
 * ({@link AbstractEnergyBlockEntity#STATUS_LIFECYCLE}); every {@link #INTERVAL} ticks their status
 * is re-evaluated and only changes are sent, batched per chunk, to the players tracking it. A
 * player who starts watching a chunk gets its non-dark lamps at once. Clients re-mesh a block only
 * when its lamp actually changes, so a running factory costs no rendering work.
 *
 * <p>Short gaps between two operations do not flicker red: a machine that was running stays green
 * for {@link #HOLD} ticks unless it reports a power problem.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class MachineStatusTracker {
    private MachineStatusTracker() {}

    public static final int INTERVAL = 10, HOLD = 30;

    private static final class Entry {
        MachineStatus sent = MachineStatus.OFF;
        long lastRunning = Long.MIN_VALUE / 2;
    }

    /** level -> chunk -> machine -> entry (server thread only). */
    private static final Map<ServerLevel, Map<Long, Map<AbstractEnergyBlockEntity, Entry>>> TRACKED = new IdentityHashMap<>();

    static {
        install();
    }

    static void install() {
        AbstractEnergyBlockEntity.STATUS_LIFECYCLE = MachineStatusTracker::lifecycle;
    }

    @SubscribeEvent
    static void serverStarting(ServerStartingEvent event) {
        install();
        TRACKED.clear();
    }

    private static void lifecycle(AbstractEnergyBlockEntity machine, Boolean loaded) {
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        long chunk = ChunkPos.asLong(machine.getBlockPos());
        if (loaded) {
            TRACKED.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(chunk, c -> new IdentityHashMap<>())
                .putIfAbsent(machine, new Entry());
        } else {
            var chunks = TRACKED.get(level);
            if (chunks == null) return;
            var inChunk = chunks.get(chunk);
            if (inChunk == null) return;
            inChunk.remove(machine);
            if (inChunk.isEmpty()) chunks.remove(chunk);
        }
    }

    /** Current (held) status of a tracked machine, or null when untracked. For tests / tools. */
    public static MachineStatus sentStatus(AbstractEnergyBlockEntity machine) {
        if (!(machine.getLevel() instanceof ServerLevel level)) return null;
        var chunks = TRACKED.get(level);
        var inChunk = chunks == null ? null : chunks.get(ChunkPos.asLong(machine.getBlockPos()));
        Entry e = inChunk == null ? null : inChunk.get(machine);
        return e == null ? null : e.sent;
    }

    @SubscribeEvent
    static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now % INTERVAL != 0) return;
        var chunks = TRACKED.get(level);
        if (chunks == null || chunks.isEmpty()) return;
        for (var chunkEntry : new ArrayList<>(chunks.entrySet())) {
            List<Long> pos = null; List<Byte> st = null;
            for (var e : new ArrayList<>(chunkEntry.getValue().entrySet())) {
                AbstractEnergyBlockEntity machine = e.getKey();
                if (machine.isRemoved()) { chunkEntry.getValue().remove(machine); continue; }
                // never load a chunk for a lamp: the redstone check reads neighbour blocks
                var p = machine.getBlockPos();
                if (!level.hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 1, 1))) continue;
                MachineStatus status;
                try {
                    status = machine.machineStatus();
                } catch (RuntimeException ex) {
                    continue;                                   // a lamp must never break a machine
                }
                Entry entry = e.getValue();
                if (status == MachineStatus.RUNNING) entry.lastRunning = now;
                else if (entry.sent == MachineStatus.RUNNING && status == MachineStatus.BLOCKED && now - entry.lastRunning < HOLD) {
                    status = MachineStatus.RUNNING;             // between two operations
                }
                if (status == entry.sent) continue;
                entry.sent = status;
                if (pos == null) { pos = new ArrayList<>(); st = new ArrayList<>(); }
                pos.add(machine.getBlockPos().asLong());
                st.add((byte) status.ordinal());
            }
            if (pos != null) send(level, new ChunkPos(chunkEntry.getKey()), pos, st, null);
            if (chunkEntry.getValue().isEmpty()) chunks.remove(chunkEntry.getKey());
        }
    }

    @SubscribeEvent
    static void chunkWatched(ChunkWatchEvent.Sent event) {
        var chunks = TRACKED.get(event.getLevel());
        var inChunk = chunks == null ? null : chunks.get(event.getPos().toLong());
        if (inChunk == null || inChunk.isEmpty()) return;
        List<Long> pos = new ArrayList<>(); List<Byte> st = new ArrayList<>();
        for (var e : inChunk.entrySet()) {
            if (e.getValue().sent == MachineStatus.OFF) continue;
            pos.add(e.getKey().getBlockPos().asLong());
            st.add((byte) e.getValue().sent.ordinal());
        }
        if (!pos.isEmpty()) send(event.getLevel(), event.getPos(), pos, st, event.getPlayer());
    }

    private static void send(ServerLevel level, ChunkPos chunk, List<Long> pos, List<Byte> st, net.minecraft.server.level.ServerPlayer only) {
        long[] p = new long[pos.size()]; byte[] s = new byte[st.size()];
        for (int i = 0; i < p.length; i++) { p[i] = pos.get(i); s[i] = st.get(i); }
        var packet = new MachineStatusPacket(p, s);
        try {
            if (only != null) {
                if (only.connection != null && only.connection.hasChannel(MachineStatusPacket.TYPE)) PacketDistributor.sendToPlayer(only, packet);
            } else {
                PacketDistributor.sendToPlayersTrackingChunk(level, chunk, packet);
            }
        } catch (RuntimeException ignored) {
            // fake / detached connections
        }
    }
}
