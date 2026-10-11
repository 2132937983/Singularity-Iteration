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
 * Server side of the front status lamps. It sends {@link MachineStatusPacket} (server to
 * client).
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>A machine registers itself when it loads and unregisters itself when it is removed
 *       ({@link AbstractEnergyBlockEntity#STATUS_LIFECYCLE}).</li>
 *   <li>Every {@link #INTERVAL} game ticks, the tracker reads the status of each tracked machine.
 *       It collects only the machines whose status changed since the last send.</li>
 *   <li>It sends one packet per chunk with changes, to each player that tracks the chunk.</li>
 *   <li>When a player starts to watch a chunk, the player receives one packet with each lamp of
 *       that chunk that is not {@link MachineStatus#OFF}. The client sets new block entities to
 *       {@code OFF}, so the tracker does not send {@code OFF} lamps.</li>
 * </ul>
 *
 * <h2>Rate rule and why it exists</h2>
 * The tracker sends at most one packet per chunk per {@link #INTERVAL} ticks (0.5 s). It sends
 * only changes. One entry costs 9 bytes (8 for the position, 1 for the status). A vanilla
 * block entity update packet would contain the full update tag. The client re-meshes a block
 * only when its lamp changes. Thus a factory that runs without
 * a change costs no bandwidth and no render work.
 *
 * <h2>Hold rule</h2>
 * A machine that ran in the last {@link #HOLD} ticks, and now reports
 * {@link MachineStatus#BLOCKED}, stays {@link MachineStatus#RUNNING}. Thus a short gap between
 * two operations does not make the lamp flash red. {@link MachineStatus#NO_POWER} and
 * {@link MachineStatus#OFF} show at once.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>{@link Entry#sent} is the last status that the tracker sent for the machine (or
 *       {@code OFF} before the first send). The clients that track the chunk have this
 *       status.</li>
 *   <li>The tracker removes a chunk map when the last machine of the chunk goes away. The
 *       level tick also removes each chunk map that it finds empty.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * All maps are for the server thread only. The lifecycle hook, the level tick and the chunk
 * watch event all run on the server thread.
 *
 * <h2>Memory cleanup</h2>
 * <ul>
 *   <li>Machine removal (break or chunk unload): {@link #lifecycle} removes the entry.</li>
 *   <li>The level tick also removes each machine that {@link AbstractEnergyBlockEntity#isRemoved()}
 *       reports as removed.</li>
 *   <li>Level unload: {@link #levelUnload} removes all data of the level.</li>
 *   <li>Server start and server stop: all data is cleared.</li>
 * </ul>
 *
 * <h2>Missing channel</h2>
 * A vanilla client cannot connect, because the channel is not optional. The send method checks
 * the channel for a single receiver. It ignores send errors from fake or detached connections.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class MachineStatusTracker {
    private MachineStatusTracker() {}

    /**
     * {@code INTERVAL}: game ticks between two status reads (10 ticks = 0.5 s).
     * {@code HOLD}: game ticks for which a machine that ran stays green when it is blocked (30 ticks = 1.5 s).
     */
    public static final int INTERVAL = 10, HOLD = 30;

    /** Per-machine tracker state. */
    private static final class Entry {
        /** Last status sent to the clients. */
        MachineStatus sent = MachineStatus.OFF;
        /** Game time (ticks) of the last {@code RUNNING} read. The start value is far in the past without an overflow risk. */
        long lastRunning = Long.MIN_VALUE / 2;
    }

    /**
     * Level to chunk ({@link ChunkPos#toLong()}) to machine to entry. Server thread only.
     * The level and machine maps use identity keys.
     */
    private static final Map<ServerLevel, Map<Long, Map<AbstractEnergyBlockEntity, Entry>>> TRACKED = new IdentityHashMap<>();

    static {
        install();
    }

    /** Connects the lifecycle hook of {@link AbstractEnergyBlockEntity} to this tracker. */
    static void install() {
        AbstractEnergyBlockEntity.STATUS_LIFECYCLE = MachineStatusTracker::lifecycle;
    }

    /** Sets the hook again and removes data that a previous integrated server left. */
    @SubscribeEvent
    static void serverStarting(ServerStartingEvent event) {
        install();
        TRACKED.clear();
    }

    /** Releases the level, and through it the integrated server, when the level unloads. */
    @SubscribeEvent
    static void levelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) TRACKED.remove(level);
    }

    /** Removes all data when the server stops. */
    @SubscribeEvent
    static void serverStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        TRACKED.clear();
    }

    /**
     * Lifecycle hook. {@code loaded = true} from {@code onLoad()} adds the machine.
     * {@code loaded = false} from {@code setRemoved()} removes the machine, and its chunk map
     * when the map is empty. Server side only.
     */
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

    /**
     * Last sent status of a tracked machine, after the hold rule. Null when the tracker does not
     * track the machine. For tests and tools.
     */
    public static MachineStatus sentStatus(AbstractEnergyBlockEntity machine) {
        if (!(machine.getLevel() instanceof ServerLevel level)) return null;
        var chunks = TRACKED.get(level);
        var inChunk = chunks == null ? null : chunks.get(ChunkPos.asLong(machine.getBlockPos()));
        Entry e = inChunk == null ? null : inChunk.get(machine);
        return e == null ? null : e.sent;
    }

    /** Reused snapshot lists for {@link #levelTick}. Server thread only. They save one allocation per tick. */
    private static final List<Map.Entry<Long, Map<AbstractEnergyBlockEntity, Entry>>> CHUNK_SCRATCH = new ArrayList<>();
    private static final List<Map.Entry<AbstractEnergyBlockEntity, Entry>> MACHINE_SCRATCH = new ArrayList<>();

    /**
     * Reads the status of each tracked machine of the level every {@link #INTERVAL} ticks.
     * Sends the changes, one packet per chunk, to the players that track the chunk.
     */
    @SubscribeEvent
    static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now % INTERVAL != 0) return;
        var chunks = TRACKED.get(level);
        if (chunks == null || chunks.isEmpty()) return;
        // Iterate over snapshots in reused scratch lists. A status read can load or unload a
        // machine, and that changes the maps.
        CHUNK_SCRATCH.clear();
        CHUNK_SCRATCH.addAll(chunks.entrySet());
        for (var chunkEntry : CHUNK_SCRATCH) {
            List<Long> pos = null; List<Byte> st = null;
            MACHINE_SCRATCH.clear();
            MACHINE_SCRATCH.addAll(chunkEntry.getValue().entrySet());
            for (var e : MACHINE_SCRATCH) {
                AbstractEnergyBlockEntity machine = e.getKey();
                if (machine.isRemoved()) { chunkEntry.getValue().remove(machine); continue; }
                // Never load a chunk for a lamp. The redstone check reads the neighbour blocks.
                var p = machine.getBlockPos();
                if (!level.hasChunksAt(p.offset(-1, -1, -1), p.offset(1, 1, 1))) continue;
                MachineStatus status;
                try {
                    status = machine.machineStatus();
                } catch (RuntimeException ex) {
                    continue;                                   // A lamp error must never stop a machine tick.
                }
                Entry entry = e.getValue();
                // Hold rule: BLOCKED shortly after RUNNING stays RUNNING.
                if (status == MachineStatus.RUNNING) entry.lastRunning = now;
                else if (entry.sent == MachineStatus.RUNNING && status == MachineStatus.BLOCKED && now - entry.lastRunning < HOLD) {
                    status = MachineStatus.RUNNING;             // Gap between two operations.
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
        CHUNK_SCRATCH.clear();
        MACHINE_SCRATCH.clear();
    }

    /**
     * Sends the lamps of a chunk to a player that starts to watch it. NeoForge fires
     * {@code ChunkWatchEvent.Sent} after the chunk data packet, so the client already has the
     * block entities. Only lamps that are not {@code OFF} go out.
     */
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

    /**
     * Builds one {@link MachineStatusPacket} and sends it.
     *
     * @param level the level of the chunk
     * @param chunk the chunk of all positions
     * @param pos   packed block positions ({@code BlockPos.asLong()})
     * @param st    status ordinals, same index as {@code pos}
     * @param only  a single receiver, or null for each player that tracks the chunk
     */
    private static void send(ServerLevel level, ChunkPos chunk, List<Long> pos, List<Byte> st, net.minecraft.server.level.ServerPlayer only) {
        // Split into packets of at most MAX_ENTRIES (the decoder rejects a larger count).
        int total = Math.min(pos.size(), st.size());
        for (int from = 0; from < total; from += MachineStatusPacket.MAX_ENTRIES) {
            int len = Math.min(MachineStatusPacket.MAX_ENTRIES, total - from);
            long[] p = new long[len]; byte[] s = new byte[len];
            for (int i = 0; i < len; i++) { p[i] = pos.get(from + i); s[i] = st.get(from + i); }
            sendOne(level, chunk, new MachineStatusPacket(p, s), only);
        }
    }

    private static void sendOne(ServerLevel level, ChunkPos chunk, MachineStatusPacket packet, net.minecraft.server.level.ServerPlayer only) {
        try {
            if (only != null) {
                if (only.connection != null && only.connection.hasChannel(MachineStatusPacket.TYPE)) PacketDistributor.sendToPlayer(only, packet);
            } else {
                PacketDistributor.sendToPlayersTrackingChunk(level, chunk, packet);
            }
        } catch (RuntimeException ignored) {
            // A fake or detached connection can throw. The lamp state is not important for it.
        }
    }
}
