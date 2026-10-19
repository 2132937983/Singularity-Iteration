package com.miophas.singularity_iteration.core.runtime.sync;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Limits the rate of {@code sendBlockUpdated} calls for block entities whose data can change
 * every tick (for example a tank that a pipe fills). 0.1.10.10: also the voltage detector, the item
 * and fluid distributors, the storage box, the liquid heat source and the semifluid generators.
 *
 * <h2>What it sends</h2>
 * This class sends no custom payload. It calls {@link ServerLevel#sendBlockUpdated} with flag 3.
 * Vanilla then sends a {@code ClientboundBlockEntityDataPacket} (from
 * {@code getUpdatePacket()}) to each player that tracks the chunk. Direction: server to client.
 * The packet is vanilla, so a client without this mod can receive it. A fake player has no
 * chunk tracking, so it receives nothing.
 *
 * <h2>Rate rule</h2>
 * <ul>
 *   <li>The first change after a quiet period of at least {@link #MIN_INTERVAL_TICKS} game ticks
 *       goes out at once.</li>
 *   <li>Each further change inside the window does not send a packet. The class records one
 *       trailing update for the block entity. The trailing update is due at
 *       {@code last sent tick + MIN_INTERVAL_TICKS}.</li>
 *   <li>{@link #flush} runs at the end of each server tick. It sends each trailing update that
 *       is due.</li>
 * </ul>
 * Result: each block entity sends at most one packet per window. The client always receives the
 * final state. The final state is at most {@code MIN_INTERVAL_TICKS} ticks late.
 *
 * <h2>Why it exists</h2>
 * A block entity update packet contains the full update tag (for a tank: the fluid stack and
 * its components). A pipe-fed tank, or a fill that touches each tank of a multi-tank column,
 * can change every tick. Without this class, each change sent one full packet to each tracking
 * player. The rate rule limits this bandwidth and the client re-mesh work.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>A key in {@link #PENDING} always has a value that is greater than its
 *       {@link #LAST_SENT} value.</li>
 *   <li>An immediate send removes the block entity from {@link #PENDING}. Thus one block entity
 *       never has two packets in one window.</li>
 *   <li>A game time that goes back ({@code now < last}, for example after a new world in the
 *       same integrated server) counts as a quiet period. The update goes out at once.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * Call this class on the server thread. The maps are not thread-safe. A call from a different
 * thread does not touch the maps: it queues the same request on the server thread
 * ({@code MinecraftServer.execute}), where the normal rate rule applies.
 *
 * <h2>Memory cleanup</h2>
 * <ul>
 *   <li>{@link #LAST_SENT} holds weak keys. The garbage collector removes the entry of a block
 *       entity that nothing else references.</li>
 *   <li>{@link #PENDING} holds strong keys. {@link #flush} removes each entry whose block entity
 *       is removed, has no server level, or is in an unloaded chunk. A level unload removes its
 *       block entities, so the next flush releases them.</li>
 *   <li>{@code ServerStoppedEvent} clears both maps.</li>
 * </ul>
 */
public final class ClientSyncThrottle {
    /** Minimum number of game ticks between two packets for one block entity. 5 ticks = 0.25 s. */
    public static final int MIN_INTERVAL_TICKS = 5;

    /**
     * Game time (ticks) of the last packet that this class sent, per block entity.
     * The keys are weak, so this map does not keep an unloaded block entity in memory.
     */
    private static final Map<BlockEntity, Long> LAST_SENT = new WeakHashMap<>();
    /**
     * Block entities with a merged trailing update that is not sent yet.
     * The value is the earliest game time (ticks) at which {@link #flush} can send it.
     * Identity keys: two block entities are never equal.
     */
    private static final Map<BlockEntity, Long> PENDING = new IdentityHashMap<>();
    /** True after {@link #install()} added the event listeners. The listeners stay for the JVM lifetime. */
    private static boolean installed;

    private ClientSyncThrottle() {}

    /**
     * Requests a client update for {@code tile}.
     * Call this method each time the synced data of the block entity changes.
     * The method does nothing on the client side, or when the block entity is removed.
     *
     * @param tile the block entity that changed
     */
    public static void request(BlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || tile.isRemoved()) return;
        // Off-thread caller: do not touch the maps and do not call sendBlockUpdated here (the
        // chunk map is not thread-safe). Run the request again on the server thread (0.1.7.36).
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> request(tile));
            return;
        }
        install();
        com.miophas.singularity_iteration.core.runtime.PerfCounters.add(com.miophas.singularity_iteration.core.runtime.PerfCounters.Kind.SYNC_REQUESTS);
        long now = level.getGameTime();
        Long last = LAST_SENT.get(tile);
        // Quiet period passed, first request, or game time went back: send now.
        if (last == null || now - last >= MIN_INTERVAL_TICKS || now < last) {
            PENDING.remove(tile);
            LAST_SENT.put(tile, now);
            send(level, tile);
        } else {
            // Inside the window: merge into one trailing update. Keep the first due time.
            PENDING.putIfAbsent(tile, last + MIN_INTERVAL_TICKS);
        }
    }

    /**
     * Sends each trailing update that is due. Runs on the server thread after each server tick.
     * Drops the entries of block entities that are removed or in an unloaded chunk. The client
     * gets the current state of such a block entity with the chunk data when the chunk loads.
     */
    private static void flush(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        Iterator<Map.Entry<BlockEntity, Long>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockEntity, Long> entry = it.next();
            BlockEntity tile = entry.getKey();
            if (!(tile.getLevel() instanceof ServerLevel level) || tile.isRemoved()
                    || !level.isLoaded(tile.getBlockPos())) {
                it.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now < entry.getValue()) continue;
            it.remove();
            LAST_SENT.put(tile, now);
            send(level, tile);
        }
    }

    /**
     * Sends the vanilla block entity update to each player that tracks the chunk.
     * Flag 3 = {@code UPDATE_NEIGHBORS | UPDATE_CLIENTS}. The block state does not change.
     */
    private static void send(ServerLevel level, BlockEntity tile) {
        com.miophas.singularity_iteration.core.runtime.PerfCounters.add(com.miophas.singularity_iteration.core.runtime.PerfCounters.Kind.SYNC_SENT);
        var state = tile.getBlockState();
        level.sendBlockUpdated(tile.getBlockPos(), state, state, 3);
    }

    /**
     * Adds the tick listener and the server-stop cleanup listener on the first call.
     * Later calls do nothing. Server thread only.
     */
    private static void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(ClientSyncThrottle::flush);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            PENDING.clear();
            LAST_SENT.clear();
        });
    }
}
