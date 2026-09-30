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
 * Rate-limits {@code sendBlockUpdated} for block entities whose data can change every tick
 * (for example a tank fed by a pipe).
 *
 * <p>The first change after a quiet period is sent immediately. Further changes within
 * {@link #MIN_INTERVAL_TICKS} are merged into one trailing update, sent from the server tick
 * once the window has passed. Clients therefore always see the final state, at most
 * {@code MIN_INTERVAL_TICKS} late, and never more than one packet per window per block entity.
 *
 * <p>Server-thread only. A call from any other thread falls back to an immediate update.
 */
public final class ClientSyncThrottle {
    public static final int MIN_INTERVAL_TICKS = 5;

    /** Game time of the last packet actually sent. Weak so unloaded tiles are never retained. */
    private static final Map<BlockEntity, Long> LAST_SENT = new WeakHashMap<>();
    /** Tiles with a merged, not yet sent update, mapped to the earliest tick it may go out. */
    private static final Map<BlockEntity, Long> PENDING = new IdentityHashMap<>();
    private static boolean installed;

    private ClientSyncThrottle() {}

    public static void request(BlockEntity tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || tile.isRemoved()) return;
        if (!level.getServer().isSameThread()) {
            send(level, tile);
            return;
        }
        install();
        long now = level.getGameTime();
        Long last = LAST_SENT.get(tile);
        if (last == null || now - last >= MIN_INTERVAL_TICKS || now < last) {
            PENDING.remove(tile);
            LAST_SENT.put(tile, now);
            send(level, tile);
        } else {
            PENDING.putIfAbsent(tile, last + MIN_INTERVAL_TICKS);
        }
    }

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

    private static void send(ServerLevel level, BlockEntity tile) {
        var state = tile.getBlockState();
        level.sendBlockUpdated(tile.getBlockPos(), state, state, 3);
    }

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
